package com.lingomak.lingomakapp.ui.mantenimiento

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.repository.MantenimientoRepository
import kotlinx.coroutines.launch

class MantenimientoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MantenimientoRepository(application)
    
    private val _listaMantenimientosOriginales = MediatorLiveData<List<MantenimientoModel>>()
    val listaMantenimientos: LiveData<List<MantenimientoModel>> = _listaMantenimientosOriginales

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    private val auth = FirebaseAuth.getInstance()

    fun cargarMantenimientos() {
        val user = auth.currentUser
        if (user == null) {
            _mensajeError.value = "Sesión no válida"
            return
        }

        // Si es operario, solo cargamos los asignados. 
        // Nota: En un sistema real verificaríamos el rol. 
        // Por ahora, si llamamos a cargarAsignados, filtramos.
    }

    fun listarMantenimientos(esOperario: Boolean) {
        val userUid = auth.currentUser?.uid ?: ""
        
        val source = if (esOperario) {
            repository.obtenerAsignadosObservable(userUid)
        } else {
            repository.obtenerTodosObservable()
        }

        _listaMantenimientosOriginales.addSource(source) {
            _listaMantenimientosOriginales.value = it
        }

        viewModelScope.launch {
            repository.descargarCambiosDeFirestore()
        }
    }

    fun agregarMantenimiento(mantenimiento: MantenimientoModel, onSuccess: () -> Unit) {
        repository.agregarMantenimiento(mantenimiento, onSuccess, { _mensajeError.postValue(it) })
    }

    fun cambiarEstadoMantenimiento(uid: String, nuevoEstado: String, onSuccess: () -> Unit) {
        repository.cambiarEstadoMantenimiento(uid, nuevoEstado, onSuccess, { _mensajeError.postValue(it) })
    }

    fun actualizarMantenimiento(mantenimiento: MantenimientoModel, onSuccess: () -> Unit) {
        repository.actualizarMantenimiento(mantenimiento, onSuccess, { _mensajeError.postValue(it) })
    }

    fun finalizarMantenimiento(
        uidMantenimiento: String,
        uidMaquinaria: String,
        fechaRealizada: String,
        horometroReal: Int,
        costoReal: Double,
        observacionesFinales: String,
        onSuccess: () -> Unit
    ) {
        repository.finalizarMantenimiento(
            uidMantenimiento,
            uidMaquinaria,
            fechaRealizada,
            horometroReal,
            costoReal,
            observacionesFinales,
            onSuccess,
            { _mensajeError.postValue(it) }
        )
    }

    fun iniciarMantenimiento(
        uidMantenimiento: String,
        uidMaquinaria: String,
        onSuccess: () -> Unit
    ) {
        repository.iniciarMantenimiento(
            uidMantenimiento,
            uidMaquinaria,
            onSuccess,
            { _mensajeError.postValue(it) }
        )
    }

    fun validarMantenimientoActivo(
        uidMaquinaria: String,
        onExiste: () -> Unit,
        onNoExiste: () -> Unit
    ) {
        repository.validarMantenimientoActivo(
            uidMaquinaria,
            onExiste,
            onNoExiste,
            { _mensajeError.postValue(it) }
        )
    }

    fun obtenerMantenimientoPorUid(uid: String, onSuccess: (MantenimientoModel) -> Unit) {
        repository.obtenerMantenimientoPorUid(uid, onSuccess, { _mensajeError.postValue(it) })
    }

    private var estaCargandoMas = false

    fun cargarSiguienteLote() {
        if (estaCargandoMas) return
        viewModelScope.launch {
            estaCargandoMas = true
            val listaActual = listaMantenimientos.value ?: emptyList()
            if (listaActual.isNotEmpty()) {
                val ultimaFecha = listaActual.last().fechaProgramada
                repository.descargarCambiosPaginados(ultimaFecha, 50)
            }
            estaCargandoMas = false
        }
    }
}
