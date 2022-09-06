package id.co.sistema.vkey

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.vkey.android.internal.vguard.engine.BasicThreatInfo
import com.vkey.android.vguard.*
import id.co.sistema.vkey.sfio.BlockDataActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import vkey.android.vos.Vos
import vkey.android.vos.VosWrapper
import java.lang.Exception
import java.util.ArrayList

class SplashScreenActivity : AppCompatActivity(){

//    private var vGuardMgr: VGuard? = null
//    private lateinit var hook: VGuardLifecycleHook
//    private lateinit var broadcastRcvr: VGuardBroadcastReceiver
//
//    private lateinit var mVos: Vos
//    private lateinit var mStartVosThread: Thread
//
//    private var isStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Using for splashscreen
//        Handler(Looper.getMainLooper()).postDelayed(
//            { startActivity(Intent(this, MainActivity::class.java)); finish() },
//            5000
//        )
    }

    /**
     * Method to retrieve [BasicThreatInfo] from [CustomApplication] class
     * after being broadcast by [EventBus].
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onMessageEvent(isStarted: Boolean?) {
        val message = "isVosStarted: $isStarted"
        showLog(LevelInfo.Debug, "Splash", message)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /**
     * Handle [EventBus] lifecycle.
     */
    override fun onStart() {
        super.onStart()
        showLog(LevelInfo.Debug, "Splash", "EventBus OnStart")
        EventBus.getDefault().register(this)
    }

    /**
     * Handle [EventBus] lifecycle.
     */
    override fun onPause() {
        super.onPause()
        showLog(LevelInfo.Debug, "Splash", "EventBus OnPause")
        EventBus.getDefault().unregister(this)
    }

    //    override fun onResume() {
//        super.onResume()
//        vGuardMgr?.onResume(hook)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        vGuardMgr?.onPause(hook)
//    }
//
//    override fun onDestroy() {
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastRcvr)
//        if (vGuardMgr != null) {
//            vGuardMgr?.destroy()
//        }
//        super.onDestroy()
//    }
//
//    private fun setupVGuard() {
//        receiveVGuardBroadcast()
//        registerLocalBroadcast()
//        setupAppProtection()
//    }
//
//    /**
//     * Handling receiving notifications from V-OS App Protection
//     * */
//    private fun receiveVGuardBroadcast() {
//        broadcastRcvr = object : VGuardBroadcastReceiver(null) {
//            override fun onReceive(context: Context?, intent: Intent?) {
//                super.onReceive(context, intent)
//
//                when {
//                    PROFILE_LOADED == intent?.action -> {
//                        showLog(LevelInfo.Debug, TAG, "Profile is loaded...")
//                        vGuardMgr?.setThreatIntelligenceServerURL("https://sistemadev.my.id/")
//                    }
//
//                    VOS_READY == intent?.action -> {
//                        instanceVGuardManager(intent)
//                    }
//
//                    ACTION_SCAN_COMPLETE == intent?.action -> {
//                        showLog(LevelInfo.Debug, TAG, "Scan complete...")
//                        scanningThreats(intent)
//                    }
//
//                    VGUARD_STATUS == intent?.action -> {}
//                }
//            }
//        }
//    }
//
//    private fun instanceVGuardManager(intent: Intent) {
//        val firmwareCode = intent.getLongExtra(VOS_FIRMWARE_RETURN_CODE_KEY, 0)
//        if (firmwareCode >= 0) {
//            // if the `VGuardManager` is not available,
//            // create a `VGuardManager` instance from `VGuardFactory`
//            if (vGuardMgr == null) {
//                vGuardMgr = VGuardFactory.getInstance()
//                hook = ActivityLifecycleHook(vGuardMgr)
//
//                mVos = Vos(applicationContext)
//                mVos.registerVosWrapperCallback(this)
////                startVos(this)
//
//                if (vGuardMgr?.isVosStarted == true) {
//                    startActivity(Intent(this, BlockDataActivity::class.java))
//                    finish()
//                }
//            }
//        } else {
//            // Error handling
//            val message = "firmwareCode: $firmwareCode, failed instance VGuardManager"
//            showLog(LevelInfo.Error, TAG, message)
//        }
//    }
//
//    private fun scanningThreats(intent: Intent) {
//        val detectedThreats = intent
//            .getParcelableArrayListExtra<Parcelable>(VGuardBroadcastReceiver.SCAN_COMPLETE_RESULT) as ArrayList<Parcelable>
//
//        val threats: ArrayList<BasicThreatInfo> = arrayListOf()
//        for(item in detectedThreats) {
//            threats.add(item as BasicThreatInfo)
//            showLog(LevelInfo.Debug, TAG, "Threat: $threats")
//        }
//
//        /**
//         * EventBus is used to send the threat into [MainActivity]. It need to be delayed for 3 sec
//         * to make sure [MainActivity] is already rendered on the screen.
//         */
//        Handler(Looper.getMainLooper()).postDelayed({
//            EventBus.getDefault().post(threats)
//        }, 5000)
//    }
//
//    /**
//     * Register using LocalBroadcastManager only for keeping data within your app
//     * */
//    private fun registerLocalBroadcast() {
//        LocalBroadcastManager.getInstance(applicationContext).apply {
//            registerReceiver(broadcastRcvr, IntentFilter(VGuardBroadcastReceiver.ACTION_FINISH))
//            registerReceiver(broadcastRcvr, IntentFilter(VGuardBroadcastReceiver.ACTION_SCAN_COMPLETE))
//            registerReceiver(broadcastRcvr, IntentFilter(PROFILE_LOADED))
//            registerReceiver(broadcastRcvr, IntentFilter(VGuardBroadcastReceiver.VOS_READY))
//            registerReceiver(broadcastRcvr, IntentFilter(PROFILE_THREAT_RESPONSE))
//            registerReceiver(broadcastRcvr, IntentFilter(VGuardBroadcastReceiver.VGUARD_STATUS))
//        }
//    }
//
//    /**
//     * Setting up V-OS App Protection here
//     * */
//    private fun setupAppProtection() {
//        try {
//            val config = VGuardFactory.Builder()
//                .setDebugable(true)
//                .setAllowsArbitraryNetworking(true)
//                .setMemoryConfiguration(MemoryConfiguration.DEFAULT)
//                .setVGExceptionHandler(this)
//
//            VGuardFactory().getVGuard(applicationContext, config)
//        } catch (e: Exception) {
//            showLog(LevelInfo.Error, TAG, e.message.toString())
//            e.printStackTrace()
//        }
//    }
//
//    override fun handleException(e: Exception?) {
//        showLog(LevelInfo.Error, TAG, e?.message.toString())
//        e?.printStackTrace()
//    }
//
//    override fun onNotified(p0: Int, p1: Int): Boolean {
//        showLog(LevelInfo.Debug, TAG, "onNotified: $p0 || $p1")
//        return true
//    }
//
//    companion object {
//        private const val TAG = "SplashScreen"
//    }
}