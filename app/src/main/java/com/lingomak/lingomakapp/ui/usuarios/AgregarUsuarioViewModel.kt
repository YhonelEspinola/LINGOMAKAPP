package com.lingomak.lingomakapp.ui.usuarios

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.data.repository.UserRepository

class AgregarUsuarioViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _usuarioCreado = MutableLiveData<Boolean>()

    val usuarioCreado : LiveData<Boolean> = _usuarioCreado

    private val _error = MutableLiveData<String>()

    val error : LiveData<String> = _error

    fun crearUsuario(
        nombre: String,
        correo: String,
        password: String,
        rol: String
    ){
        if (nombre.isEmpty() || correo.isEmpty() || rol.isEmpty() ){
            _error.value = "Complete todos los campos obligatorios"
            return
        }

        repository.crearUsuarioConFunction(
            nombre = nombre,
            correo = correo,
            password = password,
            rol = rol,
            creadoPor = "ADMIN",
            onSuccess = {
                _usuarioCreado.value = true
            },
            onError = { mensaje ->
                _error.value = mensaje
            }
        )
    }
}