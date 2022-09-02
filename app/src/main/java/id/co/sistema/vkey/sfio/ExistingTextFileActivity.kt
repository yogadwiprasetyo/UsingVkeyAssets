package id.co.sistema.vkey.sfio

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vkey.securefileio.SecureFileIO
import id.co.sistema.vkey.*
import id.co.sistema.vkey.databinding.ActivityExistingTextFileBinding
import java.io.File
import java.io.FileOutputStream

class ExistingTextFileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExistingTextFileBinding
    private lateinit var encryptedFileLocation: String
    private var isFileEncrypted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExistingTextFileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prepareFiles()

        binding.apply {
            btnEncrypt.setOnClickListener { encrypt() }
            btnDecrypt.setOnClickListener { decrypt() }
            btnUpdate.setOnClickListener { updateContent() }
        }
    }

    private fun encrypt() {
        if (fieldIsEmpty(binding.etPassword)) {
            showToast("Password must be filled!")
            return
        }

        try {
            val password = usePrefixValidPassword(binding.etPassword.text.toString())
            SecureFileIO.encryptFile(encryptedFileLocation, password)

            isFileEncrypted = true
            showToast("File encrypted")
        } catch (e: Exception) {
            showToast("Failed encrypted file")
            showLog(LevelInfo.Debug, TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    private fun decrypt() {
        if (fieldIsEmpty(binding.etPasswordDc)) {
            showToast("Password must be filled!")
            return
        }

        try {
            val password = usePrefixValidPassword(binding.etPasswordDc.text.toString())
            val textInString = com.vkey.securefileio.FileInputStream(encryptedFileLocation, password)
                .bufferedReader().use { it.readText() }

            binding.tvRead.text = textInString
            showToast("Decrypted file")
        } catch (e: Exception) {
            showToast("Failed decrypted file")
            showLog(LevelInfo.Error, TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    private fun updateContent() {
        if (fieldIsEmpty(binding.etInput) && fieldIsEmpty(binding.etPasswordUd)) {
            showToast("Content and password must be filled!")
            return
        }

        if (isFileEncrypted) {
            overwriteEncryptedFile()
        } else {
            overwriteExistingFile()
        }

        showToast("Content is updated")
    }

    private fun overwriteEncryptedFile() {
        try {
            val file = File(encryptedFileLocation)
            val data = binding.etInput.text.toString()
            val password = usePrefixValidPassword(binding.etPasswordUd.text.toString())
            com.vkey.securefileio.FileOutputStream(file, password).use {
                it.write(data.toByteArray())
                it.close()
            }
            showToast("Encrypted file overwritten")
        } catch (e: Exception) {
            showToast("Failed overwrite encrypted file")
            showLog(LevelInfo.Error, TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    private fun overwriteExistingFile() {
        try {
            val file = File(encryptedFileLocation)
            val data = binding.etInput.text.toString()
            FileOutputStream(file).use {
                it.write(data.toByteArray())
                it.close()
            }
            showToast("Existing file overwritten")
        } catch (e: Exception) {
            showToast("Failed overwrite existing file")
            showLog(LevelInfo.Error, TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    private fun prepareFiles() {
        val dirLocation = File("${this.filesDir.absolutePath}/test")
        if (!dirLocation.exists()) dirLocation.mkdir()

        encryptedFileLocation = "$dirLocation/existingFile.txt"
        val file = File(encryptedFileLocation)
        if (!file.exists()) {
            file.createNewFile()
            showLog(LevelInfo.Debug, TAG, "Creating new file")
        } else {
            showLog(LevelInfo.Debug, TAG, "Use existing file")
        }
    }

    companion object {
        private const val TAG = "StringToFromFile"
    }
}