package com.lingomak.lingomakapp.ui.movimientos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.data.repository.MovimientoRepository
import java.util.*

class MovimientosEstadisticasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MovimientoRepository(application)
    private val repuestoDao = AppDatabase.getInstance(application).repuestoDao()

    private val _rango = MutableLiveData<Pair<Long, Long>>()
    val rango: LiveData<Pair<Long, Long>> get() = _rango

    private val _etiquetaRango = MutableLiveData<String>("Últimos 30 días")
    val etiquetaRango: LiveData<String> get() = _etiquetaRango

    private val todosLosMovimientos = repository.obtenerTodos()
    private val todosLosRepuestos = repuestoDao.obtenerTodosObservable()

    val estadisticas = MediatorLiveData<EstadisticasData>().apply {
        fun update() {
            val movimientos = todosLosMovimientos.value ?: emptyList()
            val repuestos = todosLosRepuestos.value ?: emptyList()
            val r = _rango.value
            if (movimientos.isEmpty()) return

            val repuestosMap = repuestos.associateBy { it.uid }

            val enPeriodo = if (r != null) {
                movimientos.filter { it.fecha != null && it.fecha.time >= r.first && it.fecha.time <= r.second }
            } else {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
                movimientos.filter { it.fecha != null && it.fecha.after(cal.time) }
            }

            val entradas = enPeriodo.filter { it.tipo == "ENTRADA" }.sumOf { it.cantidad }
            val salidas = enPeriodo.filter { it.tipo == "SALIDA" }.sumOf { it.cantidad }

            // Promedios reales para las etiquetas
            val labelResumen = if (r != null) {
                val diff = r.second - r.first
                val days = (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                String.format(Locale.getDefault(), "Promedio diario: %.1f/día", salidas.toDouble() / days.toDouble())
            } else {
                String.format(Locale.getDefault(), "Promedio diario: %.1f/día", salidas.toDouble() / 30.0)
            }

            // Producto más usado en el periodo
            val productoMasUsadoId = enPeriodo.groupBy { it.repuestoUid }
                .maxByOrNull { entry -> entry.value.sumOf { it.cantidad } }?.key
            val productoMasUsadoNombre = repuestosMap[productoMasUsadoId]?.nombre ?: "---"

            // Top 5 productos (histórico total)
            val top5 = movimientos.groupBy { it.repuestoUid }
                .map { entry -> 
                    val nombre = repuestosMap[entry.key]?.nombre ?: "Desconocido"
                    val count = entry.value.size
                    nombre to count
                }
                .sortedByDescending { it.second }
                .take(5)

            // Recomendación de stock (basada en 30 días fija)
            val cal30 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
            val movimientos30 = movimientos.filter { it.fecha != null && it.fecha.after(cal30.time) }
            
            val productoEnRiesgo = repuestos
                .filter { it.estado == "ACTIVO" }
                .map { r ->
                    val salidasR = movimientos30.filter { it.repuestoUid == r.uid && it.tipo == "SALIDA" }.sumOf { it.cantidad }
                    val velocidad = salidasR.toDouble() / 30.0
                    val dias = if (velocidad > 0) r.stockActual.toDouble() / velocidad else Double.MAX_VALUE
                    r.nombre to dias
                }
                .filter { it.second <= 14 }
                .minByOrNull { it.second }

            value = EstadisticasData(
                totalEntradas = entradas,
                totalSalidas = salidas,
                totalMovimientos = enPeriodo.size,
                resumenTexto = labelResumen,
                productoMasUsado = productoMasUsadoNombre,
                topProductos = top5,
                recomendacion = productoEnRiesgo?.first
            )
        }
        addSource(todosLosMovimientos) { update() }
        addSource(todosLosRepuestos) { update() }
        addSource(_rango) { update() }
    }

    fun setRango(inicio: Long, fin: Long, etiqueta: String) {
        _rango.value = inicio to fin
        _etiquetaRango.value = etiqueta
    }

    data class EstadisticasData(
        val totalEntradas: Int,
        val totalSalidas: Int,
        val totalMovimientos: Int,
        val resumenTexto: String,
        val productoMasUsado: String,
        val topProductos: List<Pair<String, Int>>,
        val recomendacion: String?
    )
}
