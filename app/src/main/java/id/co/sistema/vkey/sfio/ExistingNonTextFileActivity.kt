package id.co.sistema.vkey.sfio

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.vkey.securefileio.SecureFileIO
import id.co.sistema.vkey.util.PASSWORD
import id.co.sistema.vkey.databinding.ActivityExistingNonTextFileBinding
import id.co.sistema.vkey.util.showLog
import id.co.sistema.vkey.util.showToast
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExistingNonTextFileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExistingNonTextFileBinding
    private lateinit var encryptedFileLocation: String
    private var getFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExistingNonTextFileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ibUpload.setOnClickListener { startGallery() }
        binding.btnEncryptImage.setOnClickListener { encrypt() }
        binding.btnDecryptImage.setOnClickListener { decrypt() }
    }

    private fun encrypt() {
        if (isFileNull()) {
            showToast("You must upload image first!")
            return
        }

        try {
            SecureFileIO.encryptFile(encryptedFileLocation, PASSWORD)
            binding.btnDecryptImage.isEnabled = true
            binding.tvResultEncrypt.text = "Result: Encrypted image success"
        } catch (e: Exception) {
            binding.tvResultEncrypt.text = e.message.toString()
            showLog(e)
        }
    }

    private fun isFileNull() = getFile == null

    private fun decrypt() {
        try {
            val decrypt = SecureFileIO.decryptFile(encryptedFileLocation, PASSWORD)
            val image = BitmapFactory.decodeByteArray(decrypt, 0, decrypt.size)

            binding.apply {
                ivResult.setImageBitmap(image)
                tvResultEncrypt.text = "Decrypt image success"
                btnDecryptImage.isEnabled = false
                tvPath.text = ""
            }

            // This is for requiring user to upload image again
            // after decrypt the image.
            // Because after decrypt, image file is deleted.
            getFile = null
            File(encryptedFileLocation).delete()
        } catch (e: Exception) {
            showToast("Decrypted image is failed")
            showLog(e)
        }
    }

    /**
     * Open folder with only show file type is image
     * */
    private fun startGallery() {
        val intent = Intent()
        intent.action = ACTION_GET_CONTENT
        intent.type = "image/*"
        val chooser = Intent.createChooser(intent, "Upload image")
        launcherIntentGallery.launch(chooser)
    }

    /**
     * Launcher to gallery on client
     * */
    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImage: Uri = result.data?.data as Uri
            val myFile = uriToFile(selectedImage, this@ExistingNonTextFileActivity)

            getFile = myFile
            binding.tvPath.text = encryptedFileLocation
            showLog("${selectedImage.path} | ${selectedImage.pathSegments} | ${selectedImage.encodedPath} | $selectedImage |  $encryptedFileLocation")
        }
    }

    /**
     * Changing uri image to file using write byte array
     * */
    fun uriToFile(selectedImg: Uri, context: Context): File {
        val contentResolver: ContentResolver = context.contentResolver
        val myFile = createTempFile()
        encryptedFileLocation = myFile.absolutePath

        val inputStream = contentResolver.openInputStream(selectedImg) as InputStream
        val outputStream: OutputStream = FileOutputStream(myFile)
        val buf = ByteArray(1024)
        var len: Int
        while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
        outputStream.close()
        inputStream.close()

        return myFile
    }

    /**
     * Write temporary image file
     * */
    fun createTempFile(): File {
        val dirLocation = File("${this.filesDir.absolutePath}/test")
        if (!dirLocation.exists()) dirLocation.mkdir()
        return File.createTempFile(timeStamp, ".jpg", dirLocation)
    }

    /**
     * Time stamp for result photo file
     * */
    val timeStamp: String = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        .format(System.currentTimeMillis())
}