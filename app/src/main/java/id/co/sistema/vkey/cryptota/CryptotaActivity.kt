package id.co.sistema.vkey.cryptota

import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.vkey.vos.signer.taInterface
import id.co.sistema.vkey.*
import id.co.sistema.vkey.databinding.ActivityCryptotaBinding
import id.co.sistema.vkey.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel

class CryptotaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCryptotaBinding
    private lateinit var mCryptoTA: taInterface

    private val viewModel by viewModel<CryptotaViewModel>()
    private var jwt = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptotaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setViewModelObservers()

        binding.btnSignMessage.setOnClickListener {
            if (binding.etMessage.text.isEmpty()) {
                showToast("Message can not be empty!")

                return@setOnClickListener
            }

            val message = wrappingRequest(binding.etMessage.text.toString())
            showToast("Signing message ...")

            // When doing signing message, disable button and hide view.
            // Only showing progressbar to waiting the process
            binding.btnSignMessage.isEnabled = false
            binding.btnDecrypt.isEnabled = false
            binding.tvSignMessage.isVisible = false
            binding.tvMessage.isVisible = false
            binding.pb.isVisible = true

            CoroutineScope(Dispatchers.Main).launch {
                signMessage(message)
            }
        }

        binding.btnDecrypt.setOnClickListener {
            viewModel.decrypt(jwt)
        }
    }

    private fun setViewModelObservers() {
        val owner = this@CryptotaActivity
        with(viewModel) {
            isEncryptSuccess.observe(owner, ::onEncrypted)
            decryptMessage.observe(owner, ::onMessageFetched)
            isError.observe(owner, ::onError)
            isLoading.observe(owner, ::onLoading)
        }
    }

    private fun onEncrypted(isSuccess: Boolean) {
        binding.apply {
            btnDecrypt.isEnabled = isSuccess
            tvSignMessage.isVisible = isSuccess
            tvSignMessage.text = "Signed message: $jwt"
        }
        showToast("is encrypted success: $isSuccess")
    }

    private fun onMessageFetched(message: String) {
        binding.tvMessage.text = message
        binding.tvMessage.isVisible = true
    }

    private fun onError(event: Event<String>) {
        val errorMessage = event.getContentIfNotHandled() ?: "Error fetched data"
        binding.tvMessage.text = "Error: $errorMessage"
        binding.tvMessage.isVisible = true
        showToast("Failed encrypted message to server!")
    }

    private fun onLoading(isLoading: Boolean) {
        binding.pb.isVisible = isLoading
        binding.btnSignMessage.isEnabled = !isLoading
    }

    private suspend fun signMessage(message: String) {

        // Checking VOS is already started or not
        showLog("CryptoTA-isVosStarted: ${CustomApplication.vGuardMgr?.isVosStarted}")

        withContext(Dispatchers.IO) {
            // Get V-OS Crypto TA Instance
            mCryptoTA = taInterface.getInstance()
            showLog("Instance Crypto TA")

            // Load TA into V-OS
            mCryptoTA.loadTA()
            showLog("Load TA into V-OS")

            // Initialize TA in V-OS
            mCryptoTA.initialize()
            showLog("Initialize TA in V-OS")

            // Process the manifest file
            val processManifest = mCryptoTA.processManifest(this@CryptotaActivity)
            showLog("Process the manifest file: $processManifest")

            if (processManifest < 0) {
                showLog(Exception("Manifest error"))
                showToast("Manifest error")
                return@withContext
            }

            /**
             * This is required for using [taInterface.signMsg]
             * Set the trusted time server URL
             * */
            CustomApplication.vosWrapper?.setTrustedTimeServerUrl(BASE_URL)

            showLog("Process manifest success")
            val payload = setupPayload(message.toByteArray())
            val bytes = payload.toByteArray()

            // 4th argument is "2" to use SHA256. Change to "1" for SHA1
            // parameter alias in signMsg method must be "Sistema_CA_RSA_2022" as key in datastore
            // If error value is positive, signing message is successful
            val error = IntArray(1)
            val signature = mCryptoTA.signMsg(bytes, KEY_DATASTORE, 2, error)
            showLog(Exception("Error signing value: ${error[0]}"))

            if (signature == null) {
                showLog("Signature null value: $signature")
                val unloadTaResult = mCryptoTA.unloadTA()
                showLog("Unload TA result: $unloadTaResult")
                return@withContext
            }

            showLog("Signature not null value: $signature")
            val unloadTaResult = mCryptoTA.unloadTA()
            showLog("Unload TA result: $unloadTaResult")
            jwt = "$payload.${encodeUrlSafe(signature)}"
            showLog("Signature JWT value: $jwt")
            viewModel.encrypt(jwt)
        }


        showToast("Message signed... Waiting to verify from server")
    }

    /**
     * Merge header and data encode to prepare payload for jwt
     * */
    private fun setupPayload(message: ByteArray): String {
        val headerEncode = encodeUrlSafe(HEADER_JWT.toByteArray())
        val dataEncode = encodeUrlSafe(message)
        showLog("setupPayload: $headerEncode.$dataEncode")
        return "$headerEncode.$dataEncode"
    }

    /**
     * Encode url safe from byte array.
     * Using [Base64.encode] method
     * */
    private fun encodeUrlSafe(data: ByteArray): String {
        val flags = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        val encode: ByteArray = Base64.encode(data, flags)
        showLog("encodeUrlSafe: ${String(encode, 0, encode.size)}")
        return String(encode, 0, encode.size)
    }

    /**
     * Wrapping the input for preparing request to sent with jwt as payload
     * This is required configuration for jwt decode on server.
     * */
    private fun wrappingRequest(input: String) = """{"message": "$input"}"""

    companion object {
        private const val HEADER_JWT = "{\"alg\": \"RS256\",\"typ\": \"JWT\"}"
    }
}