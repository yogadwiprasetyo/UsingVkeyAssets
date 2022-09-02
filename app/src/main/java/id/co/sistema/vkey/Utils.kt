package id.co.sistema.vkey

import android.app.Activity
import android.util.Log
import android.widget.EditText
import android.widget.Toast


fun fieldIsEmpty(editText: EditText): Boolean {
    return editText.text.isEmpty()
}

fun showLog(tagVersion: LevelInfo, tag: String, msg: String) {
    when(tagVersion) {
        LevelInfo.Debug -> Log.d(tag, msg)
        LevelInfo.Info -> Log.i(tag, msg)
        LevelInfo.Error -> Log.e(tag, msg)
    }
}

fun Activity.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun usePrefixValidPassword(inputPassword: String): String {
    return "$PASSWORD$inputPassword"
}