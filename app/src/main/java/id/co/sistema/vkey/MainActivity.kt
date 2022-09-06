package id.co.sistema.vkey

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.vkey.android.internal.vguard.engine.BasicThreatInfo
import com.vkey.android.vguard.VGException
import com.vkey.android.vguard.VGuard
import com.vkey.android.vguard.VGuardFactory
import com.vkey.securefileio.SecureData.encrypt
import com.vkey.securefileio.SecureFile
import com.vkey.securefileio.SecureFileIO
import id.co.sistema.vkey.databinding.ActivityMainBinding
import id.co.sistema.vkey.sfio.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import vkey.android.vos.Vos
import vkey.android.vos.VosWrapper
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

/**
 * Found BUGS (SOLVED)
 * (Bugs) When running on debug, all project working successfully.
 * However using Run app mode, found error acquire v-os
 * */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var threatAdapter: ThreatAdapter
    private lateinit var vGuard: VGuard

    private lateinit var mVos: Vos
    private lateinit var mStartVosThread: Thread
    private lateinit var tvMessage: TextView
    private lateinit var ivLogo: ImageView

    companion object {
        private const val TAG = "MainActivity"
        private const val TAG_SFIO = "SFIO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        threatAdapter = ThreatAdapter()
        vGuard = VGuardFactory.getInstance()
        val vosWrapper = VosWrapper.getInstance(this)

        val message = "${vGuard.isVosStarted} || ${vGuard.sdkVersion()} || ${vGuard.troubleshootingId} || ${vosWrapper.firmwareVersion} || ${vosWrapper.processorVersion} || ${CustomApplication.firmwareReturnCode}"
        showLog(LevelInfo.Info, TAG, message)

        binding.apply {
            tvTid.text = "TID: ${vGuard.troubleshootingId}"
            tvValueSdkVersion.text = vGuard.sdkVersion()
            tvValueFirmwareVersion.text = vosWrapper.firmwareVersion
            tvValueProcessorVersion.text = vosWrapper.processorVersion
            tvValueFirmwareCode.text = CustomApplication.firmwareReturnCode.toString()
        }

//        startVos(this)
        initClickListener()
        setupRecyclerView()
        encryptDecryptBlockData() // Success
        // encryptDecryptStringFile() // Success
        // encryptDecryptByteFile() // Success
        // encryptExistingFile() // Success
        // writeReadEncryptedFile() // Success
        // updatingEncryptedFilePassword() // Success
        // encryptDecryptExistingFileNonText() // Success
    }

    override fun onResume() {
        super.onResume()
        showLog(LevelInfo.Info, TAG, "OnResume-isVosStarted: ${CustomApplication.isVosStart}")
        encryptDecryptBlockData()
    }

    private fun initClickListener() {
        val context = this
        binding.apply {
            // Move to Encrypting/Decrypting a Block of Data
            btnBlockData.setOnClickListener {
                startActivity(Intent(context, BlockDataActivity::class.java))
            }

            // Move to Encrypting/Decrypting a Block of Data to/from a File
            btnBlockDataFile.setOnClickListener {
                startActivity(Intent(context, BlockDataToFromFileActivity::class.java))
            }

            // Move to Encrypting/Decrypting a String to/from a File
            btnStringFile.setOnClickListener {
                startActivity(Intent(context, StringToFromFileActivity::class.java))
            }

            // Move to Encrypting an Existing File
            btnEncryptExistingFile.setOnClickListener {
                startActivity(Intent(context, ExistingTextFileActivity::class.java))
            }

            // Move to Encrypting an Existing Non Text File (Sample Image)
            btnEncryptFileNonText.setOnClickListener {
                startActivity(Intent(context, ExistingNonTextFileActivity::class.java))
            }

            // Move to Demo Secure Keyboard
            btnSecureKeyboard.setOnClickListener {
                startActivity(Intent(context, SecureKeyboardActivity::class.java))
            }
        }
    }

    /**
     * Found STILL CONFUSING - Crash detected signal = 11!!! (SOLVED)
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
                    val processorVersion = vosWrapper.processorVersion
                    val firmwareVersion = vosWrapper.firmwareVersion
                    val troubleShootingID = vGuard.troubleshootingId
                    val sdkVersion = vGuard.sdkVersion()

                    runOnUiThread {
                        binding.apply {
                            tvTid.text = "TID: $troubleShootingID"
                            tvValueSdkVersion.text = sdkVersion
                            tvValueFirmwareVersion.text = firmwareVersion
                            tvValueProcessorVersion.text = processorVersion
                            tvValueFirmwareCode.text = vosReturnCode.toString()
                        }
                    }
                } else {
                    // Failed to start V-OS
                    Log.e(TAG, "Failed to start V-OS")
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

    private fun setupRecyclerView() {
        binding.rvThreats.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = threatAdapter
        }
    }

    /**
     * Method to retrieve [BasicThreatInfo] from [CustomApplication] class
     * after being broadcast by [EventBus].
     */
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onMessageEvent(threats: ArrayList<BasicThreatInfo>) {
        val message = "Threat onMessageEvent: $threats, threats size: ${threats.size}"
        showLog(LevelInfo.Debug, TAG, message)
        binding.pbThreats.isVisible = false

        if (threats.isEmpty()) {
            binding.rvThreats.isVisible = false
            binding.tvNoThreat.isVisible = true
            return
        }

        threatAdapter.submitList(threats)

        binding.rvThreats.isVisible = true
        binding.tvNoThreat.isVisible = false
    }

    /**
     * Handle [EventBus] lifecycle.
     */
    override fun onStart() {
        super.onStart()
        showLog(LevelInfo.Debug, TAG, "EventBus OnStart")
        EventBus.getDefault().register(this)
    }

    /**
     * Handle [EventBus] lifecycle.
     */
    override fun onPause() {
        super.onPause()
        showLog(LevelInfo.Debug, TAG, "EventBus OnPause")
        EventBus.getDefault().unregister(this)
    }

    /**
     * SFIO OPERATIONS - Encrypt/Decrypt Block of Data
     * The APIs for encrypting and decrypting block of data are part of the SecureFileIO class.
     * Found Error VOSMI 60: Failed to acquire v-os (SOLVED)
     * */
    private fun encryptDecryptBlockData() {
        try {
            // The block of data in byte
            val input = STR_INPUT.toByteArray()

            // Encrypt the block of data
            val chiper: ByteArray = SecureFileIO.encryptData(input)

            // Decrypt the block encrypted block of data
            val decrypted = SecureFileIO.decryptData(chiper)
            val decryptedInput = String(decrypted)

            showLog(LevelInfo.Debug,TAG_SFIO, decryptedInput)
//            tvMessage.text = decryptedInput
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    /**
     * SFIO OPERATIONS - Encrypting/Decrypting a String to/from a File
     * The APIs for encrypting and decrypting string to/from files are part of the SecureFileIO class.
     * Found Error VOSMI 60: Failed to acquire v-os (SOLVED)
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

            showLog(LevelInfo.Debug,TAG_SFIO, decryptedString)
            tvMessage.text = decryptedString
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    /**
     * SFIO OPERATIONS - Encrypting/Decrypting a Block of Data to/from a File
     * The APIs for encrypting and decrypting block data to/from files are part of the SecureFileIO class.
     * Found Error VOSMI 60: Failed to acquire v-os (SOLVED)
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

            showLog(LevelInfo.Debug,TAG_SFIO, decryptedResult)
            tvMessage.text = decryptedResult
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    /**
     * SFIO OPERATIONS - Encrypting an Existing File
     * The API for encrypting an existing file is part of the SecureFileIO class.
     * Found UnsupportedFormatException: SFU 741 | when creating file txt from Device File Explorer (SOLVED)
     * Found NoSuchFileException: SFU 700 | when not creating file txt (SOLVED)
     * Found IllegalArgumentException: SF 597 | when creating file using java.io.File (SOLVED)
     * Found UnsupportedFormatException: SFU 741 | when creating encrypting file (SOLVED)
     *
     * SOLUTION: Must be create non encrypted file from java.io.FileOutputStream
     * */
    private fun encryptExistingFile() {
        try {
            // The path to the file to be encrypted
            val filePath = "${this.filesDir.absolutePath}/nonEncryptedFile.txt"

            // SOLUTION
            // Creating non encrypted file first
            FileOutputStream(filePath).use {
                it.write(STR_INPUT.toByteArray())
                it.close()
            }

            // If you do not wish to set a password, use an empty string
            // like "" instead.
            SecureFileIO.encryptFile(filePath, PASSWORD)
            val message = "encryptExistingFile: Success"

            Log.d(TAG_SFIO, message)
            tvMessage.text = message
        } catch (e: Exception) {
            Log.e(TAG_SFIO, e.message.toString())
            e.printStackTrace()
        }
    }

    /**
     * SFIO OPERATIONS: Writing/Reading to/from an Encrypted File
     * The API for writing/reading to/from an encrypted file is
     * part of the FileInputStream and FileOutputStream class
     *
     * Secure File, FileInputStream and FileOutputStream is working
     * */
    private fun writeReadEncryptedFile() {
        try {
            val filePath = "${this.filesDir.absolutePath}/WriteReadEncryptedFile.txt"

            // Successfully write to encrypted file
            writeToEncryptedFileUseFOS(filePath)

            // Successfully read encrypted file
            readFromEncryptedFileUseFIS(filePath)

            // Successfully write to encrypted file using random access (flags and permission)
            // writeReadEncryptedFileUseFlagAndPermissions(filePath)
        } catch (e: Exception) {
            Log.e(TAG_SFIO, e.message.toString())
            e.printStackTrace()
        }
    }

    /**
     * This will create the file if it does not exist, and if it does, it will overwrite.
     * FOS: com.vkey.securefileio.FileOutputStream
     * */
    private fun writeToEncryptedFileUseFOS(filePath: String) {
        com.vkey.securefileio.FileOutputStream(filePath, PASSWORD).use {
            it.write("Hello ".toByteArray())
            it.write("World".toByteArray())
            it.close()
        }
    }

    /**
     * Read from a file stream using FileInputStream class.
     * FIS: com.vkey.securefileio.FileInputStream
     * */
    private fun readFromEncryptedFileUseFIS(filePath: String) {
        val textInString = com.vkey.securefileio.FileInputStream(filePath, PASSWORD)
            .bufferedReader()
            .use { it.readText() }

        showLog(LevelInfo.Debug,TAG_SFIO, textInString)
        tvMessage.text = textInString
    }

    /**
     * Finally, you can use SecureFile class if you need random access to an encrypted file.
     * */
    private fun writeReadEncryptedFileUseFlagAndPermissions(filePath: String) {
        // Supported flags
        val flags = SecureFile.O_CREAT or SecureFile.O_TRUNC or SecureFile.O_RDWR;
        // Permissions must be given only when the flag is set for O_CREAT
        val permission = SecureFile.S_IRUSR or SecureFile.S_IWUSR or
                SecureFile.S_IRGRP or SecureFile.S_IROTH

        // Flags and permission parameter are optional. If not provided, O_RDONLY is assumed.
        // If you do not wish to set a password, use an empty string like "" instead.
        val input = "Write from secure file method".toByteArray()
        val secureFile = SecureFile(filePath, PASSWORD, flags, permission)
        secureFile.write(input, 0, input.size)
        secureFile.close()

        readFromEncryptedFileUseFIS(filePath)
    }

    /**
     * SFIO OPERATIONS: Updating the Password
     * Updating password encrypted file with new password and remove password
     * */
    private fun updatingEncryptedFilePassword() {
        try {
            val filePath = "${this.filesDir.absolutePath}/updatingPWEncryptedFile.txt"
            SecureFileIO.encryptString(STR_INPUT, filePath, PASSWORD, false)

            // To use updateFile() API to change the password of a previously SFIO encrypted file
            val newPassword = "New$PASSWORD"
            SecureFileIO.updateFile(filePath, newPassword, PASSWORD)

            // To use updateFile() API to remove the password of a previously SFIO encrypted file
            // SecureFileIO.updateFile(filePath, "", newPassword)

            val message = "Updating password successfully"
            showLog(LevelInfo.Debug,TAG_SFIO, message)
            tvMessage.text = message
        } catch (e: Exception) {
            Log.e(TAG_SFIO, e.message.toString())
            e.printStackTrace()
        }
    }

    /**
     * SFIO OPERATIONS - Encrypting/Decrypting non text file (sample PNG)
     * Encrypt and Decrypt file non text, in this method using PNG file as sample.
     * */
    private fun encryptDecryptExistingFileNonText() {
        try {
            val filePath = "${this.filesDir.absolutePath}/image.png"
            creatingPNGFileFromDrawable(filePath)

            SecureFileIO.encryptFile(filePath, PASSWORD)

            val decrypt = SecureFileIO.decryptFile(filePath, PASSWORD)
            val imageLogo = BitmapFactory.decodeByteArray(decrypt, 0, decrypt.size)

            tvMessage.text = "Success encrypt decrypt file non text"
            ivLogo.setImageBitmap(imageLogo)
        } catch (e: Exception) {
            Log.e(TAG_SFIO, e.message.toString())
            e.printStackTrace()
        }
    }

    /**
     * Creating file "image.png" from drawable and
     * remember must be use java.io.FilOutputStream
     * */
    private fun creatingPNGFileFromDrawable(filePath: String) {
        val bitmap = BitmapFactory.decodeResource(this.resources, R.drawable.gojek_logo)
        val bitmapData = ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, it)
            it.toByteArray()
        }

        FileOutputStream(filePath).use {
            it.write(bitmapData)
            it.flush()
            it.close()
        }

        showLog(LevelInfo.Debug,TAG_SFIO, "Creating new file png")
    }

//    override fun onNotified(p0: Int, p1: Int): Boolean {
//        showLog(LevelInfo.Debug,TAG, "$p0 || $p1")
//        return true
//    }
}