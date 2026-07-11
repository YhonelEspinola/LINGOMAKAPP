package com.lingomak.lingomakapp.ui.movimientos

import android.app.Application
import androidx.lifecycle.*
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.repository.MovimientoRepository
import com.lingomak.lingomakapp.data.repository.RepuestoRepository
import com.lingomak.lingomakapp.data.repository.MaquinariaRepositoryOp
import kotlinx.coroutines.launch
import java.util.Locale

class MovimientosOpViewModel(application: Application) : AndroidViewModel(application) {

    private val movRepo = MovimientoRepository(application)
    private val repRepo = RepuestoRepository(application)
    private val maqRepo = MaquinariaRepositoryOp(application)

    private val _movimientosOriginales = movRepo.obtenerTodos()
    private val _repuestosOriginales = repRepo.obtenerRepuestosObservable()
    
    private val _textoBusqueda = MutableLiveData("")
    private val _rangoFechas = MutableLiveData<Pair<Long, Long>?>(null)

    val movimientosFiltrados = MediatorLiveData<List<Pair<MovimientoModel, String>>>().apply {
        fun filtrar() {
            val listaMov = _movimientosOriginales.value ?: return
            val listaRep = _repuestosOriginales.value ?: emptyList()
            val query = _textoBusqueda.value?.lowercase(Locale.getDefault()) ?: ""
            val rango = _rangoFechas.value

            val repuestosMap = listaRep.associateBy { it.uid }

            value = listaMov.filter { mov ->
                val repuesto = repuestosMap[mov.repuestoUid]
                val coincideTexto = query.isEmpty() || 
                                   repuesto?.nombre?.lowercase(Locale.getDefault())?.contains(query) == true ||
                                   repuesto?.codigoInterno?.lowercase(Locale.getDefault())?.contains(query) == true ||
                                   mov.observacion.lowercase(Locale.getDefault()).contains(query)
                
                val coincideFecha = if (rango != null) {
                    val fechaMov = mov.fecha?.time ?: 0L
                    fechaMov in rango.first..rango.second
                } else true

                coincideTexto && coincideFecha
            }.map { mov ->
                val nombre = repuestosMap[mov.repuestoUid]?.nombre ?: "Desconocido"
                Pair(mov, nombre)
            }
        }
        addSource(_movimientosOriginales) { filtrar() }
        addSource(_repuestosOriginales) { filtrar() }
        addSource(_textoBusqueda) { filtrar() }
        addSource(_rangoFechas) { filtrar() }
    }

    val todosLosRepuestos = _repuestosOriginales
    val todasLasMaquinas = maqRepo.obtenerMaquinariasObservable()

    private val _registroExitoso = MutableLiveData<Boolean>()
    val registroExitoso: LiveData<Boolean> get() = _registroExitoso

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private var ultimoTimestampCargado: Long? = null
    private var estaCargandoMas = false

    fun registrarSalida(movimiento: MovimientoModel) {
        viewModelScope.launch {
            try {
                val repuesto = repRepo.obtenerRepuestoPorUid(movimiento.repuestoUid)
                if (repuesto != null && movimiento.cantidad > repuesto.stockActual) {
                    _error.value = "Stock insuficiente (Disponible: ${repuesto.stockActual})"
                    return@launch
                }

                movRepo.registrarMovimiento(movimiento)
                _registroExitoso.value = true
            } catch (e: Exception) {
                _error.value = "Error al registrar salida"
            }
        }
    }

    fun setRangoFechas(inicio: Long, fin: Long) {
        _rangoFechas.value = Pair(inicio, fin)
    }

    fun filtrarPorTexto(query: String) {
        _textoBusqueda.value = query
    }

    fun sincronizarDatos() {
        viewModelScope.launch {
            _loading.value = true
            try {
                movRepo.descargarMovimientosPaginados(batchSize = 50)
                repRepo.descargarCambiosDeFirestore()
                maqRepo.descargarMaquinariasDeFirestore()
            } catch (e: Exception) {
                _error.value = "Error al actualizar datos"
            } finally {
                _loading.value = false
            }
        }
    }

    fun cargarSiguienteLote() {
        if (estaCargandoMas) return
        
        viewModelScope.launch {
            estaCargandoMas = true
            val listaActual = movimientosFiltrados.value ?: emptyList()
            if (listaActual.isNotEmpty()) {
                val ultimoMov = listaActual.last().first
                ultimoTimestampCargado = ultimoMov.fecha?.time
                
                movRepo.descargarMovimientosPaginados(ultimoTimestampCargado, 50)
            }
            estaCargandoMas = false
        }
    }
}