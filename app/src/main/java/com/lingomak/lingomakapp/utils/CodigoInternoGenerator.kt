package com.lingomak.lingomakapp.utils

import com.lingomak.lingomakapp.data.local.dao.ContadorDao

/**
 * Generador de códigos internos para repuestos.
 *
 * Formato: REP-{3 letras de categoría}-{correlativo de 4 dígitos}
 * Ejemplos: REP-ACE-0001, REP-FIL-0003, REP-ELE-0001
 *
 * El correlativo es independiente por categoría y se almacena
 * localmente en Room (tabla contadores), funcionando offline sin
 * necesitar Firestore.
 *
 * Dado que solo el admin registra repuestos (un único dispositivo),
 * no hay riesgo de colisión entre dispositivos.
 */
object CodigoInternoGenerator {

    /**
     * Genera el siguiente código interno para la categoría dada.
     * Debe llamarse dentro de una coroutine (es suspend).
     *
     * @param categoria El nombre completo de la categoría
     *                  (ej. "Aceites", "Filtros", "Eléctrico").
     * @param contadorDao El DAO de contadores de Room.
     * @return El código generado (ej. "REP-ACE-0001").
     */
    suspend fun generar(categoria: String, contadorDao: ContadorDao): String {
        val prefijo = categoria
            .trim()
            .uppercase()
            .filter { it.isLetter() }
            .take(3)
            .ifEmpty { "GEN" } // Fallback si la categoría no tiene letras

        val numero = contadorDao.reservarSiguienteNumero(prefijo)

        return "REP-$prefijo-${numero.toString().padStart(4, '0')}"
    }
}
