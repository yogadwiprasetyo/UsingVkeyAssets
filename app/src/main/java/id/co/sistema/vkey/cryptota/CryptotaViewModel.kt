package id.co.sistema.vkey.cryptota

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.co.sistema.vkey.data.CryptotaApiService
import id.co.sistema.vkey.util.Event
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CryptotaViewModel(private val apiService: CryptotaApiService) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<Event<String>>()
    val isError: LiveData<Event<String>> = _errorMessage

    private val _isEncryptSuccess = MutableLiveData<Boolean>()
    val isEncryptSuccess: LiveData<Boolean> = _isEncryptSuccess

    private val _decryptMessage = MutableLiveData<String>()
    val decryptMessage: LiveData<String> = _decryptMessage

    fun encrypt(jwt: String) {
        viewModelScope.launch(errorHandler) {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                val data = apiService.encrypt(jwt)
                Log.d("ViewModel", "data: $data")
                _isEncryptSuccess.postValue(data.tokenStatus)
            }
            _isLoading.value = false
        }
    }

    fun decrypt(jwt: String) {
        viewModelScope.launch(errorHandler) {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                val data = apiService.decrypt(jwt)
                Log.d("ViewModel", "data: $data")
                _decryptMessage.postValue(data.payload)
            }
            _isLoading.value = false
        }
    }

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("ViewModel", "error: ${exception.message.toString()}")
        _errorMessage.value = Event(exception.message.toString())
        _isLoading.value = false
    }
}