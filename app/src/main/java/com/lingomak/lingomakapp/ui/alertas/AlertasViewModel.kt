package com.lingomak.lingomakapp.ui.alertas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.repository.AlertasRepository
import com.lingomak.lingomakapp.data.repository.MantenimientoRepository

class AlertasViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AlertasRepository(application)
    private val mantenimientoRepository = MantenimientoRepository(application)

    // UI observa Room directamente
    val listaAlertas: LiveData<List<AlertaModel>> = repository.obtenerAlertasObservable()
    val totalAlertas: LiveData<Int> = repository.obtenerTotalAlertasObservable()

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun listarAlertas(esOperario: Boolean = false) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        _loading.value = true
        
        mantenimientoRepository.actualizarMantenimientosVencidos(
            onSuccess = {
                repository.listarAlertas(
                    userUid = userUid,
                    esOperario = esOperario,
                    onSuccess = {
                        _loading.postValue(false)
                    },
                    onError = { error ->
                        _loading.postValue(false)
                        _mensajeError.postValue(error)
                    }
                )
            },
            onError = { error ->
                _loading.postValue(false)
                _mensajeError.postValue(error)
            }
        )
    }
}
