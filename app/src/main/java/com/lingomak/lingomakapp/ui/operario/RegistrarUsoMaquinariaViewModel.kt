package com.lingomak.lingomakapp.ui.operario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lingomak.lingomakapp.data.repository.RegistroUsoMaquinariaRepository

class RegistrarUsoMaquinariaViewModel : ViewModel() {

    private val repository = RegistroUsoMaquinariaRepository()

    private val _registroExitoso = MutableLiveData<Boolean>()
    val registroExitoso: LiveData<Boolean> get() = _registroExitoso

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    private val _cargando = MutableLiveData<Boolean>()
    val cargando: LiveData<Boolean> get() = _cargando

    fun registrarUsoMaquinaria(
        uidMaquinaria: String,
        uidOperario: String,
        nombreOperario: String,
        correoOperario: String,
        horasUso: Int,
        observacion: String
    ) {

        if (uidMaquinaria.isEmpty()) {
            _mensajeError.value = "Seleccione una maquinaria"
            return
        }

        if (horasUso <= 0) {
            _mensajeError.value = "Las horas de uso deben ser mayor a 0"
            return
        }

        if (horasUso > 24) {
            _mensajeError.value = "No puede registrar más de 24 horas en un día"
            return
        }

        _cargando.value = true

        repository.registrarUsoMaquinaria(
            uidMaquinaria = uidMaquinaria,
            uidOperario = uidOperario,
            nombreOperario = nombreOperario,
            correoOperario = correoOperario,
            horasUso = horasUso,
            observacion = observacion,
            onSuccess = {
                _cargando.value = false
                _registroExitoso.value = true
            },
            onError = { error ->
                _cargando.value = false
                _mensajeError.value = error
            }
        )
    }
}