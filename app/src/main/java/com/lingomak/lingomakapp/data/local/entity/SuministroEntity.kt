package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suministros")
data class SuministroEntity(
    @PrimaryKey val uid: String,
    val uidMaquinaria: String,
    val fecha: String,
    val horometroSuministro: Double,
    val tipoCarga: String,
    val tipoCombustible: String,
    val galonesCombustible: Double,
    val galonesAceite: Double,
    val uidRegistroUso: String? = null,
    
    // Control de sincronización
    val estadoSync: String = "SINCRONIZADO",
    val timestampLocal: Long = System.currentTimeMillis()
)
