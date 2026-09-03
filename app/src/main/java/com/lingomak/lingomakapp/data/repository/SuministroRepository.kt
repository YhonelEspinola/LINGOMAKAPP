package com.lingomak.lingomakapp.data.repository

import android.content.Context
import com.lingomak.lingomakapp.data.local.AppDatabase
import java.text.SimpleDateFormat
import java.util.*

class SuministroRepository(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val suministroDao = database.suministroDao()
    private val registroUsoDao = database.registroUsoMaquinariaDao()
    private val maquinariaDao = database.maquinariaDao()

    /**
     * Calcula el consumo promedio (Gls/h) de una máquina en un rango de días.
     * Lee directamente de los registros de uso unificados.
     */
    suspend fun calcularConsumoGlsHora(uidMaquinaria: String, diasAtras: Int = 30): Double? {
        val fechaLimite = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Date(System.currentTimeMillis() - (diasAtras.toLong() * 24 * 60 * 60 * 1000))
        )

        val registros = registroUsoDao.obtenerTodos().filter { 
            it.uidMaquinaria == uidMaquinaria && it.fechaUso >= fechaLimite 
        }
        
        val totalCombustible = registros.sumOf { it.galonesCombustible }
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


}
