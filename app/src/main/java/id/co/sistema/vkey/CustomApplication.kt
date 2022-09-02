package id.co.sistema.vkey

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.os.AsyncTask.execute
import android.text.TextUtils
import android.util.Log
import com.vkey.android.internal.vguard.engine.BasicThreatInfo
import com.vkey.android.vguard.*
import com.vkey.android.vguard.VGuardBroadcastReceiver.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import vkey.android.vos.Vos
import vkey.android.vos.VosWrapper
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class CustomApplication : Application(), VGExceptionHandler,
    Application.ActivityLifecycleCallbacks, VosWrapper.Callback {

    private var vGuardMgr: VGuard? = null // VGuard object that is used for scanning
    private lateinit var hook: VGuardLifecycleHook // LifecycleHook to notify VGuard of activity's lifecycle
    private lateinit var broadcastRcvr: VGuardBroadcastReceiver // For VGuard to notify host app of events

    private lateinit var mVos: Vos
    private lateinit var mStartVosThread: Thread

    var firmwareReturnCode = 0L

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
                        showLog(LevelInfo.Debug, TAG, "Profile is loaded...")
                    }

                    VOS_READY == intent?.action -> {
                        instanceVGuardManager(intent)
                    }

                    ACTION_SCAN_COMPLETE == intent?.action -> {
                        showLog(LevelInfo.Debug, TAG, "Scan complete...")
                        scanningThreats(intent)
                    }

                    VGUARD_STATUS == intent?.action -> {
//                        scanThreatsDetected(intent)
                    }
                }
            }
        }
    }

    private fun instanceVGuardManager(intent: Intent) {
        val firmwareCode = intent.getLongExtra(VOS_FIRMWARE_RETURN_CODE_KEY, 0)
        if (firmwareCode >= 0) {
            // if the `VGuardManager` is not available,
            // create a `VGuardManager` instance from `VGuardFactory`
            if (vGuardMgr == null) {
                vGuardMgr = VGuardFactory.getInstance()
                hook = ActivityLifecycleHook(vGuardMgr)

                mVos = Vos(this)
                mVos.registerVosWrapperCallback(this)
                firmwareReturnCode = firmwareCode
//                startVos(this)
            }
        } else {
            // Error handling
            val message = "firmwareCode: $firmwareCode, failed instance VGuardManager"
            showLog(LevelInfo.Error, TAG, message)
        }
    }

    private fun scanningThreats(intent: Intent) {
        val detectedThreats = intent
            .getParcelableArrayListExtra<Parcelable>(SCAN_COMPLETE_RESULT) as ArrayList<Parcelable>

        val threats: ArrayList<BasicThreatInfo> = arrayListOf()
        for(item in detectedThreats) {
            threats.add(item as BasicThreatInfo)
            showLog(LevelInfo.Debug, TAG, "Threat: $threats")
        }

        /**
         * EventBus is used to send the threat into [MainActivity]. It need to be delayed for 3 sec
         * to make sure [MainActivity] is already rendered on the screen.
         */
        Handler(Looper.getMainLooper()).postDelayed({
            EventBus.getDefault().post(threats)
        }, 5000)
//        EventBus.getDefault().post(threats)
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
            showLog(LevelInfo.Error, TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    private fun startVos(ctx: Context) {
        runBlocking {
            launch(Dispatchers.IO) {
                delay(10000)
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
                        val vosWrapper = VosWrapper.getInstance(ctx)
                    } else {
                        // Failed to start V-OS
                        Log.e(TAG, "Failed to start V-OS")
                    }
                } catch (e: VGException) {
                    Log.e(TAG, e.message.toString())
                    e.printStackTrace()
                }
            }
        }

        // TODO: Too much work in main thread
//        thread {
//            try {
//                // Get the kernel data in byte from `firmware` asset file
//                val inputStream = ctx.assets.open("firmware")
//                val kernelData = inputStream.readBytes()
//                inputStream.read(kernelData)
//                inputStream.close()
//
//                // Start V-OS
//                val vosReturnCode = mVos.start(kernelData, null, null, null, null)
//
//                if (vosReturnCode > 0) {
//                    // Successfully started V-OS
//                    // Instantiate a `VosWrapper` instance for calling V-OS Processor APIs
//                    val vosWrapper = VosWrapper.getInstance(ctx)
//                } else {
//                    // Failed to start V-OS
//                    Log.e(TAG, "Failed to start V-OS")
//                }
//            } catch (e: VGException) {
//                Log.e(TAG, e.message.toString())
//                e.printStackTrace()
//            }
//        }

        // TODO: Not Stable, sometimes is safe, sometimes is broke
//        Executors.newSingleThreadExecutor().execute {
//            try {
//                // Get the kernel data in byte from `firmware` asset file
//                val inputStream = ctx.assets.open("firmware")
//                val kernelData = inputStream.readBytes()
//                inputStream.read(kernelData)
//                inputStream.close()
//
//                // Start V-OS
//                val vosReturnCode = mVos.start(kernelData, null, null, null, null)
//
//                if (vosReturnCode > 0) {
//                    // Successfully started V-OS
//                    // Instantiate a `VosWrapper` instance for calling V-OS Processor APIs
//                    val vosWrapper = VosWrapper.getInstance(ctx)
//                } else {
//                    // Failed to start V-OS
//                    Log.e(TAG, "Failed to start V-OS")
//                }
//            } catch (e: VGException) {
//                Log.e(TAG, e.message.toString())
//                e.printStackTrace()
//            }
//        }

        // TODO: Too much work in main thread
//        mStartVosThread = Thread {
//            try {
//                // Get the kernel data in byte from `firmware` asset file
//                val inputStream = ctx.assets.open("firmware")
//                val kernelData = inputStream.readBytes()
//                inputStream.read(kernelData)
//                inputStream.close()
//
//                // Start V-OS
//                val vosReturnCode = mVos.start(kernelData, null, null, null, null)
//
//                if (vosReturnCode > 0) {
//                    // Successfully started V-OS
//                    // Instantiate a `VosWrapper` instance for calling V-OS Processor APIs
//                    val vosWrapper = VosWrapper.getInstance(ctx)
//                } else {
//                    // Failed to start V-OS
//                    Log.e(TAG, "Failed to start V-OS")
//                }
//            } catch (e: VGException) {
//                Log.e(TAG, e.message.toString())
//                e.printStackTrace()
//            }
//        }
//
//        mStartVosThread.start()
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (vGuardMgr == null && activity is SplashScreenActivity) {
            setupVGuard()
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
            vGuardMgr?.destroy()
            vGuardMgr = null
        }
    }

    override fun handleException(e: Exception?) {
        showLog(LevelInfo.Error, TAG, e?.message.toString())
        e?.printStackTrace()
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, savedInstanceState: Bundle) {}

    companion object {
        private val TAG = CustomApplication::class.java.simpleName
        var firmwareReturnCode = 0L
    }

    override fun onNotified(p0: Int, p1: Int): Boolean {
        return false
    }
}