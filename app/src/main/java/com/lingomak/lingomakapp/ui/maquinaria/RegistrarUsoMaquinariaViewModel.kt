package com.lingomak.lingomakapp.ui.maquinaria

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lingomak.lingomakapp.data.model.RegistroUsoMaquinariaModel
import com.lingomak.lingomakapp.data.model.SuministroModel
import com.lingomak.lingomakapp.data.repository.RegistroUsoMaquinariaRepository
import kotlinx.coroutines.launch

class RegistrarUsoMaquinariaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RegistroUsoMaquinariaRepository(application)

    private val _registroExitoso = MutableLiveData<Boolean>()
    val registroExitoso: LiveData<Boolean> get() = _registroExitoso

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    private val _cargando = MutableLiveData<Boolean>()
    val cargando: LiveData<Boolean> get() = _cargando

    private val _suministroExistente = MutableLiveData<SuministroModel?>()
    val suministroExistente: LiveData<SuministroModel?> get() = _suministroExistente

    val tiposCombustible: LiveData<List<String>> = repository.obtenerTiposCombustible()

    fun cargarSuministroAsociado(uidRegistroUso: String) {
        viewModelScope.launch {
            val suministro = repository.obtenerSuministroAsociado(uidRegistroUso)
            _suministroExistente.postValue(suministro)
        }
    }

    fun registrarUsoMaquinaria(
        registroUso: RegistroUsoMaquinariaModel,
        suministro: SuministroModel?,
        esEdicion: Boolean,
        horometroFinalOriginal: Double? = null
    ) {
        if (registroUso.uidMaquinaria.isEmpty()) {
            _mensajeError.value = "Seleccione una maquinaria"
            return
        }

        if (registroUso.horometroFinal <= registroUso.horometroAnterior) {
            _mensajeError.value = "El horómetro final debe ser mayor al anterior"
            return
        }

        if (registroUso.trabajoRealizado.isEmpty()) {
            _mensajeError.value = "Describe el trabajo realizado"
            return
        }

        _cargando.value = true

        viewModelScope.launch {
            repository.registrarUsoMaquinaria(
                registroUsoModel = registroUso,
                suministroModel = suministro,
                esEdicion = esEdicion,
                horometroFinalOriginal = horometroFinalOriginal,
                onSuccess = {
                    _cargando.postValue(false)
                    _registroExitoso.postValue(true)
                },
                onError = { error ->
                    _cargando.postValue(false)
                    _mensajeError.postValue(error)
                }
            )
        }
    }
}
