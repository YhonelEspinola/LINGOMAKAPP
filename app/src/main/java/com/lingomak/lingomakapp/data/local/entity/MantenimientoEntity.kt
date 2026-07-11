package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mantenimientos")
data class MantenimientoEntity(
    @PrimaryKey val uid: String,
    val codigoMantenimiento: String,
    val uidMaquinaria: String,
    val codigoMaquinaria: String,
    val nombreMaquinaria: String,
    val tipoMaquinaria: String,
    val tipoMantenimiento: String,
    val descripcion: String,
    val fechaProgramada: String,
    val fechaRealizada: String,
    val estado: String,
    val responsable: String,
    val responsableUid: String,
    val observaciones: String,
    val costoEstimado: Double,
    val costoReal: Double,
    val horometroProgramado: Int,
    val horometroReal: Int,
    val fechaRegistro: String,
    val fechaActualizacion: String,
    val registradoPor: String,
    val actualizadoPor: String,
    val prioridad: String,
    
    // Campos para sincronización
    val estadoSync: String = "SINCRONIZADO",
    val timestampLocal: Long = System.currentTimeMillis()
)