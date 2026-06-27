package com.lingomak.lingomakapp.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.MovimientoEntity
import com.lingomak.lingomakapp.data.model.MovimientoModel
import java.util.Date

/**
 * Repositorio de Movimientos (Entradas/Salidas).
 * Sigue el patrón offline-first.
 */
class MovimientoRepository(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val movimientosCollection = db.collection("movimientos")
    
    private val movimientoDao = AppDatabase.getInstance(context).movimientoDao()

    /**
     * Sincroniza los movimientos pendientes de subir a Firestore.
     * Al ser registros históricos e inmutables, no hay conflictos
     * que resolver (no hay bajada de datos remotos masiva).
     */
    suspend fun sincronizarPendientesConFirestore() {
        val pendientes = movimientoDao.obtenerPendientesDeSincronizar()
        for (entity in pendientes) {
            try {
                movimientosCollection.document(entity.uid)
                    .set(entity.aModel())
                    .await()
                movimientoDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) {
                // Silenciamos error para que el Worker reintente después
            }
        }
    }
}

// =======================================================================
// MAPPERS
// =======================================================================

private fun MovimientoEntity.aModel(): MovimientoModel {
    return MovimientoModel(
        uid = uid,
        repuestoUid = repuestoUid,
        tipo = tipo,
        cantidad = cantidad,
        fecha = fecha?.let { Date(it) },
        registradoPor = registradoPor,
        observacion = observacion
    )
}
