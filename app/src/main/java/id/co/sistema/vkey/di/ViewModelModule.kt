package id.co.sistema.vkey.di

import id.co.sistema.vkey.cryptota.CryptotaViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { CryptotaViewModel(get()) }
}