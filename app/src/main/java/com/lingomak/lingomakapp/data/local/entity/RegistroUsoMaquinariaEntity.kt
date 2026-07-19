package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registros_uso_maquinaria")
data class RegistroUsoMaquinariaEntity(
    @PrimaryKey val uid: String,
    val uidMaquinaria: String,
    val codigoMaquinaria: String,
    val nombreMaquinaria: String,
    val tipoMaquinaria: String,
    val uidOperario: String,
    val nombreOperario: String,
    val correoOperario: String,
    val fechaUso: String,
    val horometroAnterior: Int,
    val horasUso: Int,
    val horometroFinal: Int,
    val observacion: String,
    val fechaRegistro: String,
    
    // Control de sincronización
    val estadoSync: String = "SINCRONIZADO",
    val timestampLocal: Long = System.currentTimeMillis()
)
