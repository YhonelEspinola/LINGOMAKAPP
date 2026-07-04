package com.lingomak.lingomakapp.ui.alertas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.repository.AlertasRepository

class AlertasViewModel : ViewModel() {

    private val repository = AlertasRepository()

    private val _listaAlertas = MutableLiveData<List<AlertaModel>>()
    val listaAlertas: LiveData<List<AlertaModel>> get() = _listaAlertas

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    fun listarAlertas() {
        repository.listarAlertas(
            onSuccess = { lista ->
                _listaAlertas.postValue(lista)
            },
            onError = { error ->
                _mensajeError.postValue(error)
            }
        )
    }
}