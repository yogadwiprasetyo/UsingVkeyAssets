package id.co.sistema.vkey.data

import com.google.gson.annotations.SerializedName

data class ResponseEncryptCryptota(
	@field:SerializedName("token_status")
	val tokenStatus: Boolean
)

data class ResponseDecryptCryptota(
	@field:SerializedName("payload")
	val payload: String
)
