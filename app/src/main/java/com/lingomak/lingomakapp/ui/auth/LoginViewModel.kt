package com.lingomak.lingomakapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.data.repository.AuthRepository

class LoginViewModel : ViewModel(){

    private val repository = AuthRepository()

    private val _usuario = MutableLiveData<UserModel>()
    val usuario : LiveData<UserModel> = _usuario

    private val _error = MutableLiveData<String>()
    val error : LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading : LiveData<Boolean> = _loading

    fun login(correo: String, password: String){
        _loading.value = true

        repository.login(correo,password,
            onSuccess = { user ->
                _loading.value = false
                _usuario.value = user
            },
            onError = { mensaje ->
                _loading.value = false
                _error.value = mensaje
            }
        )
    }

    fun verificarSesionActiva() {
        _loading.value = true

        repository.verificarSesionActiva(
            onSuccess = { user ->

                _loading.value = false

                _usuario.value = user
            },
            onError = {

                _loading.value = false
            }
        )
    }

}