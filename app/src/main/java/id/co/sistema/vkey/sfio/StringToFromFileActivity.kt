package id.co.sistema.vkey.sfio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vkey.securefileio.SecureFileIO
import id.co.sistema.vkey.databinding.ActivityStringToFromFileBinding
import id.co.sistema.vkey.util.*
import java.io.File

class StringToFromFileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStringToFromFileBinding
    private lateinit var encryptedFileLocation: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStringToFromFileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prepareFiles()

        binding.apply {
            btnSave.setOnClickListener { encrypt() }
            btnRead.setOnClickListener { decrypt() }
            btnUpdate.setOnClickListener { updatePassword() }
        }
    }

    private fun encrypt() {
        if (binding.etInput.fieldIsEmpty() && binding.etPasswordEc.fieldIsEmpty()) {
            showToast("Field input and password should not empty!")
            return
        }

        try {
            val data = binding.etInput.text.toString()
            val password = usePrefixValidPassword(binding.etPasswordEc.text.toString())

            SecureFileIO.encryptString(data, encryptedFileLocation, password, false)
            updateButtonState()

            clearFieldEncrypt()
            showToast("Text encrypted")
        } catch (e: Exception) {
            showToast("Failed encrypted text")
            showLog(e)
        }
    }

    private fun updateButtonState() {
        binding.btnRead.isEnabled = true
        binding.btnUpdate.isEnabled = true
    }

    private fun clearFieldEncrypt() {
        binding.etInput.clear()
        binding.etPasswordEc.clear()
    }

    private fun decrypt() {
        if (binding.etPasswordDc.fieldIsEmpty()) {
            showToast("Field password decrypt should not empty")
            return
        }

        try {
            val password = usePrefixValidPassword(binding.etPasswordDc.text.toString())
            val decryptInString = SecureFileIO.decryptString(encryptedFileLocation, password)
            val readText = "Text: $decryptInString"

            showResultAndClearField(readText)
            showToast("File decrypted")
        } catch (e: Exception) {
            binding.tvReadFile.text = e.message.toString()
            showToast("Failed decrypted file")
            showLog(e)
        }
    }

    private fun showResultAndClearField(text: String) {
        binding.tvReadFile.text = text
        binding.etPasswordDc.clear()
    }

    private fun updatePassword() {
        val oldPassword = usePrefixValidPassword(binding.etPasswordOp.text.toString())
        val newPassword = usePrefixValidPassword(binding.etPasswordNp.text.toString())
        val confirmNewPassword = usePrefixValidPassword(binding.etPasswordCnp.text.toString())

        if ( // Op: Old password, Np: New password, Cnp: Confirm new password
            binding.etPasswordOp.fieldIsEmpty() ||
            binding.etPasswordNp.fieldIsEmpty() ||
            binding.etPasswordCnp.fieldIsEmpty()
        ) {
            showToast("Field password should not be empty!")
            return
        }

        if (newPassword != confirmNewPassword) {
            showToast("New password and confirm new password isn't same!")
            return
        }

        try {
            SecureFileIO.updateFile(encryptedFileLocation, newPassword, oldPassword)
            clearFieldUpdatePassword()
            showToast("Updating password")
        } catch (e: Exception) {
            showToast("Failed updating password")
            showLog(e)
        }
    }

    private fun clearFieldUpdatePassword() {
        binding.etPasswordOp.clear()
        binding.etPasswordNp.clear()
        binding.etPasswordCnp.clear()
    }

    private fun prepareFiles() {
        val dirLocation = File("${this.filesDir.absolutePath}/test")
        if (!dirLocation.exists()) dirLocation.mkdir()

        encryptedFileLocation = "$dirLocation/encryptedStringFile.txt"
        val file = File(encryptedFileLocation)
        if (!file.exists()) {
            file.createNewFile()
            showLog("Creating new file")
        } else {
            showLog("Use existing file")
        }
    }
}