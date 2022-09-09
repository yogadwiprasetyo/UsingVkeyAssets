package id.co.sistema.vkey.cryptota

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vkey.vos.signer.taInterface
import id.co.sistema.vkey.CustomApplication
import id.co.sistema.vkey.databinding.ActivityCryptotaBinding
import id.co.sistema.vkey.showLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CryptotaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCryptotaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptotaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Checking VOS is already started or not
        showLog("CryptoTA-isVosStarted: ${CustomApplication.vGuardMgr?.isVosStarted}")

        try {
            CoroutineScope(Dispatchers.IO).launch {
                // Get V-OS Crypto TA Instance
                val cryptoTA = taInterface.getInstance()
                showLog("Instance Crypto TA")

                // Load TA into V-OS
                cryptoTA.loadTA()
                showLog("Load TA into V-OS")

                // Initialize TA in V-OS
                cryptoTA.initialize()
                showLog("Initialize TA in V-OS")

                // Process the manifest file
                val processManifest = cryptoTA.processManifest(this@CryptotaActivity)
                showLog("Process the manifest file: $processManifest")

                if (processManifest < 0) {
                    showLog(Exception("Manifest error"))
                } else {
                    showLog("Process manifest success")
                }
            }

            binding.tvMessage.text = "Success integration Crypto TA"
        } catch (e: Exception) {
            showLog(e)
        }
    }
}