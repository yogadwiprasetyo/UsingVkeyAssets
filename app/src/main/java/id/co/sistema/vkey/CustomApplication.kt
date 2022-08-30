package id.co.sistema.vkey

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import com.vkey.android.internal.vguard.engine.BasicThreatInfo
import com.vkey.android.vguard.*
import com.vkey.android.vguard.VGuardBroadcastReceiver.*
import org.json.JSONObject
import vkey.android.vos.VosWrapper
import java.text.SimpleDateFormat
import java.util.*

class CustomApplication : Application(), VGExceptionHandler,
    Application.ActivityLifecycleCallbacks {

    // VGuard object that is used for scanning
    private var vGuardMgr: VGuard? = null

    // LifecycleHook to notify VGuard of activity's lifecycle
    private lateinit var hook: VGuardLifecycleHook

    // For VGuard to notify host app of events
    private lateinit var broadcastRcvr: VGuardBroadcastReceiver

    companion object {
        private const val TAG_ON_RECEIVE = "OnReceive"
        private const val TAG = "CustomApplication"
    }

    private fun setupVGuard() {
        // for receiving notifications from V-OS App Protection
        receiveVGuardBroadcast()

        // register using LocalBroadcastManager only for keeping data within your app
        registerLocalBroadcast()

        // Setting up V-OS App Protection here
        setupAppProtection()
    }

    private fun receiveVGuardBroadcast() {
        broadcastRcvr = object : VGuardBroadcastReceiver(null) {
            override fun onReceive(context: Context?, intent: Intent?) {
                super.onReceive(context, intent)

                when {
                    PROFILE_LOADED == intent?.action -> {
                        showLogDebug(TAG_ON_RECEIVE, "PROFILE_LOADED")
                    }

                    ACTION_SCAN_COMPLETE == intent?.action -> showLogDebug(
                        TAG_ON_RECEIVE,
                        "ACTION_SCAN_COMPLETE"
                    )

                    VGUARD_OVERLAY_DETECTED_DISABLE == intent?.action -> showLogDebug(
                        TAG_ON_RECEIVE,
                        "VGUARD_OVERLAY_DETECTED_DISABLE"
                    )

                    VGUARD_OVERLAY_DETECTED == intent?.action -> showLogDebug(
                        TAG_ON_RECEIVE,
                        "VGUARD_OVERLAY_DETECTED"
                    )

                    VGUARD_STATUS == intent?.action -> {
                        showLogDebug(
                            TAG_ON_RECEIVE,
                            "HasExtraVGuardInitStatus: ${intent.hasExtra(VGUARD_INIT_STATUS)}"
                        )

                        if (intent.hasExtra(VGUARD_MESSAGE)) {
                            val message = intent.getStringExtra(VGUARD_MESSAGE)
                            var allMessage = "\n $VGUARD_MESSAGE : $message"
                            if (message != null) {
                                showLogDebug("MSG", message)
                            }
                            showLogDebug(TAG_ON_RECEIVE, allMessage)
                        }

                        if (intent.hasExtra(VGUARD_HANDLE_THREAT_POLICY)) {
                            val detectedThreats =
                                intent.getParcelableArrayListExtra<Parcelable>(SCAN_COMPLETE_RESULT)
                            val builder = StringBuilder()

                            if (detectedThreats != null) {
                                for (info in detectedThreats) {
                                    val infoStr = (info as BasicThreatInfo).toString()
                                    builder.append("$infoStr \n")
                                }

                                val highestResponse =
                                    intent.getIntExtra(VGUARD_HIGHEST_THREAT_POLICY, -1)
                                val alertTitle = intent.getStringExtra(VGUARD_ALERT_TITLE)
                                val alertMessage = intent.getStringExtra(VGUARD_ALERT_MESSAGE)
                                val disabledAppExpired =
                                    intent.getLongExtra(VGUARD_DISABLED_APP_EXPIRED, 0)

                                when {
                                    highestResponse > 0 -> builder.append("highest policy: $highestResponse\n")
                                    !TextUtils.isEmpty(alertTitle) -> builder.append("alertTitle: $alertTitle\n")
                                    !TextUtils.isEmpty(alertMessage) -> builder.append("alertMessage: $alertMessage\n")
                                    disabledAppExpired > 0 -> {
                                        val format = SimpleDateFormat(
                                            "yyyy-MMdd HH:mm:ss",
                                            Locale.getDefault()
                                        )
                                        val activeDate = format.format(Date(disabledAppExpired))
                                        builder.append("App can use again after: $activeDate\n")
                                    }
                                }

                                showLogDebug(TAG_ON_RECEIVE, builder.toString())
                            }
                        }

                        if (intent.hasExtra(VGUARD_INIT_STATUS)) {
                            showLogDebug(
                                TAG_ON_RECEIVE,
                                "VGUARD_INIT_STATUS: ${
                                    intent.getBooleanExtra(
                                        VGUARD_INIT_STATUS,
                                        false
                                    )
                                }"
                            )
                            val initStatus = intent.getBooleanExtra(VGUARD_INIT_STATUS, false)
                            var message = "\n $VGUARD_STATUS: $initStatus"

                            if (!initStatus) {
                                try {
                                    val jsonObject =
                                        JSONObject(intent.getStringExtra(VGUARD_MESSAGE))
                                    showLogDebug(
                                        TAG_ON_RECEIVE,
                                        "code: ${jsonObject.getString("code")}"
                                    )
                                    showLogDebug(
                                        TAG_ON_RECEIVE,
                                        "code: ${jsonObject.getString("description")}"
                                    )
                                    message += jsonObject.toString()
                                } catch (e: Exception) {
                                    Log.e(TAG_ON_RECEIVE, e.message.toString())
                                    e.printStackTrace()
                                }
                                showLogDebug(TAG_ON_RECEIVE, message)
                            }
                        }

                        if (intent.hasExtra(VGUARD_SSL_ERROR_DETECTED)) {
                            showLogDebug(
                                TAG_ON_RECEIVE,
                                "VGUARD_SSL_ERROR_DETECTED: ${
                                    intent.getBooleanExtra(
                                        VGUARD_SSL_ERROR_DETECTED,
                                        false
                                    )
                                }"
                            )
                            val sslError = intent.getBooleanExtra(VGUARD_SSL_ERROR_DETECTED, false)
                            var message = "\n $VGUARD_SSL_ERROR_DETECTED: $sslError"

                            if (sslError) {
                                try {
                                    val jsonObject =
                                        JSONObject(intent.getStringExtra(VGUARD_MESSAGE))
                                    showLogDebug(
                                        TAG_ON_RECEIVE,
                                        jsonObject.getString(VGUARD_ALERT_TITLE)
                                    )
                                    showLogDebug(
                                        TAG_ON_RECEIVE,
                                        jsonObject.getString(VGUARD_ALERT_MESSAGE)
                                    )
                                    message += jsonObject.toString()
                                } catch (e: Exception) {
                                    Log.e(TAG_ON_RECEIVE, e.message.toString())
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    VOS_READY == intent?.action -> {
                        val firmwareReturnCode =
                            intent.getLongExtra(VOS_FIRMWARE_RETURN_CODE_KEY, DEFAULT_LONG)
                        if (firmwareReturnCode >= DEFAULT_LONG) {
                            // if the `VGuardManager` is not available,
                            // create a `VGuardManager` instance from `VGuardFactory`
                            if (vGuardMgr == null) {
                                vGuardMgr = VGuardFactory.getInstance()
                                hook = ActivityLifecycleHook(vGuardMgr)

                                val isStarted = vGuardMgr?.isVosStarted.toString()
                                val valueTID = vGuardMgr?.troubleshootingId.toString()
                                showLogDebug(TAG_ON_RECEIVE, "isVosStarted: $isStarted")
                                showLogDebug(TAG_ON_RECEIVE, "TID: $valueTID")
                            }
                        } else {
                            // Error handling
                            showLogDebug(TAG_ON_RECEIVE, "vos_ready_error_firmware")
                        }
                        showLogDebug(TAG_ON_RECEIVE, "VOS_READY")
                    }
                }
            }
        }
    }

    private fun registerLocalBroadcast() {
        LocalBroadcastManager.getInstance(this).apply {
            registerReceiver(broadcastRcvr, IntentFilter(ACTION_FINISH))
            registerReceiver(broadcastRcvr, IntentFilter(ACTION_SCAN_COMPLETE))
            registerReceiver(broadcastRcvr, IntentFilter(PROFILE_LOADED))
            registerReceiver(broadcastRcvr, IntentFilter(VOS_READY))
            registerReceiver(broadcastRcvr, IntentFilter(PROFILE_THREAT_RESPONSE))
            registerReceiver(broadcastRcvr, IntentFilter(VGUARD_OVERLAY_DETECTED))
            registerReceiver(broadcastRcvr, IntentFilter(VGUARD_OVERLAY_DETECTED_DISABLE))
            registerReceiver(broadcastRcvr, IntentFilter(VGUARD_STATUS))
        }
    }

    private fun setupAppProtection() {
        try {
            val config = VGuardFactory.Builder()
                .setDebugable(true)
                .setAllowsArbitraryNetworking(true)
                .setMemoryConfiguration(MemoryConfiguration.DEFAULT)
                .setVGExceptionHandler(this)

            VGuardFactory().getVGuard(this, config)
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun handleException(e: Exception?) {}

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

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, savedInstanceState: Bundle) {}
}