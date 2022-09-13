package id.co.sistema.vkey.data

import retrofit2.http.Header
import retrofit2.http.POST

interface CryptotaApiService {

    @POST("encrypt")
    suspend fun encrypt(
        @Header("jwt") jwt: String
    ) : ResponseEncryptCryptota

    @POST("decrypt")
    suspend fun decrypt(
        @Header("jwt") jwt: String
    ) : ResponseDecryptCryptota
}