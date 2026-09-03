package com.lingomak.lingomakapp.ui.mantenimiento

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel
import com.lingomak.lingomakapp.data.repository.SolicitudMantenimientoRepository
import kotlinx.coroutines.launch

class SolicitudMantenimientoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SolicitudMantenimientoRepository(application)

    val solicitudesPendientes: LiveData<List<SolicitudMantenimientoModel>> =
        repository.listarPendientesObservable()

    private val _cargando = MutableLiveData<Boolean>()
    val cargando: LiveData<Boolean> get() = _cargando

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    private val _solicitudRechazada = MutableLiveData<Boolean>()
    val solicitudRechazada: LiveData<Boolean> get() = _solicitudRechazada

    fun rechazarSolicitud(
        uidSolicitud: String,
        uidAdministrador: String,
        motivoRechazo: String
    ) {
        if (uidSolicitud.isBlank()) {
            _mensajeError.value = "No se encontró la solicitud"
            return
        }
        if (uidAdministrador.isBlank()) {
            _mensajeError.value = "No se encontró el administrador autenticado"
            return
        }
        if (motivoRechazo.isBlank()) {
            _mensajeError.value = "Ingrese el motivo del rechazo"
            return
        }

        _cargando.value = true
        viewModelScope.launch {
            try {
                repository.rechazarSolicitud(
                    uidSolicitud = uidSolicitud,
                    uidAdministrador = uidAdministrador,
                    motivoRechazo = motivoRechazo.trim()
                )
                _cargando.postValue(false)
                _solicitudRechazada.postValue(true)
            } catch (e: Exception) {
                _cargando.postValue(false)
                _mensajeError.postValue(e.message ?: "Error al rechazar la solicitud")
            }
        }
    }

    fun marcarComoConvertida(
        uidSolicitud: String,
        uidAdministrador: String,
        uidMantenimientoGenerado: String,
        onSuccess: () -> Unit
    ) {
        if (uidSolicitud.isBlank() || uidAdministrador.isBlank() || uidMantenimientoGenerado.isBlank()) {
            _mensajeError.value = "Faltan datos para actualizar la solicitud"
            return
        }

        viewModelScope.launch {
            try {
                repository.marcarComoConvertida(
                    uidSolicitud = uidSolicitud,
                    uidAdministrador = uidAdministrador,
                    uidMantenimientoGenerado = uidMantenimientoGenerado
                )
                onSuccess()
            } catch (e: Exception) {
                _mensajeError.postValue(e.message ?: "Error al actualizar la solicitud")
            }
        }
    }

    fun limpiarEstadoRechazo() {
        _solicitudRechazada.value = false
    }
}
