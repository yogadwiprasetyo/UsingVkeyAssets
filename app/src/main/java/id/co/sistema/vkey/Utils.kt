package id.co.sistema.vkey

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.EditText
import android.widget.Toast

/**
 * This method for simplify Log method from android framework.
 * For tracking the log, check with class name or "UtilsDebug"
 * */
fun Context.showLog(message: String) {
    val className = this::class.java.simpleName
    val name = if (className.length > LIMIT_TAG_NAME) "UtilsDebug" else className
    Log.d(name, message)
}

/**
 * This method for simplify Log method from android framework.
 * For tracking the log, check with class name or "UtilsError".
 * */
fun Context.showLog(exception: Exception) {
    val className = this::class.java.simpleName
    val name = if (className.length > LIMIT_TAG_NAME) "UtilsError" else className
    Log.e(name, exception.message.toString())
}

/**
 * Extension function for simplify showing toast
 * */
fun Activity.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Extension function for simplify checking edit text value is empty or not
 * */
fun EditText.fieldIsEmpty(): Boolean {
    return this.text.isEmpty()
}

/**
 * Extension function for simplify clearing edit text value
 * */
fun EditText.clear() {
    this.setText("")
}

/**
 * Add prefix valid password SFIO, for avoid error password not valid
 * Example: "[PASSWORD]123"
 * */
fun usePrefixValidPassword(inputPassword: String): String {
    return "$PASSWORD$inputPassword"
}