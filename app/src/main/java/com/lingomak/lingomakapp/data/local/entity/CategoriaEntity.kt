package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingomak.lingomakapp.data.model.CategoriaModel

@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey val uid: String,
    val nombre: String,
    val tipo: String, // "REPUESTO" o "MAQUINARIA"
    val estado: String = "ACTIVO",
    val fechaRegistro: Long,
    
    // Sincronización
    val estadoSync: String = "SINCRONIZADO",
    val timestampLocal: Long = System.currentTimeMillis()
) {
    fun aModel() = CategoriaModel(
        uid = uid,
        nombre = nombre,
        tipo = tipo,
        estado = estado,
        fechaRegistro = fechaRegistro
    )
}

fun CategoriaModel.aEntity(estadoSync: String = "SINCRONIZADO", timestampLocal: Long = System.currentTimeMillis()) = CategoriaEntity(
    uid = uid,
    nombre = nombre,
    tipo = tipo,
    estado = estado,
    fechaRegistro = fechaRegistro,
    estadoSync = estadoSync,
    timestampLocal = timestampLocal
)
