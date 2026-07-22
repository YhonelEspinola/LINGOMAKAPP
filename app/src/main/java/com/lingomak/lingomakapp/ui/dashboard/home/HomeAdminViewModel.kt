package com.lingomak.lingomakapp.ui.dashboard.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.data.repository.AuthRepository
import com.lingomak.lingomakapp.data.repository.MantenimientoRepository
import com.lingomak.lingomakapp.data.repository.RepuestoRepository

class HomeAdminViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val repuestoRepository = RepuestoRepository(application)
    private val mantenimientoRepository = MantenimientoRepository(application)

    private val _usuario = MutableLiveData<UserModel>()
    val usuario: LiveData<UserModel> = _usuario

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Información rápida conectada a Room (Tiempo real)
    val totalRepuestos: LiveData<Int> = repuestoRepository.obtenerRepuestosObservable().map { it.size }
    
    val totalMantenimientos: LiveData<Int> = mantenimientoRepository.obtenerTodosObservable().map { it.size }
    
    val stockCritico: LiveData<Int> = repuestoRepository.obtenerRepuestosObservable().map { lista ->
        lista.count { it.stockActual <= it.stockMinimo }
    }

    fun cargarUsuarioLogado() {
        authRepository.verificarSesionActiva(
            onSuccess = { user ->
                _usuario.postValue(user)
            },
            onError = { mensaje ->
                _error.postValue(mensaje)
            }
        )
    }
}