package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maquinarias")
data class MaquinariaEntity(
    @PrimaryKey val uid: String,
    val codigoMaquinaria: String,
    val nombre: String,
    val tipo: String,
    val marca: String,
    val modelo: String,
    val placaSerie: String,
    val anio: Int,
    val estado: String,
    val horometroActual: Int,
    val horometroUltimoMantenimiento: Int,
    val ubicacionActual: String,
    val imagenUrl: String,
    val observaciones: String,
    val fechaRegistro: String,
    val fechaActualizacion: String,
    val registradoPor: String,
    
    // Campos para sincronización
    val estadoSync: String = "SINCRONIZADO", // PENDIENTE_CREAR, PENDIENTE_ACTUALIZAR, SINCRONIZADO
    val timestampLocal: Long = System.currentTimeMillis()
)