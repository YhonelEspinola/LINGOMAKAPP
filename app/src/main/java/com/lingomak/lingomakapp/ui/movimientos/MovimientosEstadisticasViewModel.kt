package com.lingomak.lingomakapp.ui.movimientos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.repository.MantenimientoRepository
import com.lingomak.lingomakapp.data.repository.MovimientoRepository
import com.lingomak.lingomakapp.data.repository.MaquinariaRepository
import java.text.SimpleDateFormat
import java.util.*

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MovimientosEstadisticasViewModel(application: Application) : AndroidViewModel(application) {

    private val movimientoRepo = MovimientoRepository(application)
    private val maquinariaRepo = MaquinariaRepository(application)
    private val mantenimientoRepo = MantenimientoRepository(application)
    private val suministroRepo = com.lingomak.lingomakapp.data.repository.SuministroRepository(application)
    private val registroUsoRepo = com.lingomak.lingomakapp.data.repository.RegistroUsoMaquinariaRepository(application)
    private val repuestoDao = AppDatabase.getInstance(application).repuestoDao()

    private val _rango = MutableLiveData<Pair<Long, Long>>()
    val rango: LiveData<Pair<Long, Long>> get() = _rango

    private val _etiquetaRango = MutableLiveData<String>("Últimos 30 días")
    val etiquetaRango: LiveData<String> get() = _etiquetaRango

    private val todosLosMovimientos = movimientoRepo.obtenerTodos()
    private val todosLosRepuestos = repuestoDao.obtenerTodosObservable()
    private val todasLasMaquinas = maquinariaRepo.obtenerMaquinariasObservable()
    private val todosLosMantenimientos = mantenimientoRepo.obtenerTodosObservable()
    
    private val _topMenorConsumo = MutableLiveData<List<Pair<com.lingomak.lingomakapp.data.model.MaquinariaModel, Double>>>()
    val topMenorConsumo: LiveData<List<Pair<com.lingomak.lingomakapp.data.model.MaquinariaModel, Double>>> = _topMenorConsumo

    private val _rendimientoOperarios = MutableLiveData<List<com.lingomak.lingomakapp.data.repository.RendimientoOperarioMes>>()
    val rendimientoOperarios: LiveData<List<com.lingomak.lingomakapp.data.repository.RendimientoOperarioMes>> = _rendimientoOperarios

    fun cargarTopMenorConsumo() {
        viewModelScope.launch {
            _topMenorConsumo.value = suministroRepo.obtenerTopMenorConsumo()
        }
    }

    fun cargarRendimientoOperarios() {
        viewModelScope.launch {
            _rendimientoOperarios.value = registroUsoRepo.obtenerHorasPorOperarioYMes()
        }
    }

    val estadisticas = MediatorLiveData<EstadisticasData>().apply {
        fun update() {
            val movimientos = todosLosMovimientos.value ?: return
            val repuestos = todosLosRepuestos.value ?: return
            val maquinas = todasLasMaquinas.value ?: emptyList()
            val mantenimientos = todosLosMantenimientos.value ?: emptyList()
            val topConsumo = _topMenorConsumo.value ?: emptyList()
            val rendimiento = _rendimientoOperarios.value ?: emptyList()
            
            val r = _rango.value
            val repuestosMap = repuestos.associateBy { it.uid }
            val maquinasMap = maquinas.associateBy { it.uid }

            // Periodo actual
            val cal30 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
            val startTs = r?.first ?: cal30.timeInMillis
            val endTs = r?.second ?: System.currentTimeMillis()

            val enPeriodo = movimientos.filter { it.fecha != null && it.fecha.time in startTs..endTs }

            // PAGINA 1: Resumen
            val entradas = enPeriodo.filter { it.tipo == "ENTRADA" }.sumOf { it.cantidad }
            val salidas = enPeriodo.filter { it.tipo == "SALIDA" }.sumOf { it.cantidad }
            
            val diff = (endTs - startTs).coerceAtLeast(1)
            val days = (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
            val labelResumen = String.format(Locale.getDefault(), "Promedio diario: %.1f/día", salidas.toDouble() / days.toDouble())
            
            val productoMasUsadoId = enPeriodo.filter { it.tipo == "SALIDA" }
                .groupBy { it.repuestoUid }
                .maxByOrNull { entry -> entry.value.sumOf { it.cantidad } }?.key
            val productoMasUsadoNombre = repuestosMap[productoMasUsadoId]?.nombre ?: "---"

            // PAGINA 2: Tendencia (Dinámica según el rango)
            val tendencia = mutableListOf<Pair<String, Pair<Int, Int>>>()
            if (days <= 31) {
                // Diaria
                val sdfDia = SimpleDateFormat("dd/MM", Locale.getDefault())
                for (i in 0 until days.toInt()) {
                    val cal = Calendar.getInstance().apply { timeInMillis = startTs; add(Calendar.DAY_OF_YEAR, i) }
                    val d = cal.get(Calendar.DAY_OF_MONTH)
                    val m = cal.get(Calendar.MONTH)
                    val a = cal.get(Calendar.YEAR)
                    
                    val movsDia = movimientos.filter { mov ->
                        val mCal = Calendar.getInstance().apply { time = mov.fecha ?: Date(0) }
                        mCal.get(Calendar.DAY_OF_MONTH) == d && mCal.get(Calendar.MONTH) == m && mCal.get(Calendar.YEAR) == a
                    }
                    val ent = movsDia.filter { it.tipo == "ENTRADA" }.sumOf { it.cantidad }
                    val sal = movsDia.filter { it.tipo == "SALIDA" }.sumOf { it.cantidad }
                    tendencia.add(sdfDia.format(cal.time) to (ent to sal))
                }
            } else {
                // Mensual (últimos N meses del rango)
                val months = (days / 30).coerceAtLeast(1).toInt().coerceAtMost(12)
                val sdfMes = SimpleDateFormat("MMM", Locale.getDefault())
                for (i in (months - 1) downTo 0) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MONTH, -i)
                    val mes = cal.get(Calendar.MONTH)
                    val anio = cal.get(Calendar.YEAR)
                    
                    val movsMes = movimientos.filter { mov ->
                        val mCal = Calendar.getInstance().apply { time = mov.fecha ?: Date(0) }
                        mCal.get(Calendar.MONTH) == mes && mCal.get(Calendar.YEAR) == anio
                    }
                    val ent = movsMes.filter { it.tipo == "ENTRADA" }.sumOf { it.cantidad }
                    val sal = movsMes.filter { it.tipo == "SALIDA" }.sumOf { it.cantidad }
                    tendencia.add(sdfMes.format(cal.time) to (ent to sal))
                }
            }

            // PAGINA 3: Top Repuestos
            val topConsumidos = enPeriodo.filter { it.tipo == "SALIDA" }
                .groupBy { it.repuestoUid }
                .map { (uid, list) -> (repuestosMap[uid]?.nombre ?: "Desconocido") to list.sumOf { it.cantidad } }
                .sortedByDescending { it.second }

            // PAGINA 4: Baja/Nula rotación
            val bajaRotacion = repuestos.filter { it.estado == "ACTIVO" }
                .map { rep ->
                    val cantSalidas = enPeriodo.filter { it.repuestoUid == rep.uid && it.tipo == "SALIDA" }.sumOf { it.cantidad }
                    rep.nombre to cantSalidas
                }
                .sortedBy { it.second }

            // PAGINA 5: Consumo por Máquina
            val consumoMaquina = enPeriodo.filter { it.tipo == "SALIDA" && it.maquinariaUid != null }
                .groupBy { it.maquinariaUid }
                .map { (uid, list) -> (maquinasMap[uid]?.nombre ?: "Maq. Desconocida") to list.sumOf { it.cantidad } }
                .sortedByDescending { it.second }

            // PAGINA 6: Interno vs Externo
            val porDestino = enPeriodo.filter { it.tipo == "SALIDA" }
                .groupBy { it.destinoSalida }
                .mapValues { it.value.sumOf { it.cantidad } }

            // PAGINA 7: OM vs Sueltas
            val salidasConOM = enPeriodo.filter { it.tipo == "SALIDA" && it.ordenMantenimientoUid != null }.sumOf { it.cantidad }
            val salidasSinOM = enPeriodo.filter { it.tipo == "SALIDA" && it.ordenMantenimientoUid == null }.sumOf { it.cantidad }

            // PAGINA 8: Costos Mantenimiento
            val mantEnPeriodo = mantenimientos.filter { movMant ->
                if (movMant.estado != "FINALIZADO") return@filter false
                val mDate = try { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(movMant.fechaRealizada) } catch(e: Exception) { null }
                if (mDate == null) return@filter false
                mDate.time in startTs..endTs
            }
            val costoEst = mantEnPeriodo.sumOf { it.costoEstimado }
            val costoReal = mantEnPeriodo.sumOf { it.costoReal }
            val diffCosto = costoReal - costoEst
            val diffPerc = if (costoEst > 0) (diffCosto / costoEst) * 100 else 0.0
            val labelCostos = String.format(Locale.getDefault(), "Diferencia: S/ %.2f (%.1f%%)", diffCosto, diffPerc)

            // RECOMENDACIÓN STOCK (fija 30 días para estabilidad)
            val movs30 = movimientos.filter { it.fecha != null && it.fecha.after(cal30.time) }
            val productoEnRiesgo = repuestos.filter { it.estado == "ACTIVO" }
                .map { rep ->
                    val sals = movs30.filter { it.repuestoUid == rep.uid && it.tipo == "SALIDA" }.sumOf { it.cantidad }
                    val vel = sals.toDouble() / 30.0
                    val dias = if (vel > 0) rep.stockActual.toDouble() / vel else Double.MAX_VALUE
                    rep.nombre to dias
                }
                .filter { it.second <= 14 }
                .minByOrNull { it.second }?.first

            value = EstadisticasData(
                totalEntradas = entradas,
                totalSalidas = salidas,
                totalMovimientos = enPeriodo.size,
                resumenTexto = labelResumen,
                productoMasUsado = productoMasUsadoNombre,
                tendenciaMensual = tendencia,
                topProductos = topConsumidos,
                bajaRotacion = bajaRotacion,
                consumoMaquina = consumoMaquina,
                distribucionSalida = porDestino,
                omVsSueltas = salidasConOM to salidasSinOM,
                costosComparativa = Triple(costoEst, costoReal, labelCostos),
                recomendacion = productoEnRiesgo,
                topMenorConsumo = topConsumo.map { "${it.first.nombre} (${it.first.codigoMaquinaria})" to it.second },
                rendimientoOperarios = rendimiento
            )
        }
        addSource(todosLosMovimientos) { update() }
        addSource(todosLosRepuestos) { update() }
        addSource(todasLasMaquinas) { update() }
        addSource(todosLosMantenimientos) { update() }
        addSource(_rango) { update() }
        addSource(_topMenorConsumo) { update() }
        addSource(_rendimientoOperarios) { update() }
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
        val tendenciaMensual: List<Pair<String, Pair<Int, Int>>>,
        val topProductos: List<Pair<String, Int>>,
        val bajaRotacion: List<Pair<String, Int>>,
        val consumoMaquina: List<Pair<String, Int>>,
        val distribucionSalida: Map<String, Int>,
        val omVsSueltas: Pair<Int, Int>,
        val costosComparativa: Triple<Double, Double, String>,
        val recomendacion: String?,
        val topMenorConsumo: List<Pair<String, Double>> = emptyList(),
        val rendimientoOperarios: List<com.lingomak.lingomakapp.data.repository.RendimientoOperarioMes> = emptyList()
    )
}
