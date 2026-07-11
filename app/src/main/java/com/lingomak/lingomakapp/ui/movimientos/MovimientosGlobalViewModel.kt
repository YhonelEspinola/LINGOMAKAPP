package com.lingomak.lingomakapp.ui.movimientos

import android.app.Application
import androidx.lifecycle.*
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.data.repository.MovimientoRepository
import kotlinx.coroutines.launch
import java.util.*

class MovimientosGlobalViewModel(application: Application) : AndroidViewModel(application) {

    private val movimientoRepository = MovimientoRepository(application)
    private val repuestoDao = AppDatabase.getInstance(application).repuestoDao()

    private val _filtroTipo = MutableLiveData<String?>(null)
    private val _filtroTexto = MutableLiveData<String>("")
    private val _rango = MutableLiveData<String>("MES") // DIA, SEMANA, MES, ANIO, PERSONALIZADO
    val rango: LiveData<String> get() = _rango

    private val _fechaInicio = MutableLiveData<Long?>(null)
    private val _fechaFin = MutableLiveData<Long?>(null)

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _registroExitoso = MutableLiveData<Boolean>()
    val registroExitoso: LiveData<Boolean> get() = _registroExitoso

    val todosLosRepuestos = repuestoDao.obtenerTodosObservable().map { entities ->
        entities.map { entity ->
            RepuestoModel(
                uid = entity.uid,
                nombre = entity.nombre,
                codigoInterno = entity.codigoInterno,
                categoria = entity.categoria,
                marca = entity.marca,
                descripcion = entity.descripcion,
                stockActual = entity.stockActual,
                stockMinimo = entity.stockMinimo,
                stockMaximo = entity.stockMaximo,
                ubicacionAlmacen = entity.ubicacionAlmacen,
                imagenUrl = entity.imagenUrl,
                codigoQR = entity.codigoQR,
                estado = entity.estado
            )
        }
    }

    private val todosLosMovimientos = movimientoRepository.obtenerTodos()

    val movimientosFiltrados = MediatorLiveData<List<Pair<MovimientoModel, String>>>().apply {
        fun update() {
            val movimientos = todosLosMovimientos.value ?: emptyList()
            val repuestos = todosLosRepuestos.value ?: emptyList()
            val tipo = _filtroTipo.value
            val texto = _filtroTexto.value?.lowercase(Locale.getDefault()) ?: ""
            val rangoActivo = _rango.value ?: "MES"

            val repuestosMap = repuestos.associateBy { it.uid }

            val filtrados = movimientos.filter { mov ->
                val repuesto = repuestosMap[mov.repuestoUid]
                
                val matchTipo = tipo == null || mov.tipo == tipo
                val textoMinuscula = texto.lowercase(Locale.getDefault())
                val matchTexto = texto.isEmpty() || 
                        repuesto?.nombre?.lowercase(Locale.getDefault())?.contains(textoMinuscula) == true ||
                        repuesto?.codigoInterno?.lowercase(Locale.getDefault())?.contains(textoMinuscula) == true
                
                val matchRango = when(rangoActivo) {
                    "PERSONALIZADO" -> {
                        val inicio = _fechaInicio.value ?: 0L
                        val fin = _fechaFin.value ?: Long.MAX_VALUE
                        mov.fecha != null && mov.fecha.time in inicio..fin
                    }
                    "TODO" -> true
                    else -> {
                        val diasFiltro = when (rangoActivo) {
                            "DIA" -> 1
                            "SEMANA" -> 7
                            "MES" -> 30
                            "ANIO" -> 365
                            else -> 30
                        }
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -diasFiltro) }
                        mov.fecha != null && mov.fecha.after(cal.time)
                    }
                }

                matchTipo && matchTexto && matchRango
            }.map { mov ->
                val nombre = repuestosMap[mov.repuestoUid]?.nombre ?: "Desconocido"
                Pair(mov, nombre)
            }
            value = filtrados
        }

        addSource(todosLosMovimientos) { update() }
        addSource(todosLosRepuestos) { update() }
        addSource(_filtroTipo) { update() }
        addSource(_filtroTexto) { update() }
        addSource(_rango) { update() }
        addSource(_fechaInicio) { update() }
        addSource(_fechaFin) { update() }
    }

    val resumenGlobal: LiveData<ResumenGlobal> = MutableLiveData()

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private var ultimoTimestampCargado: Long? = null
    private var estaCargandoMas = false

    init {
        descargarDatos()
    }

    private fun descargarDatos() {
        viewModelScope.launch {
            _loading.value = true
            movimientoRepository.descargarMovimientosPaginados(batchSize = 50)
            _loading.value = false
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
                
                movimientoRepository.descargarMovimientosPaginados(ultimoTimestampCargado, 50)
            }
            estaCargandoMas = false
        }
    }

    fun filtrarPorTipo(tipo: String?) {
        _filtroTipo.value = tipo
    }

    fun filtrarPorTexto(texto: String) {
        _filtroTexto.value = texto
    }

    fun setRangoFechas(inicio: Long, fin: Long) {
        _fechaInicio.value = inicio
        _fechaFin.value = fin
        _rango.value = "PERSONALIZADO"
    }

    fun registrarMovimiento(movimiento: MovimientoModel) {
        viewModelScope.launch {
            try {
                val repuestoActual = repuestoDao.obtenerPorUid(movimiento.repuestoUid)
                if (repuestoActual == null) {
                    _error.value = "Error: Repuesto no encontrado"
                    return@launch
                }

                if (movimiento.tipo == "SALIDA") {
                    if (movimiento.cantidad > repuestoActual.stockActual) {
                        _error.postValue("Error: Stock insuficiente. Solo hay ${repuestoActual.stockActual} disponibles.")
                        return@launch
                    }
                    if (movimiento.destinoSalida.isEmpty()) {
                        _error.postValue("Error: Seleccione el destino de la salida")
                        return@launch
                    }
                }

                if (movimiento.tipo == "ENTRADA" && (repuestoActual.stockActual + movimiento.cantidad) > repuestoActual.stockMaximo) {
                    _error.postValue("Error: Se excede el stock máximo de ${repuestoActual.stockMaximo}.")
                    return@launch
                }

                movimientoRepository.registrarMovimiento(movimiento)
                _registroExitoso.postValue(true)
            } catch (e: Exception) {
                _error.postValue("Error al registrar: ${e.message}")
            }
        }
    }

    fun eliminarMovimiento(uid: String) {
        viewModelScope.launch {
            try {
                movimientoRepository.eliminarMovimiento(uid)
            } catch (e: Exception) {
                _error.postValue("Error al eliminar: ${e.message}")
            }
        }
    }

    fun actualizarMovimiento(movimiento: MovimientoModel, cantidadAnterior: Int, tipoAnterior: String) {
        viewModelScope.launch {
            try {
                val repuestoActual = repuestoDao.obtenerPorUid(movimiento.repuestoUid)
                    ?: throw Exception("Repuesto no encontrado")

                val reversion = if (tipoAnterior == "ENTRADA") -cantidadAnterior else cantidadAnterior
                val aplicacion = if (movimiento.tipo == "ENTRADA") movimiento.cantidad else -movimiento.cantidad
                val deltaFinal = reversion + aplicacion
                val nuevoStockProyectado = repuestoActual.stockActual + deltaFinal

                if (nuevoStockProyectado < 0) {
                    _error.postValue("Error: Stock insuficiente.")
                    return@launch
                }
                if (nuevoStockProyectado > repuestoActual.stockMaximo) {
                    _error.postValue("Error: Se excede el stock máximo.")
                    return@launch
                }
                if (movimiento.tipo == "SALIDA" && movimiento.destinoSalida.isEmpty()) {
                    _error.postValue("Error: Seleccione el destino de la salida")
                    return@launch
                }

                movimientoRepository.actualizarMovimiento(movimiento, deltaFinal)
                _registroExitoso.postValue(true)
            } catch (e: Exception) {
                _error.postValue("Error al actualizar: ${e.message}")
            }
        }
    }

    data class ResumenGlobal(
        val entradasMétrica: Double = 0.0,
        val salidasMétrica: Double = 0.0,
        val totalMovimientos: Int = 0,
        val periodoLabel: String = ""
    )
}
