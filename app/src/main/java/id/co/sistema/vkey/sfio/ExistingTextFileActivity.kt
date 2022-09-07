package id.co.sistema.vkey.sfio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
        if (binding.etPassword.fieldIsEmpty()) {
            showToast("Password must be filled!")
            return
        }

        if (!isFileEncrypted) {
            overwriteExistingFile()
        }

        try {
            val password = usePrefixValidPassword(binding.etPassword.text.toString())
            SecureFileIO.encryptFile(encryptedFileLocation, password)

            updateButtonState()
            binding.etPassword.clear()
            showToast("File encrypted")
        } catch (e: Exception) {
            showToast("Failed encrypted file")
            showLog(e)
        }
    }

    private fun overwriteExistingFile() {
        try {
            FileOutputStream(encryptedFileLocation).use {
                it.write(STR_INPUT.toByteArray())
                it.close()
            }
            showToast("Existing file overwritten")
        } catch (e: Exception) {
            showToast("Failed overwrite existing file")
            showLog(e)
        }
    }

    /**
     * Enabling button update and decrypt, also update state file encrypted, if encrypt is success.
     *
     * When existing file is already encrypted, button encrypt is disabled,
     * for avoiding encrypt file is already encrypting.
     * */
    private fun updateButtonState() {
        isFileEncrypted = true
        binding.btnUpdate.isEnabled = true
        binding.btnDecrypt.isEnabled = true
        binding.btnEncrypt.isEnabled = false
    }

    private fun decrypt() {
        if (binding.etPasswordDc.fieldIsEmpty()) {
            showToast("Password must be filled!")
            return
        }

        try {
            val password = usePrefixValidPassword(binding.etPasswordDc.text.toString())
            val textInString = com.vkey.securefileio.FileInputStream(encryptedFileLocation, password)
                    .bufferedReader().use { it.readText() }

            binding.tvRead.text = textInString
            binding.etPasswordDc.clear()
            showToast("Decrypted file")
        } catch (e: Exception) {
            showToast("Failed decrypted file")
            showLog(e)
        }
    }

    private fun updateContent() {
        if (binding.etInput.fieldIsEmpty() && binding.etPasswordUd.fieldIsEmpty()) {
            showToast("Content and password must be filled!")
            return
        }

        try {
            val data = binding.etInput.text.toString()
            val password = usePrefixValidPassword(binding.etPasswordUd.text.toString())
            com.vkey.securefileio.FileOutputStream(encryptedFileLocation, password).use {
                it.write(data.toByteArray())
                it.close()
            }

            clearFieldUpdateContent()
            showToast("Content is updated")
        } catch (e: Exception) {
            showToast("Failed update content")
            showLog(e)
        }
    }

    /**
     * Clearing field input to remove old value
     * */
    private fun clearFieldUpdateContent() {
        binding.etInput.clear()
        binding.etPasswordUd.clear()
    }

    private fun prepareFiles() {
        val dirLocation = File("${this.filesDir.absolutePath}/test")
        if (!dirLocation.exists()) dirLocation.mkdir()

        encryptedFileLocation = "$dirLocation/existingFile.txt"
        val file = File(encryptedFileLocation)
        if (!file.exists()) {
            file.createNewFile()
            showLog("Creating new file")
        } else {
            showLog("Use existing file")
        }
    }
}