package com.lingomak.lingomakapp.ui.mantenimiento

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.repository.MantenimientoRepository

class MantenimientoViewModel : ViewModel() {

    private val repository = MantenimientoRepository()
    private val _listaMantenimiento = MutableLiveData<List<MantenimientoModel>>()
    val listaMantenimientos: LiveData<List<MantenimientoModel>> get() = _listaMantenimiento
    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    fun agregarMantenimiento(
        mantenimiento: MantenimientoModel,
        onSuccess: () -> Unit
    ) {
        repository.agregarMantenimiento(
            mantenimiento = mantenimiento,
            onSuccess = {
                onSuccess()
            },
            onError = { error ->
                _mensajeError.postValue(error)
            }
        )
    }

    private var ultimoDocMantenimiento: com.google.firebase.firestore.DocumentSnapshot? = null
    private var estaCargandoMantenimientos = false
    private val listaMantenimientosAcumulados = mutableListOf<MantenimientoModel>()

    fun listarMantenimientos(reset: Boolean = false) {
        if (estaCargandoMantenimientos) return
        if (reset) {
            ultimoDocMantenimiento = null
            listaMantenimientosAcumulados.clear()
        }
        
        estaCargandoMantenimientos = true
        repository.listarMantenimientosPaginados(
            ultimoDocumento = ultimoDocMantenimiento,
            batchSize = 20,
            onSuccess = { lista, ultimo ->
                ultimoDocMantenimiento = ultimo
                listaMantenimientosAcumulados.addAll(lista)
                _listaMantenimiento.postValue(listaMantenimientosAcumulados)
                estaCargandoMantenimientos = false
            },
            onError = { error ->
                _mensajeError.postValue(error)
                estaCargandoMantenimientos = false
            }
        )
    }

    fun cargarSiguienteLote() {
        listarMantenimientos(reset = false)
    }

    fun cambiarEstadoMantenimiento(
        uid: String,
        nuevoEstado: String,
        onSuccess: () -> Unit
    ) {
        repository.cambiarEstadoMantenimiento(
            uid = uid,
            nuevoEstado = nuevoEstado,
            onSuccess = {
                onSuccess()
            },
            onError = { error ->
                _mensajeError.postValue(error)
            }
        )

    }

    fun actualizarMantenimiento(
        mantenimiento: MantenimientoModel,
        onSuccess: () -> Unit
    ) {
        repository.actualizarMantenimiento(
            mantenimiento = mantenimiento,
            onSuccess = {
                onSuccess()
            },
            onError = { error ->
                _mensajeError.postValue(error)
            }
        )
    }

    fun finalizarMantenimiento(
        uid: String,
        uidMaquinaria: String,
        fechaRealizada: String,
        horometroReal: Int,
        costoReal: Double,
        observacionesFinales: String,
        onSuccess: () -> Unit
    ) {
        repository.finalizarMantenimiento(
            uid = uid,
            uidMaquinaria = uidMaquinaria,
            fechaRealizada = fechaRealizada,
            horometroReal = horometroReal,
            costoReal = costoReal,
            observacionesFinales = observacionesFinales,
            onSuccess = {
                onSuccess()
            },
            onError =  { error ->
            _mensajeError.postValue(error)
        }
        )
    }

    fun iniciarMantenimiento(
        uidMantenimiento: String,
        uidMaquinaria: String,
        onSuccess: () -> Unit
    ) {
        repository.iniciarMantenimiento(
            uidMantenimiento = uidMantenimiento,
            uidMaquinaria = uidMaquinaria,
            onSuccess = { onSuccess() },
            onError = { error ->
                _mensajeError.postValue(error)
            }
        )
    }

    fun validarMantenimientoActivo(
        uidMaquinaria: String,
        onExiste: () -> Unit,
        onNoExiste: () -> Unit
    ) {
        repository.validarMantenimientoActivo(
            uidMaquinaria = uidMaquinaria,
            onExiste = {
                onExiste()
            },
            onNoExiste = {
                onNoExiste()
            },
            onError = { error ->
                _mensajeError.postValue(error)
            }
        )
    }

    fun actualizarMantenimientosVencidos(
        onSuccess: () -> Unit
    ) {
        repository.actualizarMantenimientosVencidos(
            onSuccess = {
                onSuccess()
            },
            onError = { error ->
                _mensajeError.postValue(error)
            }
        )
    }

    fun obtenerMantenimientoPorUid(
        uid: String,
        onSuccess: (MantenimientoModel) -> Unit
    ) {
        /*
         * El ViewModel pide el mantenimiento al Repository.
         * Si ocurre error, lo enviamos al LiveData de errores.
         */
        repository.obtenerMantenimientoPorUid(
            uid = uid,
            onSuccess = { mantenimiento ->
                onSuccess(mantenimiento)
            },
            onError = { error ->
                _mensajeError.postValue(error)
            }
        )
    }

}