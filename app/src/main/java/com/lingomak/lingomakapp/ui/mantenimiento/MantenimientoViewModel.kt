package com.lingomak.lingomakapp.ui.mantenimiento

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.concurrent.TimeUnit
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.model.ReporteIAData
import com.lingomak.lingomakapp.data.repository.ConsumoRepuesto
import com.lingomak.lingomakapp.data.repository.MantenimientoRepository
import com.lingomak.lingomakapp.data.repository.MaquinariaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MantenimientoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MantenimientoRepository(application)
    private val maquinariaRepository = MaquinariaRepository(application)
    private val movimientoDao = AppDatabase.getInstance(application).movimientoDao()
    private val functions = FirebaseFunctions.getInstance()

    private val _listaMantenimientosOriginales = MediatorLiveData<List<MantenimientoModel>>()
    val listaMantenimientos: LiveData<List<MantenimientoModel>> = _listaMantenimientosOriginales

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> = _mensajeError

    private val _reporteGenerado = MutableLiveData<ReporteIAData?>()
    val reporteGenerado: LiveData<ReporteIAData?> = _reporteGenerado

    private val _loadingAI = MutableLiveData<Boolean>()
    val loadingAI: LiveData<Boolean> = _loadingAI

    private val auth = FirebaseAuth.getInstance()
    private var sourceActual: LiveData<List<MantenimientoModel>>? = null

    fun listarMantenimientos(soloAsignados: Boolean = false) {
        sourceActual?.let { _listaMantenimientosOriginales.removeSource(it) }

        val liveData = if (soloAsignados) {
            val userUid = auth.currentUser?.uid ?: ""
            repository.obtenerAsignadosObservable(userUid)
        } else {
            repository.obtenerTodosObservable()
        }

        sourceActual = liveData
        _listaMantenimientosOriginales.addSource(liveData) { lista ->
            _listaMantenimientosOriginales.value = lista
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.descargarCambiosDeFirestore()
                repository.actualizarMantenimientosVencidos({}, {})
            } catch (e: Exception) {
                _mensajeError.postValue("No se pudo sincronizar: ${e.message}")
            }
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
        horometroReal: Double,
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

    fun reprogramarMantenimiento(uid: String, nuevaFecha: String, onExito: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.reprogramarMantenimiento(uid, nuevaFecha, {
                viewModelScope.launch(Dispatchers.Main) { onExito() }
            }, { error ->
                _mensajeError.postValue(error)
            })
        }
    }

    fun cancelarMantenimiento(uid: String, onExito: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cancelarMantenimiento(uid, {
                viewModelScope.launch(Dispatchers.Main) { onExito() }
            }, { error ->
                _mensajeError.postValue(error)
            })
        }
    }

    fun solicitarReprogramacion(uidMantenimiento: String, onExito: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.solicitarReprogramacion(uidMantenimiento, {
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

    fun obtenerMantenimientoPorUidObservable(uid: String): LiveData<MantenimientoModel?> {
        return repository.obtenerMantenimientoPorUidObservable(uid)
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
    // LÓGICA DE REPORTES CON CLOUD FUNCTIONS (GROQ)
    // ===================================================================

    fun generarReporteIA(mantenimiento: MantenimientoModel) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingAI.postValue(true)
            _reporteGenerado.postValue(null) // Resetear estado anterior
            try {
                val movimientos = movimientoDao.obtenerPorMantenimiento(mantenimiento.uid)
                val insumosStr = if (movimientos.isEmpty()) "Ninguno registrado"
                else movimientos.joinToString(", ") { "${it.cantidad} unidades (ID: ${it.repuestoUid})" }

                val params = hashMapOf(
                    "tipoMantenimiento" to mantenimiento.tipoMantenimiento,
                    "descripcion" to mantenimiento.descripcion,
                    "observaciones" to mantenimiento.observaciones,
                    "fechaRealizada" to mantenimiento.fechaRealizada,
                    "horometroReal" to mantenimiento.horometroReal,
                    "costoReal" to mantenimiento.costoReal,
                    "nombreMaquinaria" to mantenimiento.nombreMaquinaria,
                    "insumosStr" to insumosStr
                )

                // Llamar a la Cloud Function con timeout de 20 segundos
                val result = functions
                    .getHttpsCallable("generarReporteMantenimiento")
                    .withTimeout(20, TimeUnit.SECONDS)
                    .call(params)
                    .await()

                val data = result.data as? Map<*, *>
                val reporteMap = data?.get("reporte") as? Map<*, *>
                val reporteData = ReporteIAData.desdeMapa(reporteMap)

                if (reporteData != null) {
                    _reporteGenerado.postValue(reporteData)
                } else {
                    _mensajeError.postValue("La IA respondió pero el formato no es válido.")
                }
            } catch (e: FirebaseFunctionsException) {
                val msg = when (e.code) {
                    FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> "Tiempo de espera agotado. Groq no respondió."
                    FirebaseFunctionsException.Code.UNAVAILABLE -> "El servicio de IA no está disponible en este momento."
                    else -> e.message ?: "Error en el servidor de IA"
                }
                _mensajeError.postValue(msg)
            } catch (e: Exception) {
                _mensajeError.postValue("Error: ${e.localizedMessage ?: "Fallo de conexión"}")
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
