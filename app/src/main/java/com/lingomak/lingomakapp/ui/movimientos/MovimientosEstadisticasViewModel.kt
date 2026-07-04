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

    private val _periodo = MutableLiveData<String>("SEMANA") // DIA, SEMANA, MES, ANIO
    val periodo: LiveData<String> get() = _periodo

    private val todosLosMovimientos = repository.obtenerTodos()
    private val todosLosRepuestos = repuestoDao.obtenerTodosObservable()

    val estadisticas = MediatorLiveData<EstadisticasData>().apply {
        fun update() {
            val movimientos = todosLosMovimientos.value ?: emptyList()
            val repuestos = todosLosRepuestos.value ?: emptyList()
            val p = _periodo.value ?: "SEMANA"
            if (movimientos.isEmpty()) return

            val repuestosMap = repuestos.associateBy { it.uid }

            val diasFiltro = when (p) {
                "DIA" -> 1
                "SEMANA" -> 7
                "MES" -> 30
                "ANIO" -> 365
                else -> 7
            }
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -diasFiltro) }
            val enPeriodo = movimientos.filter { it.fecha != null && it.fecha.after(cal.time) }

            val entradas = enPeriodo.filter { it.tipo == "ENTRADA" }.sumOf { it.cantidad }
            val salidas = enPeriodo.filter { it.tipo == "SALIDA" }.sumOf { it.cantidad }

            // Promedios reales para las etiquetas
            val labelResumen = when(p) {
                "DIA" -> "Consumo total de hoy"
                "SEMANA" -> String.format(Locale.getDefault(), "Promedio diario: %.1f/día", salidas.toDouble() / 7.0)
                "MES" -> String.format(Locale.getDefault(), "Promedio semanal: %.1f/sem.", salidas.toDouble() / 4.0)
                "ANIO" -> String.format(Locale.getDefault(), "Promedio mensual: %.1f/mes", salidas.toDouble() / 12.0)
                else -> ""
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
        addSource(_periodo) { update() }
    }

    fun setPeriodo(periodo: String) {
        _periodo.value = periodo
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
