package id.co.sistema.vkey

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Using for splashscreen
        Handler(Looper.getMainLooper()).postDelayed(
            { startActivity(Intent(this, HelloActivity::class.java)); finish() },
            5000
        )
    }
}