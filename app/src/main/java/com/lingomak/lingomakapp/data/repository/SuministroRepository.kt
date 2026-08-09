package com.lingomak.lingomakapp.data.repository

import android.content.Context
import com.lingomak.lingomakapp.data.local.AppDatabase
import java.text.SimpleDateFormat
import java.util.*

data class NivelCombustibleEstimado(
    val galones: Double,
    val porcentaje: Double,
    val esEstimado: Boolean = true
)

class SuministroRepository(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val suministroDao = database.suministroDao()
    private val registroUsoDao = database.registroUsoMaquinariaDao()
    private val maquinariaDao = database.maquinariaDao()

    /**
     * Calcula el consumo promedio (Gls/h) de una máquina en un rango de días.
     * Fórmula: (Σ combustibleGls) / (Σ horasUso)
     */
    suspend fun calcularConsumoGlsHora(uidMaquinaria: String, diasAtras: Int = 30): Double? {
        val fechaInicio = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Date(System.currentTimeMillis() - (diasAtras.toLong() * 24 * 60 * 60 * 1000))
        )

        val suministros = suministroDao.obtenerPorMaquinaYRango(uidMaquinaria, fechaInicio)
        val totalCombustible = suministros.sumOf { it.galonesCombustible }

        val registros = registroUsoDao.obtenerTodos().filter { 
            it.uidMaquinaria == uidMaquinaria && it.fechaUso >= fechaInicio 
        }
        val totalHoras = registros.sumOf { it.horasUso }

        if (totalHoras <= 0.0) return null
        
        return totalCombustible / totalHoras
    }

    suspend fun obtenerTopMenorConsumo(limite: Int = 5, diasAtras: Int = 30): List<Pair<com.lingomak.lingomakapp.data.model.MaquinariaModel, Double>> {
        val maquinas = maquinariaDao.obtenerTodas()
        return maquinas.mapNotNull { maquina ->
            calcularConsumoGlsHora(maquina.uid, diasAtras)?.let { consumo -> maquina.aModel() to consumo }
        }.sortedBy { it.second }.take(limite)
    }

    suspend fun calcularNivelEstimadoCombustible(uidMaquinaria: String): NivelCombustibleEstimado? {
        val maquina = maquinariaDao.obtenerPorUid(uidMaquinaria) ?: return null
        val capacidad = maquina.capacidadTanqueGls ?: return null
        
        // 1. Buscar el último llenado completo
        val ultimoLlenadoCompleto = suministroDao.obtenerTodos()
            .filter { it.uidMaquinaria == uidMaquinaria && it.tipoCarga == "Completa" }
            .maxByOrNull { it.fecha } ?: return null
        
        val fechaLlenado = ultimoLlenadoCompleto.fecha
        
        // 2. Consumo promedio 30d
        val consumoPromedio = calcularConsumoGlsHora(uidMaquinaria, 30) ?: 0.0
        
        // 3. Horas trabajadas desde el último llenado completo
        val horasDesdeLlenado = registroUsoDao.obtenerTodos()
            .filter { it.uidMaquinaria == uidMaquinaria && it.fechaUso >= fechaLlenado }
            .sumOf { it.horasUso }
            
        // 4. Galones de cargas parciales después del llenado completo
        val galonesParciales = suministroDao.obtenerTodos()
            .filter { it.uidMaquinaria == uidMaquinaria && it.tipoCarga == "Parcial" && it.fecha >= fechaLlenado }
            .sumOf { it.galonesCombustible }
            
        val nivelGls = (capacidad - (consumoPromedio * horasDesdeLlenado) + galonesParciales)
            .coerceIn(0.0, capacidad)
            
        val porcentaje = (nivelGls / capacidad) * 100
        
        return NivelCombustibleEstimado(nivelGls, porcentaje)
    }
}
