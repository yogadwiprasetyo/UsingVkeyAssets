package id.co.sistema.vkey.sfio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vkey.android.secure.keyboard.VKeySecureKeypad
import id.co.sistema.vkey.R
import id.co.sistema.vkey.databinding.ActivitySecureKeyboardBinding


class SecureKeyboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecureKeyboardBinding

    init {
        VKeySecureKeypad.VKSecureKeyboardLayout = R.xml.vk_input1
        VKeySecureKeypad.VKSecureEditTextAttrs = R.styleable.VKSecureEditText
        VKeySecureKeypad.VKSecureEditTextInDialogIdx = R.styleable.VKSecureEditText_inDialog
        VKeySecureKeypad.VKSecureEditTextRandomizedIdx = R.styleable.VKSecureEditText_randomized
        VKeySecureKeypad.VKSecureEditTextAdjustModeIdx = R.styleable.VKSecureEditText_adjustMode
        VKeySecureKeypad.qwertyLayout = R.xml.vk_keyboard_qwerty
        VKeySecureKeypad.qwertyCapsLayout = R.xml.vk_keyboard_qwerty_caps
        VKeySecureKeypad.numbersSymbolsLayout = R.xml.vk_keyboard_numbers_symbols
        VKeySecureKeypad.numbersSymbolsLayout2 = R.xml.vk_keyboard_numbers_symbols2
        VKeySecureKeypad.numbersLayout = R.xml.vk_keyboard_numbers
        VKeySecureKeypad.numbersLayoutHorizontal = R.xml.vk_keyboard_numbers_symbol_horizontal
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecureKeyboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            btnSubmitNormal.setOnClickListener {
                val textNormal = etNormal.text.toString()
                tvSkNormal.text = textNormal
                etNormal.setText("")
            }

            btnSubmitNumericNormal.setOnClickListener {
                val numericNormal = etInputNumericNormal.text.toString()
                tvSkNumericNormal.text = numericNormal
                etInputNumericNormal.setText("")
            }

            btnSubmitNumericRandom.setOnClickListener {
                val numericRandom = etInputNumericRandom.text.toString()
                tvSkNumericRandom.text = numericRandom
                etInputNumericRandom.setText("")
            }
        }
    }
}