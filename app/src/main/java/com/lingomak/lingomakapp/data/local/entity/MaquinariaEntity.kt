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
    val horometroActual: Double,
    val horometroUltimoMantenimiento: Double,
    val capacidadTanqueGls: Double? = null,
    val intervaloMantenimientoHoras: Double = 250.0,
    val ubicacionActual: String,
    val imagenUrl: String,
    val observaciones: String,
    val fechaRegistro: String,
    val fechaActualizacion: String,
    val registradoPor: String,
    
    // Campos para sincronización
    val estadoSync: String = "SINCRONIZADO",
    val timestampLocal: Long = System.currentTimeMillis()
) {
    fun aModel() = com.lingomak.lingomakapp.data.model.MaquinariaModel(
        uid = uid,
        codigoMaquinaria = codigoMaquinaria,
        nombre = nombre,
        tipo = tipo,
        marca = marca,
        modelo = modelo,
        placaSerie = placaSerie,
        anio = anio,
        estado = estado,
        horometroActual = horometroActual,
        horometroUltimoMantenimiento = horometroUltimoMantenimiento,
        capacidadTanqueGls = capacidadTanqueGls,
        intervaloMantenimientoHoras = intervaloMantenimientoHoras,
        ubicacionActual = ubicacionActual,
        imagenUrl = imagenUrl,
        observaciones = observaciones,
        fechaRegistro = fechaRegistro,
        fechaActualizacion = fechaActualizacion,
        registradoPor = registradoPor
    )
}
