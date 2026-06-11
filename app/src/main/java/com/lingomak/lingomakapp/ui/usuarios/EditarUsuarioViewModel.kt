package com.lingomak.lingomakapp.ui.usuarios

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lingomak.lingomakapp.data.repository.UserRepository

class EditarUsuarioViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _usuarioActualizado = MutableLiveData<Boolean>()

    val usuarioActualizado : LiveData<Boolean> = _usuarioActualizado

    private val _error = MutableLiveData<String>()

    val error : LiveData<String> = _error

    fun actualizarUsuario(
        uid: String,
        nombre: String,
        correo: String,
        rol: String
    ){
        if (uid.isEmpty() || nombre.isEmpty() || rol.isEmpty()){
            _error.value = "Complete todos los campos"
            return
        }

        repository.actualizarUsuario(
            uid = uid,
            nombre = nombre,
            correo = correo,
            rol = rol,
            onSuccess = {
                _usuarioActualizado.value = true
            },
            onError = { mensaje ->
                _error.value = mensaje
            }
        )

    }
}