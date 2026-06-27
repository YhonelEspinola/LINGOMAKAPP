package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla local (Room) que espeja MovimientoModel, más los campos de
 * control de sincronización offline-first.
 *
 * A diferencia de RepuestoEntity, los movimientos son registros
 * append-only (nunca se editan ni se borran después de creados), así
 * que su sincronización es más simple: solo existe el estado
 * "PENDIENTE_CREAR" -> "SINCRONIZADO", sin conflictos de last-write-wins
 * que resolver (no hay ediciones concurrentes posibles sobre el mismo
 * movimiento).
 *
 * Se indexa repuestoUid porque la pantalla de historial siempre filtra
 * por ese campo (ver MovimientoDao.obtenerPorRepuestoObservable).
 */
@Entity(
    tableName = "movimientos",
    indices = [Index(value = ["repuestoUid"])]
)
data class MovimientoEntity(
    @PrimaryKey
    val uid: String,
    val repuestoUid: String = "",
    val tipo: String = "", // "ENTRADA" o "SALIDA"
    val cantidad: Int = 0,
    val fecha: Long? = null, // epoch millis
    val registradoPor: String = "",
    val observacion: String = "",

    // ----- Control de sincronización -----
    val estadoSync: String = "SINCRONIZADO", // "SINCRONIZADO" o "PENDIENTE_CREAR"
    val timestampLocal: Long = System.currentTimeMillis()
)
