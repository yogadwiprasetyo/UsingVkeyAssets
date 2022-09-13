package id.co.sistema.vkey

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import com.vkey.android.internal.vguard.engine.BasicThreatInfo
import com.vkey.android.vguard.*
import com.vkey.android.vguard.VGuardBroadcastReceiver.*
import id.co.sistema.vkey.di.networkModule
import id.co.sistema.vkey.di.viewModelModule
import id.co.sistema.vkey.util.*
import org.greenrobot.eventbus.EventBus
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import vkey.android.vos.Vos
import vkey.android.vos.VosWrapper

class CustomApplication : Application(), VGExceptionHandler,
    Application.ActivityLifecycleCallbacks, VosWrapper.Callback {

    // LifecycleHook to notify VGuard of activity's lifecycle
    private lateinit var hook: VGuardLifecycleHook

    // For VGuard to notify host app of events
    private lateinit var broadcastRcvr: VGuardBroadcastReceiver

    private lateinit var mVos: Vos
    private lateinit var mStartVosThread: Thread

    private fun setupVGuard() {
        receiveVGuardBroadcast()
        registerLocalBroadcast()
        setupAppProtection()
    }

    /**
     * Handling receiving notifications from V-OS App Protection
     * */
    private fun receiveVGuardBroadcast() {
        broadcastRcvr = object : VGuardBroadcastReceiver(null) {
            override fun onReceive(context: Context?, intent: Intent?) {
                super.onReceive(context, intent)

                when {
                    PROFILE_LOADED == intent?.action -> {
                        showLog("Profile is loaded...")
                        vGuardMgr?.setThreatIntelligenceServerURL("https://sistemadev.my.id/")
                    }

                    VOS_READY == intent?.action -> instanceVGuardManager(intent)

                    ACTION_SCAN_COMPLETE == intent?.action -> {
                        showLog("Scan complete...")
                        scanningThreats(intent)
                        EventBus.getDefault().post(vGuardMgr?.isVosStarted)
                    }

                    VGUARD_STATUS == intent?.action -> {}
                }
            }
        }
    }

    private fun instanceVGuardManager(intent: Intent) {
        val firmwareCode = intent.getLongExtra(VOS_FIRMWARE_RETURN_CODE_KEY, DEFAULT_LONG)
        if (firmwareCode >= DEFAULT_LONG) {
            // if the `VGuardManager` is not available,
            // create a `VGuardManager` instance from `VGuardFactory`
            if (vGuardMgr == null) {
                vGuardMgr = VGuardFactory.getInstance()
                hook = ActivityLifecycleHook(vGuardMgr)

                mVos = Vos(this)
                mVos.registerVosWrapperCallback(this)
                startVos(this)

                firmwareReturnCode = firmwareCode
                isVosStart = vGuardMgr?.isVosStarted == true
            }
        } else {
            // Error handling
            val message = "firmwareCode: $firmwareCode, failed instance VGuardManager"
            showLog(Exception(message))
        }
    }

    /**
     * Scan all threats in devices
     * */
    private fun scanningThreats(intent: Intent) {
        val detectedThreats = intent
            .getParcelableArrayListExtra<Parcelable>(SCAN_COMPLETE_RESULT) as ArrayList<Parcelable>

        val threats: ArrayList<BasicThreatInfo> = arrayListOf()
        for (item in detectedThreats) {
            threats.add(item as BasicThreatInfo)
            showLog("Threat: $threats")
        }

        /**
         * EventBus is used to send the threat into []. It need to be delayed for 5 sec
         * to make sure [MainActivity] is already rendered on the screen.
         */
        Handler(Looper.getMainLooper()).postDelayed({
            EventBus.getDefault().post(threats)
        }, 5000)
    }

    /**
     * Register using LocalBroadcastManager only for keeping data within your app
     * */
    private fun registerLocalBroadcast() {
        LocalBroadcastManager.getInstance(this).apply {
            registerReceiver(broadcastRcvr, IntentFilter(ACTION_FINISH))
            registerReceiver(broadcastRcvr, IntentFilter(ACTION_SCAN_COMPLETE))
            registerReceiver(broadcastRcvr, IntentFilter(PROFILE_LOADED))
            registerReceiver(broadcastRcvr, IntentFilter(VOS_READY))
            registerReceiver(broadcastRcvr, IntentFilter(PROFILE_THREAT_RESPONSE))
            registerReceiver(broadcastRcvr, IntentFilter(VGUARD_STATUS))
        }
    }

    /**
     * Setting up V-OS App Protection here
     * */
    private fun setupAppProtection() {
        try {
            val config = VGuardFactory.Builder()
                .setDebugable(true)
                .setAllowsArbitraryNetworking(true)
                .setMemoryConfiguration(MemoryConfiguration.DEFAULT)
                .setVGExceptionHandler(this)

            VGuardFactory().getVGuard(this, config)
        } catch (e: Exception) {
            showLog(e)
        }
    }

    private fun startVos(ctx: Context) {
        mStartVosThread = Thread {
            try {
                // Get the kernel data in byte from `firmware` asset file
                val inputStream = ctx.assets.open("firmware")
                val kernelData = inputStream.readBytes()
                inputStream.read(kernelData)
                inputStream.close()

                // Start V-OS
                val vosReturnCode = mVos.start(kernelData, null, null, null, null)

                if (vosReturnCode > 0) {
                    // Successfully started V-OS
                    // Instantiate a `VosWrapper` instance for calling V-OS Processor APIs
                    vosWrapper = VosWrapper.getInstance(this)
                    showLog("Successfully started V-OS")
                } else {
                    // Failed to start V-OS
                    showLog("Failed to start V-OS")
                }
            } catch (e: VGException) {
                showLog(e)
            }
        }

        mStartVosThread.start()
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        // Initiate Koin for dependency injection
        startKoin {
            androidContext(this@CustomApplication)
            modules(
                networkModule,
                viewModelModule
            )
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (vGuardMgr == null && activity is SplashScreenActivity) {
            setupVGuard()
            VosWrapper.getInstance(this).setLoggerBaseUrl("https://sistemadev.my.id/")
        }
    }

    override fun onActivityResumed(activity: Activity) {
        vGuardMgr?.onResume(hook)
    }

    override fun onActivityPaused(activity: Activity) {
        vGuardMgr?.onPause(hook)
    }


    override fun onActivityDestroyed(activity: Activity) {
        if (activity is SplashScreenActivity) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastRcvr)
        }
    }

    override fun handleException(e: Exception?) {
        showLog(Exception(e?.message.toString()))
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, savedInstanceState: Bundle) {}

    companion object {
        // VGuard object that is used for scanning
        var vGuardMgr: VGuard? = null
        var vosWrapper: VosWrapper? = null
        var firmwareReturnCode = 0L
        var isVosStart = false
    }

    override fun onNotified(p0: Int, p1: Int): Boolean {
        return false
    }
}