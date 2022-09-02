package id.co.sistema.vkey.sfio

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vkey.securefileio.SecureFileIO
import id.co.sistema.vkey.*
import id.co.sistema.vkey.databinding.ActivityStringToFromFileBinding
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
        if (fieldIsEmpty(binding.etInput) && fieldIsEmpty(binding.etPasswordEc)) {
            showToast("Field input and password should not empty!")
            return
        }

        val data = binding.etInput.text.toString()
        val password = usePrefixValidPassword(binding.etPasswordEc.text.toString())

        try {
            SecureFileIO.encryptString(data, encryptedFileLocation, password, false)
            binding.btnRead.isEnabled = true
            binding.btnUpdate.isEnabled = true
            showToast("Text encrypted")
        } catch (e: Exception) {
            showToast("Failed encrypted text")
            showLog(LevelInfo.Error, TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    private fun decrypt() {
        if (fieldIsEmpty(binding.etPasswordDc)) {
            showToast("Field password decrypt should not empty")
            return
        }

        val password = usePrefixValidPassword(binding.etPasswordDc.text.toString())
        try {
            val decryptInString = SecureFileIO.decryptString(encryptedFileLocation, password)
            val readText = "Text: $decryptInString"
            binding.tvReadFile.text = readText
            showToast("File decrypted")
        } catch (e: Exception) {
            showToast("Failed decrypted file")
            showLog(LevelInfo.Error, TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    private fun updatePassword() {
        val oldPassword = usePrefixValidPassword(binding.etPasswordOp.text.toString())
        val newPassword = usePrefixValidPassword(binding.etPasswordNp.text.toString())
        val confirmNewPassword = usePrefixValidPassword(binding.etPasswordCnp.text.toString())

        if (
            fieldIsEmpty(binding.etPasswordOp) ||
            fieldIsEmpty(binding.etPasswordNp) ||
            fieldIsEmpty(binding.etPasswordCnp)
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
            showToast("Updating password")
        } catch (e: Exception) {
            showToast("Failed updating password")
            showLog(LevelInfo.Error, TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    private fun prepareFiles() {
        val dirLocation = File("${this.filesDir.absolutePath}/test")
        if (!dirLocation.exists()) dirLocation.mkdir()

        encryptedFileLocation = "$dirLocation/encryptedStringFile.txt"
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