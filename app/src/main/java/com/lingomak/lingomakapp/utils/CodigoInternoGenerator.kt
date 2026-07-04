package com.lingomak.lingomakapp.utils

import com.lingomak.lingomakapp.data.local.dao.ContadorDao
import com.lingomak.lingomakapp.data.local.dao.RepuestoDao
import com.lingomak.lingomakapp.data.local.entity.ContadorEntity

/**
 * Generador de códigos internos para repuestos.
 *
 * Formato: REP-{3 letras de categoría}-{correlativo de 4 dígitos}
 * Ejemplos: REP-ACE-0001, REP-FIL-0003, REP-ELE-0001
 *
 * El correlativo es independiente por categoría y se almacena
 * localmente en Room (tabla contadores), funcionando offline sin
 * necesitar Firestore.
 */
object CodigoInternoGenerator {

    /**
     * Genera el prefijo de 3 letras basado en la categoría.
     */
    fun obtenerPrefijo(categoria: String): String {
        return categoria
            .trim()
            .uppercase()
            .filter { it.isLetter() }
            .take(3)
            .ifEmpty { "GEN" }
    }

    /**
     * Previsualiza el siguiente código interno sin incrementar el contador.
     */
    suspend fun previsualizar(categoria: String, contadorDao: ContadorDao, repuestoDao: RepuestoDao): String {
        val prefijo = obtenerPrefijo(categoria)
        
        // Sincronización de seguridad: si no hay contador o es menor al máximo real en la tabla
        val maxReal = repuestoDao.obtenerMaxCorrelativo(prefijo) ?: 0
        contadorDao.actualizarSiEsMayor(prefijo, maxReal)
        
        val numero = contadorDao.obtenerSiguienteNumero(prefijo)
        return "REP-$prefijo-${numero.toString().padStart(4, '0')}"
    }

    /**
     * Genera el siguiente código interno para la categoría dada e incrementa el contador.
     */
    suspend fun generar(categoria: String, contadorDao: ContadorDao, repuestoDao: RepuestoDao): String {
        val prefijo = obtenerPrefijo(categoria)
        
        // Sincronización de seguridad
        val maxReal = repuestoDao.obtenerMaxCorrelativo(prefijo) ?: 0
        contadorDao.actualizarSiEsMayor(prefijo, maxReal)

        val numero = contadorDao.reservarSiguienteNumero(prefijo)
        return "REP-$prefijo-${numero.toString().padStart(4, '0')}"
    }

    /**
     * Extrae el prefijo de un código existente (ej. "REP-ACE-0001" -> "ACE").
     */
    fun extraerPrefijo(codigo: String): String? {
        val partes = codigo.split("-")
        return if (partes.size >= 2) partes[1] else null
    }

    /**
     * Extrae el número correlativo de un código existente (ej. "REP-ACE-0001" -> 1).
     */
    fun extraerNumero(codigo: String): Int {
        val partes = codigo.split("-")
        return if (partes.size >= 3) {
            partes[2].toIntOrNull() ?: 0
        } else 0
    }
}
