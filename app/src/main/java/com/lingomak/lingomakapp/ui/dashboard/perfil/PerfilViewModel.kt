package com.lingomak.lingomakapp.ui.dashboard.perfil

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.data.repository.AuthRepository

class PerfilViewModel : ViewModel(){

    private val repository = AuthRepository()

    private val _usuario = MutableLiveData<UserModel>()

    val usuario : LiveData<UserModel> = _usuario

    private val _error = MutableLiveData<String>()

    val error : LiveData<String> = _error

    fun cargarUsuarioLogado(){
        repository.verificarSesionActiva(
            onSuccess = { user ->
                _usuario.value = user

            },
            onError = { mensaje ->
                _error.value = mensaje
            }
        )
    }

    fun cerrarSesion(){
        repository.cerrarSesion()
    }

}