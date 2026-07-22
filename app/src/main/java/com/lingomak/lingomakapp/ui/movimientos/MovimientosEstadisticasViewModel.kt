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

class MovimientosEstadisticasViewModel(application: Application) : AndroidViewModel(application) {

    private val movimientoRepo = MovimientoRepository(application)
    private val maquinariaRepo = MaquinariaRepository(application)
    private val mantenimientoRepo = MantenimientoRepository(application)
    private val repuestoDao = AppDatabase.getInstance(application).repuestoDao()

    private val _rango = MutableLiveData<Pair<Long, Long>>()
    val rango: LiveData<Pair<Long, Long>> get() = _rango

    private val _etiquetaRango = MutableLiveData<String>("Últimos 30 días")
    val etiquetaRango: LiveData<String> get() = _etiquetaRango

    private val _mesesTendencia = MutableLiveData<Int>(6)
    val mesesTendencia: LiveData<Int> get() = _mesesTendencia

    private val todosLosMovimientos = movimientoRepo.obtenerTodos()
    private val todosLosRepuestos = repuestoDao.obtenerTodosObservable()
    private val todasLasMaquinas = maquinariaRepo.obtenerMaquinariasObservable()
    private val todosLosMantenimientos = mantenimientoRepo.obtenerTodosObservable()

    val estadisticas = MediatorLiveData<EstadisticasData>().apply {
        fun update() {
            val movimientos = todosLosMovimientos.value ?: return
            val repuestos = todosLosRepuestos.value ?: return
            val maquinas = todasLasMaquinas.value ?: emptyList()
            val mantenimientos = todosLosMantenimientos.value ?: emptyList()
            
            val r = _rango.value
            val nMeses = _mesesTendencia.value ?: 6
            val repuestosMap = repuestos.associateBy { it.uid }
            val maquinasMap = maquinas.associateBy { it.uid }

            // Periodo actual para la mayoría de estadísticas
            val enPeriodo = if (r != null) {
                movimientos.filter { it.fecha != null && it.fecha.time >= r.first && it.fecha.time <= r.second }
            } else {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
                movimientos.filter { it.fecha != null && it.fecha.after(cal.time) }
            }

            // PAGINA 1: Resumen
            val entradas = enPeriodo.filter { it.tipo == "ENTRADA" }.sumOf { it.cantidad }
            val salidas = enPeriodo.filter { it.tipo == "SALIDA" }.sumOf { it.cantidad }
            val labelResumen = if (r != null) {
                val diff = (r.second - r.first).coerceAtLeast(1)
                val days = (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                String.format(Locale.getDefault(), "Promedio diario: %.1f/día", salidas.toDouble() / days.toDouble())
            } else {
                String.format(Locale.getDefault(), "Promedio diario: %.1f/día", salidas.toDouble() / 30.0)
            }
            val productoMasUsadoId = enPeriodo.filter { it.tipo == "SALIDA" }
                .groupBy { it.repuestoUid }
                .maxByOrNull { entry -> entry.value.sumOf { it.cantidad } }?.key
            val productoMasUsadoNombre = repuestosMap[productoMasUsadoId]?.nombre ?: "---"

            // PAGINA 2: Tendencia (N meses dinámicos)
            val tendencia = mutableListOf<Pair<String, Pair<Int, Int>>>()
            val sdfMes = SimpleDateFormat("MMM", Locale.getDefault())
            for (i in (nMeses - 1) downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -i)
                val mes = cal.get(Calendar.MONTH)
                val anio = cal.get(Calendar.YEAR)
                
                val movsMes = movimientos.filter { m ->
                    val mCal = Calendar.getInstance().apply { time = m.fecha ?: Date(0) }
                    mCal.get(Calendar.MONTH) == mes && mCal.get(Calendar.YEAR) == anio
                }
                val ent = movsMes.filter { it.tipo == "ENTRADA" }.sumOf { it.cantidad }
                val sal = movsMes.filter { it.tipo == "SALIDA" }.sumOf { it.cantidad }
                tendencia.add(sdfMes.format(cal.time) to (ent to sal))
            }

            // PAGINA 3: Top Repuestos (del periodo)
            val topConsumidos = enPeriodo.filter { it.tipo == "SALIDA" }
                .groupBy { it.repuestoUid }
                .map { (uid, list) -> (repuestosMap[uid]?.nombre ?: "Desconocido") to list.sumOf { it.cantidad } }
                .sortedByDescending { it.second }

            // PAGINA 4: Baja/Nula rotación (del periodo)
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
            val mantEnPeriodo = mantenimientos.filter { m ->
                if (m.estado != "FINALIZADO") return@filter false
                val mDate = try { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(m.fechaRealizada) } catch(e: Exception) { null }
                if (mDate == null) return@filter false
                if (r != null) {
                    mDate.time >= r.first && mDate.time <= r.second
                } else {
                    val cal30 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
                    mDate.after(cal30.time)
                }
            }
            val costoEst = mantEnPeriodo.sumOf { it.costoEstimado }
            val costoReal = mantEnPeriodo.sumOf { it.costoReal }
            val diffCosto = costoReal - costoEst
            val diffPerc = if (costoEst > 0) (diffCosto / costoEst) * 100 else 0.0
            val labelCostos = String.format(Locale.getDefault(), "Diferencia: S/ %.2f (%.1f%%)", diffCosto, diffPerc)

            // RECOMENDACIÓN STOCK (30 días fija)
            val cal30 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
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
                recomendacion = productoEnRiesgo
            )
        }
        addSource(todosLosMovimientos) { update() }
        addSource(todosLosRepuestos) { update() }
        addSource(todasLasMaquinas) { update() }
        addSource(todosLosMantenimientos) { update() }
        addSource(_rango) { update() }
        addSource(_mesesTendencia) { update() }
    }

    fun setRango(inicio: Long, fin: Long, etiqueta: String) {
        _rango.value = inicio to fin
        _etiquetaRango.value = etiqueta
    }

    fun setMesesTendencia(meses: Int) {
        _mesesTendencia.value = meses
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
        val recomendacion: String?
    )
}
