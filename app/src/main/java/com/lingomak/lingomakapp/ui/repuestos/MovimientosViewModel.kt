package com.lingomak.lingomakapp.ui.repuestos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.data.repository.MovimientoRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class MovimientosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MovimientoRepository(application)
    private val repuestoDao = AppDatabase.getInstance(application).repuestoDao()

    private val _repuestoUid = MutableLiveData<String>()
    
    private val _ordenDescendente = MutableLiveData<Boolean>(true)
    val ordenDescendente: LiveData<Boolean> get() = _ordenDescendente

    private val _rangoActivo = MutableLiveData<String>("MES") // DIA, SEMANA, MES, ANIO, PERSONALIZADO
    val rangoActivo: LiveData<String> get() = _rangoActivo

    private val _fechaInicio = MutableLiveData<Long?>(null)
    private val _fechaFin = MutableLiveData<Long?>(null)

    // Fuente original desde Room
    private val historialBase: LiveData<List<MovimientoModel>> = _repuestoUid.switchMap { uid ->
        repository.obtenerHistorial(uid)
    }

    private var estaCargandoMas = false

    // Lista filtrada y ordenada que observa el Fragment
    val historialFiltrado = MediatorLiveData<List<MovimientoModel>>().apply {
        fun update() {
            val lista = historialBase.value ?: emptyList()
            val rango = _rangoActivo.value ?: "MES"
            val desc = _ordenDescendente.value ?: true

            val filtrada = when (rango) {
                "PERSONALIZADO" -> {
                    val inicio = _fechaInicio.value ?: 0L
                    val fin = _fechaFin.value ?: Long.MAX_VALUE
                    lista.filter { it.fecha != null && it.fecha.time in inicio..fin }
                }
                "TODO" -> lista
                else -> {
                    val diasFiltro = when (rango) {
                        "DIA" -> 1
                        "SEMANA" -> 7
                        "MES" -> 30
                        "ANIO" -> 365
                        else -> 30
                    }
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -diasFiltro) }
                    lista.filter { it.fecha != null && it.fecha.after(cal.time) }
                }
            }

            value = if (desc) filtrada.sortedByDescending { it.fecha }
                    else filtrada.sortedBy { it.fecha }
        }
        addSource(historialBase) { update() }
        addSource(_rangoActivo) { update() }
        addSource(_ordenDescendente) { update() }
        addSource(_fechaInicio) { update() }
        addSource(_fechaFin) { update() }
    }

    val repuesto = _repuestoUid.switchMap { uid ->
        repuestoDao.obtenerPorUidObservable(uid)
    }

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _registroExitoso = MutableLiveData<Boolean>()
    val registroExitoso: LiveData<Boolean> get() = _registroExitoso

    fun setRepuestoUid(uid: String) {
        _repuestoUid.value = uid
        descargarHistorial(uid)
    }

    private fun descargarHistorial(uid: String) {
        viewModelScope.launch {
            repository.descargarMovimientosPaginadosPorRepuesto(uid, batchSize = 50)
        }
    }

    fun cargarSiguienteLote() {
        val uid = _repuestoUid.value ?: return
        if (estaCargandoMas) return

        viewModelScope.launch {
            estaCargandoMas = true
            val listaActual = historialFiltrado.value ?: emptyList()
            if (listaActual.isNotEmpty()) {
                val ultimoTimestamp = listaActual.last().fecha?.time
                repository.descargarMovimientosPaginadosPorRepuesto(uid, ultimoTimestamp, 50)
            }
            estaCargandoMas = false
        }
    }

    fun setRangoFechas(inicio: Long, fin: Long) {
        _fechaInicio.value = inicio
        _fechaFin.value = fin
        _rangoActivo.value = "PERSONALIZADO"
    }

    fun toggleOrden() {
        _ordenDescendente.value = !(_ordenDescendente.value ?: true)
    }

    fun registrarMovimiento(movimiento: MovimientoModel) {
        val repuestoActual = repuesto.value ?: run {
            _error.value = "Error: Datos del repuesto no cargados"
            return
        }

        if (movimiento.tipo == "SALIDA" && movimiento.cantidad > repuestoActual.stockActual) {
            _error.value = "Error: Stock insuficiente. Solo hay ${repuestoActual.stockActual} disponibles."
            return
        }

        if (movimiento.tipo == "ENTRADA" && (repuestoActual.stockActual + movimiento.cantidad) > repuestoActual.stockMaximo) {
            _error.value = "Error: Se excede el stock máximo de ${repuestoActual.stockMaximo}."
            return
        }

        viewModelScope.launch {
            try {
                repository.registrarMovimiento(movimiento)
                _registroExitoso.value = true
            } catch (e: Exception) {
                _error.value = "Error al registrar: ${e.message}"
            }
        }
    }

    // --- Lógica de Resumen ---

    val resumen: LiveData<ResumenMovimientos> = historialFiltrado.map { lista ->
        val rango = _rangoActivo.value ?: "MES"
        val totalEntradas = lista.filter { it.tipo == "ENTRADA" }.sumOf { it.cantidad }
        val totalSalidas = lista.filter { it.tipo == "SALIDA" }.sumOf { it.cantidad }
        val balance = totalEntradas - totalSalidas

        val labelPeriodo = when(rango) {
            "DIA" -> " hoy"
            "SEMANA" -> " semana"
            "MES" -> " mes"
            "ANIO" -> " año"
            "PERSONALIZADO" -> " rango"
            else -> ""
        }

        ResumenMovimientos(
            totalEntradas = totalEntradas,
            totalSalidas = totalSalidas,
            balance = balance,
            periodoLabel = labelPeriodo
        )
    }

    data class ResumenMovimientos(
        val totalEntradas: Int,
        val totalSalidas: Int,
        val balance: Int,
        val periodoLabel: String
    )
}
