package com.lingomak.lingomakapp.ui.usuarios

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.data.repository.UserRepository

class UsuariosViewModel : ViewModel() {

    private val repository = UserRepository()

    private  val _usuarios = MutableLiveData<List<UserModel>>()

    val usuarios: LiveData<List<UserModel>> = _usuarios

    private val _error = MutableLiveData<String>()

    val error : LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()

    val loading : LiveData<Boolean> = _loading

    fun listarUsuarios(){
        _loading.value = true

        repository.listarUsuarios(
            onSuccess = { lista ->
                _loading.value = false

                _usuarios.value = lista
            },
            onError = { mensaje ->
                _loading.value = false

                _error.value = mensaje
            }
        )

    }

    fun cambiarEstadoUsuario(uid: String, nuevoEstado: String){
        _loading.value = true

        repository.cambiarEstadoUsuario(
            uid = uid,
            nuevoEstado = nuevoEstado,
            onSuccess = {
                _loading.value = false

                listarUsuarios()
            },
            onError = { mensaje ->
                _loading.value = false

                _error.value = mensaje
            }
        )
    }
}