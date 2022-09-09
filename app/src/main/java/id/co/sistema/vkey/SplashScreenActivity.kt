package id.co.sistema.vkey

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vkey.android.internal.vguard.engine.BasicThreatInfo
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        supportActionBar?.hide()
    }

    /**
     * Method to retrieve [BasicThreatInfo] from [CustomApplication] class
     * after being broadcast by [EventBus].
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onMessageEvent(isStarted: Boolean?) {
        showLog("Custom Application-isVosStarted: $isStarted")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /**
     * Handle [EventBus] lifecycle.
     */
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    /**
     * Handle [EventBus] lifecycle.
     */
    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }
}