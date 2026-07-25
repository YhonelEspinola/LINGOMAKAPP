package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingomak.lingomakapp.data.model.UserModel

@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey val uid: String,
    val nombre: String,
    val correo: String,
    val rol: String,
    val estado: String,
    val debeCambiarPassword: Boolean,
    val fechaCreacion: String,
    val creadoPor: String,
    
    // Sincronización
    val estadoSync: String = "SINCRONIZADO",
    val timestampLocal: Long = System.currentTimeMillis()
) {
    fun aModel() = UserModel(
        uid = uid,
        nombre = nombre,
        correo = correo,
        rol = rol,
        estado = estado,
        debeCambiarPassword = debeCambiarPassword,
        fechaCreacion = fechaCreacion,
        creadoPor = creadoPor
    )
}

fun UserModel.aEntity(estadoSync: String = "SINCRONIZADO", timestampLocal: Long = System.currentTimeMillis()) = UserEntity(
    uid = uid,
    nombre = nombre,
    correo = correo,
    rol = rol,
    estado = estado,
    debeCambiarPassword = debeCambiarPassword,
    fechaCreacion = fechaCreacion,
    creadoPor = creadoPor,
    estadoSync = estadoSync,
    timestampLocal = timestampLocal
)
