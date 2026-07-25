package com.lingomak.lingomakapp.ui.usuarios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lingomak.lingomakapp.data.repository.UserRepository

class EditarUsuarioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository(application)

    private val _usuarioActualizado = MutableLiveData<Boolean>()
    val usuarioActualizado: LiveData<Boolean> = _usuarioActualizado

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun actualizarUsuario(
        uid: String,
        nombre: String,
        correo: String,
        rol: String
    ) {
        if (uid.isEmpty() || nombre.isEmpty() || rol.isEmpty()) {
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
