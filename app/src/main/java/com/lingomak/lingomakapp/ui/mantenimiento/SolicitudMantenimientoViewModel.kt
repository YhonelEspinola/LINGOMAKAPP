package com.lingomak.lingomakapp.ui.mantenimiento

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel
import com.lingomak.lingomakapp.data.repository.SolicitudMantenimientoRepository

class SolicitudMantenimientoViewModel : ViewModel() {

    private val repository =
        SolicitudMantenimientoRepository()

    private val _solicitudesPendientes =
        MutableLiveData<List<SolicitudMantenimientoModel>>()

    val solicitudesPendientes:
            LiveData<List<SolicitudMantenimientoModel>>
        get() = _solicitudesPendientes

    private val _cargando =
        MutableLiveData<Boolean>()

    val cargando: LiveData<Boolean>
        get() = _cargando

    private val _mensajeError =
        MutableLiveData<String>()

    val mensajeError: LiveData<String>
        get() = _mensajeError


    private val _solicitudRechazada =
        MutableLiveData<Boolean>()

    val solicitudRechazada: LiveData<Boolean>
        get() = _solicitudRechazada


    fun listarSolicitudesPendientes() {

        _cargando.value = true

        repository.listarSolicitudesPendientes(
            onSuccess = { lista ->

                _cargando.postValue(false)

                _solicitudesPendientes.postValue(lista)
            },

            onError = { error ->

                _cargando.postValue(false)
                _mensajeError.postValue(error)
            }
        )
    }


    fun rechazarSolicitud(
        uidSolicitud: String,
        uidAdministrador: String,
        motivoRechazo: String
    ) {

        if (uidSolicitud.isBlank()) {
            _mensajeError.value =
                "No se encontró la solicitud"
            return
        }

        if (uidAdministrador.isBlank()) {
            _mensajeError.value =
                "No se encontró el administrador autenticado"
            return
        }

        if (motivoRechazo.isBlank()) {
            _mensajeError.value =
                "Ingrese el motivo del rechazo"
            return
        }

        _cargando.value = true

        repository.rechazarSolicitud(
            uidSolicitud = uidSolicitud,
            uidAdministrador = uidAdministrador,
            motivoRechazo = motivoRechazo.trim(),

            onSuccess = {

                _cargando.postValue(false)
                _solicitudRechazada.postValue(true)
                listarSolicitudesPendientes()
            },

            onError = { error ->

                _cargando.postValue(false)
                _mensajeError.postValue(error)
            }
        )
    }


    fun marcarComoConvertida(
        uidSolicitud: String,
        uidAdministrador: String,
        uidMantenimientoGenerado: String,
        onSuccess: () -> Unit
    ) {

        if (
            uidSolicitud.isBlank() ||
            uidAdministrador.isBlank() ||
            uidMantenimientoGenerado.isBlank()
        ) {
            _mensajeError.value =
                "Faltan datos para actualizar la solicitud"
            return
        }

        _cargando.value = true

        repository.marcarComoConvertida(
            uidSolicitud = uidSolicitud,
            uidAdministrador = uidAdministrador,
            uidMantenimientoGenerado =
                uidMantenimientoGenerado,

            onSuccess = {

                _cargando.postValue(false)
                onSuccess()
            },

            onError = { error ->

                _cargando.postValue(false)
                _mensajeError.postValue(error)
            }
        )
    }

    fun limpiarEstadoRechazo() {
        _solicitudRechazada.value = false
    }
}