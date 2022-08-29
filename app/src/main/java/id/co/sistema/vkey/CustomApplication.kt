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
        private const val PROFILE_LOADED = "vkey.android.vguard.PROFILE_LOADED"
        private const val VOS_FIRMWARE_RETURN_CODE_KEY = "vkey.android.vguard.FIRMWARE_RETURN_CODE"
        private const val PROFILE_THREAT_RESPONSE = "vkey.android.vguard.PROFILE_THREAT_RESPONSE"
        private const val TAG_ON_RECEIVE = "OnReceive"
        private const val TAG_VGUARD_STATUS = "VGuardStatus"
        private const val TAG_VOS_READY = "VosReady"
        private const val TAG_VGUARD_MESSAGE = "VguardMessage"
        private const val TAG_HANDLE_THREAT_POLICY = "HandleThreat"
        private const val TAG = "CustomApplication"
        private const val DEFAULT_VALUE = 0L
    }

    private fun setupVGuard(activity: Activity) {
        // TODO: setup V-OS App Protection here,
        receiveVGuardBroadcast(activity)

        // register using LocalBroadcastManager only for keeping data within your app
        registerLocalBroadcast()

        /** TODO: Setting up V-OS App Protection here,
         * Using new configuration method getVGuard(context, config)
         * */
        setupAppProtection()
    }

    private fun receiveVGuardBroadcast(activity: Activity) {
        broadcastRcvr = object : VGuardBroadcastReceiver(activity) {
            override fun onReceive(context: Context?, intent: Intent?) {
                super.onReceive(context, intent)

                when {
                    PROFILE_LOADED == intent?.action -> showLogDebug(
                        TAG_ON_RECEIVE,
                        "PROFILE_LOADED"
                    )

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
                            TAG_VGUARD_STATUS,
                            "HasExtraVGuardInitStatus: ${intent.hasExtra(VGUARD_INIT_STATUS)}"
                        )

                        if (intent.hasExtra(VGUARD_MESSAGE)) {
                            val message = intent.getStringExtra(VGUARD_MESSAGE)
                            var allMessage = "\n $VGUARD_MESSAGE : $message"
                            if (message != null) {
                                showLogDebug("MSG", message)
                            }
                            showLogDebug(TAG_VGUARD_MESSAGE, allMessage)
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

                                showLogDebug(TAG_HANDLE_THREAT_POLICY, builder.toString())
                            }
                        }

                        if (intent.hasExtra(VGUARD_INIT_STATUS)) {
                            showLogDebug(
                                TAG_VGUARD_STATUS,
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
                                        TAG_VGUARD_STATUS,
                                        "code: ${jsonObject.getString("code")}"
                                    )
                                    showLogDebug(
                                        TAG_VGUARD_STATUS,
                                        "code: ${jsonObject.getString("description")}"
                                    )
                                    message += jsonObject.toString()
                                } catch (e: Exception) {
                                    Log.e(TAG_VGUARD_STATUS, e.message.toString())
                                    e.printStackTrace()
                                }
                                showLogDebug(TAG_VGUARD_STATUS, message)
                            }
                        }

                        if (intent.hasExtra(VGUARD_SSL_ERROR_DETECTED)) {
                            showLogDebug(
                                TAG_VGUARD_STATUS,
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
                                        TAG_VGUARD_STATUS,
                                        jsonObject.getString(VGUARD_ALERT_TITLE)
                                    )
                                    showLogDebug(
                                        TAG_VGUARD_STATUS,
                                        jsonObject.getString(VGUARD_ALERT_MESSAGE)
                                    )
                                    message += jsonObject.toString()
                                } catch (e: Exception) {
                                    Log.e(TAG_VGUARD_STATUS, e.message.toString())
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    VOS_READY == intent?.action -> {
                        val firmwareReturnCode =
                            intent.getLongExtra(VOS_FIRMWARE_RETURN_CODE_KEY, DEFAULT_VALUE)
                        if (firmwareReturnCode >= DEFAULT_VALUE) {
                            // if the `VGuardManager` is not available,
                            // create a `VGuardManager` instance from `VGuardFactory`
                            if (vGuardMgr == null) {
                                vGuardMgr = VGuardFactory.getInstance()
                                hook = ActivityLifecycleHook(vGuardMgr)

                                val isStarted = vGuardMgr?.isVosStarted.toString()
                                val valueTID = vGuardMgr?.troubleshootingId.toString()
                                showLogDebug(TAG_VOS_READY, "isVosStarted: $isStarted")
                                showLogDebug(TAG_VOS_READY, "TID: $valueTID")
                            }
                        } else {
                            // Error handling
                            showLogDebug(TAG_VOS_READY, "vos_ready_error_firmware")
                        }
                        showLogDebug(TAG_VOS_READY, "VOS_READY")
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
        if (vGuardMgr == null && activity is MainActivity) {
            setupVGuard(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        vGuardMgr?.onResume(hook)
    }

    override fun onActivityPaused(activity: Activity) {
        vGuardMgr?.onPause(hook)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is MainActivity) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastRcvr)
            vGuardMgr?.destroy()
            vGuardMgr = null
        }
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, savedInstanceState: Bundle) {}
}