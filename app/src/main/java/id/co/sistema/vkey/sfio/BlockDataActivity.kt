package id.co.sistema.vkey.sfio

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vkey.securefileio.SecureFileIO
import id.co.sistema.vkey.util.clear
import id.co.sistema.vkey.databinding.ActivityBlockDataBinding
import id.co.sistema.vkey.util.fieldIsEmpty
import id.co.sistema.vkey.util.showLog

class BlockDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockDataBinding
    private lateinit var chiper: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEncrypt.setOnClickListener { encrypt() }
        binding.btnDecrypt.setOnClickListener { decrypt() }
    }

    private fun encrypt() {
        if (binding.etInput.fieldIsEmpty()) {
            binding.etInput.error = "Input can't be empty!"
            return
        }

        try {
            val input = binding.etInput.text.toString().toByteArray()
            chiper = SecureFileIO.encryptData(input)

            binding.apply {
                tvCiphertext.text = chiper.toString()
                btnDecrypt.isEnabled = true
                etInput.clear()
            }
        } catch (e: Exception) {
            showLog(e)
        }
    }

    private fun decrypt() {
        if (chiper.isEmpty()) {
            Toast.makeText(this, "Chiper is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val decrypted = SecureFileIO.decryptData(chiper)
            val decryptedInString = String(decrypted)

            binding.tvDecryptedText.text = decryptedInString
        } catch (e: Exception) {
            showLog(e)
        }
    }
}