package id.co.sistema.vkey

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vkey.android.vguard.VGException
import com.vkey.securefileio.SecureFileIO
import vkey.android.vos.Vos
import vkey.android.vos.VosWrapper

class MainActivity : AppCompatActivity(), VosWrapper.Callback {
    private lateinit var mVos: Vos
    private lateinit var mStartVosThread: Thread
    private lateinit var tvMessage: TextView

    companion object {
        private const val TAG = "MainActivity"
        private const val TAG_SFIO = "SecureFileIO"
        private const val STR_INPUT =
            "Quick brown fox jumps over the lazy dog. 1234567890 some_one@somewhere.com"
        private const val PASSWORD = "P@ssw0rd"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvMessage = findViewById(R.id.tv_message)

        mVos = Vos(this)
        mVos.registerVosWrapperCallback(this)

        startVos(this)
        // encryptDecryptBlockData() // Error
        // encryptDecryptStringFile() // Error
        // encryptDecryptByteFile() // Error
    }

    override fun onNotified(p0: Int, p1: Int): Boolean {
        showLogDebug(TAG, "$p0 || $p1")
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVos()
    }

    /**
     * TODO: STILL CONFUSING
     * Because method is not yet running well
     * */
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
                    val vosWrapper = VosWrapper.getInstance(ctx)
                    val version = vosWrapper.processorVersion
                    val troubleShootingID = vosWrapper.troubleshootingId
                    showLogDebug(
                        TAG,
                        "ProcessorVers: $version || TroubleShootingID: $troubleShootingID"
                    )
//                    tvMessage.text = "ProcessorVers: $version || TroubleShootingID: $troubleShootingID"
                } else {
                    // Failed to start V-OS
                    Log.e(TAG, "Failed to start V-OS")
//                    tvMessage.text = "Failed to start V-OS"
                }
            } catch (e: VGException) {
                Log.e(TAG, e.message.toString())
                e.printStackTrace()
            }
        }

        mStartVosThread.start()
    }

    private fun stopVos() {
        mVos.stop()
    }

    /**
     * SFIO OPERATIONS - Encrypt/Decrypt Block of Data
     * The APIs for encrypting and decrypting block of data are part of the SecureFileIO class.
     * TODO: Error VOSMI 60: Failed to acquire v-os
     * */
    private fun encryptDecryptBlockData() {
        try {
            // The block of data in byte
            val input: ByteArray = STR_INPUT.toByteArray()

            // Encrypt the block of data
            val chiper: ByteArray = SecureFileIO.encryptData(input)

            // Decrypt the block encrypted block of data
            val decrypted = SecureFileIO.decryptData(chiper)
            val decryptedInput = String(decrypted)

            showLogDebug(TAG_SFIO, decryptedInput)
            tvMessage.text = decryptedInput
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    /**
     * SFIO OPERATIONS - Encrypting/Decrypting a String to/from a File
     * The APIs for encrypting and decrypting string to/from files are part of the SecureFileIO class.
     * TODO: Error VOSMI 60: Failed to acquire v-os
     * */
    private fun encryptDecryptStringFile() {
        try {
            // the path to the encrypted file
            val encryptedFilePath = "${this.filesDir.absolutePath}/encryptedFile.txt"

            // Write the string to the encrypted file. If you do not wish to set a
            // password, use an empty string like "" instead. Setting the last
            // parameter to `true` will write the file atomically.
            SecureFileIO.encryptString(STR_INPUT, encryptedFilePath, PASSWORD, false)

            // Decrypt the encrypted file in the string format
            val decryptedString = SecureFileIO.decryptString(encryptedFilePath, PASSWORD)

            showLogDebug(TAG_SFIO, decryptedString)
            tvMessage.text = decryptedString
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    /**
     * SFIO OPERATIONS - Encrypting/Decrypting a Block of Data to/from a File
     * The APIs for encrypting and decrypting block data to/from files are part of the SecureFileIO class.
     * TODO: Error VOSMI 60: Failed to acquire v-os
     * */
    private fun encryptDecryptByteFile() {
        try {
            val input = STR_INPUT.toByteArray()

            // The path to the encrypted file
            val encryptedFilePath = "${this.filesDir.absolutePath}/encryptedFile.txt"

            // Write the block data to the encrypted file. If you do not wish to set a
            // password, use an empty string like "" instead. Setting the last
            // parameter to `true` will write the file atomically.
            SecureFileIO.encryptData(input, encryptedFilePath, PASSWORD, false)

            // Decrypt the encrypted file in the byte format
            val decrypted = SecureFileIO.decryptFile(encryptedFilePath, PASSWORD)
            val decryptedResult = String(decrypted)

            showLogDebug(TAG_SFIO, decryptedResult)
            tvMessage.text = decryptedResult
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            e.printStackTrace()
        }
    }
}