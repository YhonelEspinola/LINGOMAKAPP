package com.lingomak.lingomakapp.ui.mantenimiento

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.remoteConfig
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.repository.ConsumoRepuesto
import com.lingomak.lingomakapp.data.repository.MantenimientoRepository
import com.lingomak.lingomakapp.data.repository.MaquinariaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MantenimientoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MantenimientoRepository(application)
    private val maquinariaRepository = MaquinariaRepository(application)
    private val movimientoDao = AppDatabase.getInstance(application).movimientoDao()

    private val _listaMantenimientosOriginales = MediatorLiveData<List<MantenimientoModel>>()
    val listaMantenimientos: LiveData<List<MantenimientoModel>> = _listaMantenimientosOriginales

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> = _mensajeError

    private val _reporteGenerado = MutableLiveData<String?>()
    val reporteGenerado: LiveData<String?> = _reporteGenerado

    private val _loadingAI = MutableLiveData<Boolean>()
    val loadingAI: LiveData<Boolean> = _loadingAI

    private val auth = FirebaseAuth.getInstance()

    fun listarMantenimientos(soloAsignados: Boolean = false) {
        val liveData = if (soloAsignados) {
            val userUid = auth.currentUser?.uid ?: ""
            repository.obtenerAsignadosObservable(userUid)
        } else {
            repository.obtenerTodosObservable()
        }

        _listaMantenimientosOriginales.addSource(liveData) { lista ->
            _listaMantenimientosOriginales.value = lista
        }
    }

    fun agregarMantenimiento(mantenimiento: MantenimientoModel, onExito: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.agregarMantenimiento(mantenimiento, {
                viewModelScope.launch(Dispatchers.Main) { onExito() }
            }, { error ->
                _mensajeError.postValue(error)
            })
        }
    }

    fun cambiarEstadoMantenimiento(uid: String, nuevoEstado: String, onExito: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cambiarEstadoMantenimiento(uid, nuevoEstado, {
                viewModelScope.launch(Dispatchers.Main) { onExito() }
            }, { error ->
                _mensajeError.postValue(error)
            })
        }
    }

    fun actualizarMantenimiento(mantenimiento: MantenimientoModel, onExito: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.actualizarMantenimiento(mantenimiento, {
                viewModelScope.launch(Dispatchers.Main) { onExito() }
            }, { error ->
                _mensajeError.postValue(error)
            })
        }
    }

    fun finalizarMantenimiento(
        uid: String,
        uidMaquinaria: String,
        fechaRealizada: String,
        horometroReal: Int,
        costoReal: Double,
        observacionesFinales: String,
        insumos: List<ConsumoRepuesto>,
        imagenesFinalizacionLocal: List<String>,
        onExito: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.finalizarMantenimiento(
                uid, uidMaquinaria, fechaRealizada, horometroReal,
                costoReal, observacionesFinales, insumos, imagenesFinalizacionLocal,
                { viewModelScope.launch(Dispatchers.Main) { onExito() } },
                { error -> _mensajeError.postValue(error) }
            )
        }
    }

    fun iniciarMantenimiento(uidMantenimiento: String, uidMaquinaria: String, onExito: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.iniciarMantenimiento(uidMantenimiento, uidMaquinaria, {
                viewModelScope.launch(Dispatchers.Main) { onExito() }
            }, { error ->
                _mensajeError.postValue(error)
            })
        }
    }

    fun validarMantenimientoActivo(uidMaquinaria: String, onExiste: () -> Unit, onNoExiste: () -> Unit) {
        repository.validarMantenimientoActivo(uidMaquinaria, onExiste, onNoExiste) { error ->
            _mensajeError.postValue(error)
        }
    }

    fun obtenerMantenimientoPorUid(uid: String, onExito: (MantenimientoModel) -> Unit) {
        repository.obtenerMantenimientoPorUid(uid, { model ->
            viewModelScope.launch(Dispatchers.Main) { onExito(model) }
        }) { error -> _mensajeError.postValue(error) }
    }

    fun obtenerMaquinariaPorUid(uid: String, onExito: (MaquinariaModel) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val maquinaria = maquinariaRepository.obtenerMaquinariaPorUid(uid)
            viewModelScope.launch(Dispatchers.Main) {
                if (maquinaria != null) onExito(maquinaria)
                else _mensajeError.postValue("Maquinaria no encontrada")
            }
        }
    }

    // ===================================================================
    // LÓGICA DE REPORTES CON IA (CENTRALIZADA)
    // ===================================================================

    private fun getModelName(): String {
        val model = Firebase.remoteConfig.getString("ia_model_name")
        return if (model.isBlank()) "gemini-2.5-flash-lite" else model
    }

    fun generarReporteIA(mantenimiento: MantenimientoModel) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingAI.postValue(true)
            try {
                val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(modelName = getModelName())

                val movimientos = movimientoDao.obtenerPorMantenimiento(mantenimiento.uid)
                val repuestosStr = if (movimientos.isEmpty()) "Ninguno registrado"
                else movimientos.joinToString(", ") { "${it.cantidad} unidades (ID: ${it.repuestoUid})" }

                val prompt = """
                    Genera un reporte técnico profesional de mantenimiento para la empresa Lingomak.
                    Contexto real de campo:
                    - Maquinaria: ${mantenimiento.nombreMaquinaria}
                    - Tipo de Mantenimiento: ${mantenimiento.tipoMantenimiento}
                    - Descripción inicial: ${mantenimiento.descripcion}
                    - Observaciones técnico: ${mantenimiento.observaciones}
                    - Horómetro Real: ${mantenimiento.horometroReal} h (Programado: ${mantenimiento.horometroProgramado} h)
                    - Costo Real: S/ ${mantenimiento.costoReal}
                    - Repuestos e Insumos: $repuestosStr

                    Estructura en español:
                    1. Síntoma/Problema reportado
                    2. Causa probable
                    3. Acciones realizadas
                    4. Resultado final

                    Tono: Técnico y formal. No inventes datos.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                _reporteGenerado.postValue(response.text)
            } catch (e: Exception) {
                _mensajeError.postValue("Error en IA: ${e.message}")
            } finally {
                _loadingAI.postValue(false)
            }
        }
    }

    fun guardarReporte(uid: String, texto: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.guardarReporteIA(uid, texto)
        }
    }

    fun limpiarEstadoAI() {
        _reporteGenerado.postValue(null)
    }

    var estaCargandoMas = false
    fun cargarSiguienteLote() {
        if (estaCargandoMas) return
        estaCargandoMas = true
        viewModelScope.launch(Dispatchers.IO) {
            repository.descargarCambiosDeFirestore()
            estaCargandoMas = false
        }
    }
}
