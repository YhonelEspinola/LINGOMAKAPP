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
    val horometroAnterior: Double,
    val horasUso: Double,
    val horometroFinal: Double,
    val trabajoRealizado: String,
    val fechaRegistro: String,
    
    // Nuevos campos
    val tipoMovimiento: String = "Trabajo",
    val obra: String? = null,
    val contratista: String? = null,
    val ubicacion: String? = null,

    // Auditoría
    val modificadoPorUid: String? = null,
    val modificadoPorNombre: String? = null,
    val fechaUltimaModificacion: String? = null,

    // Datos de Repostaje Unificados
    val galonesCombustible: Double = 0.0,
    val galonesAceite: Double = 0.0,
    val tipoCombustible: String = "",
    val tipoCarga: String = "",
    
    // Control de sincronización
    val estadoSync: String = "SINCRONIZADO",
    val timestampLocal: Long = System.currentTimeMillis()
)
