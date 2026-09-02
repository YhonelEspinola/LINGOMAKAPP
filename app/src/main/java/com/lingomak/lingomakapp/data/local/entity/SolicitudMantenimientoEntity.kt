package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solicitudes_mantenimiento")
data class SolicitudMantenimientoEntity(
    @PrimaryKey val uid: String,
    val uidMaquinaria: String,
    val codigoMaquinaria: String,
    val nombreMaquinaria: String,
    val tipoMaquinaria: String,
    val uidOperario: String,
    val nombreOperario: String,
    val correoOperario: String,
    val horometroActual: Double,
    val horometroUltimoMantenimiento: Double,
    val intervaloMantenimientoHoras: Double,
    val horasDesdeUltimoMantenimiento: Double,
    val horasRestantes: Double,
    val motivo: String,
    val estadoSolicitud: String,
    val origen: String,
    val fechaSugerida: String,
    val fechaRegistro: String,
    val revisadoPor: String = "",
    val fechaRevision: String = "",
    val motivoRechazo: String = "",
    val uidMantenimientoGenerado: String = "",
    
    // Control de sincronización
    val estadoSync: String = "SINCRONIZADO",
    val timestampLocal: Long = System.currentTimeMillis()
)
