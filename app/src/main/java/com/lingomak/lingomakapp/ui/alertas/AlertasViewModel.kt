package com.lingomak.lingomakapp.ui.alertas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.repository.AlertasRepository
import com.lingomak.lingomakapp.data.repository.MantenimientoRepository

class AlertasViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        AlertasRepository(application)

    private val mantenimientoRepository =
        MantenimientoRepository()

    private val _listaAlertas = MutableLiveData<List<AlertaModel>>()
    val listaAlertas: LiveData<List<AlertaModel>> get() = _listaAlertas

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    fun listarAlertas() {
        mantenimientoRepository.actualizarMantenimientosVencidos(
            onSuccess = {
                repository.listarAlertas(
                    onSuccess = { lista ->
                        _listaAlertas.postValue(lista)
                    },
                    onError = { error ->
                        _mensajeError.postValue(error)
                    }
                )
            },
            onError = { error ->
                _mensajeError.postValue(error)
            }
        )
    }
}