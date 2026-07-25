package com.lingomak.lingomakapp.ui.usuarios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.data.repository.UserRepository
import kotlinx.coroutines.launch

class UsuariosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository(application)

    // La UI observa Room directamente
    val usuarios: LiveData<List<UserModel>> = repository.obtenerUsuariosObservable()

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun listarUsuarios() {
        _loading.value = true
        viewModelScope.launch {
            repository.descargarUsuariosDeFirestore()
            _loading.postValue(false)
        }
    }

    fun cambiarEstadoUsuario(uid: String, nuevoEstado: String) {
        _loading.value = true
        repository.cambiarEstadoUsuario(
            uid = uid,
            nuevoEstado = nuevoEstado,
            onSuccess = {
                _loading.value = false
                // No es necesario llamar a listarUsuarios(), Room se actualizará vía SnapshotListener 
                // o mediante la próxima descarga manual.
            },
            onError = { mensaje ->
                _loading.value = false
                _error.value = mensaje
            }
        )
    }
}
