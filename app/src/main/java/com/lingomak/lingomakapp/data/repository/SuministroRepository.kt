package com.lingomak.lingomakapp.data.repository

import android.content.Context
import com.lingomak.lingomakapp.data.local.AppDatabase
import java.text.SimpleDateFormat
import java.util.*

class SuministroRepository(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val suministroDao = database.suministroDao()
    private val registroUsoDao = database.registroUsoMaquinariaDao()

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
}
