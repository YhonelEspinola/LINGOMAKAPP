package com.lingomak.lingomakapp.data.model

import androidx.annotation.Keep

@Keep
data class MaquinariaModel(


    var uid: String = "",
    var codigoMaquinaria: String = "",
    var nombre: String = "",
    var tipo: String = "",
    var marca: String = "",
    var modelo: String = "",
    var placaSerie: String = "",
    var anio: Int = 0,
    var estado: String = "",
    var horometroActual: Double = 0.0,
    var horometroUltimoMantenimiento: Double = 0.0,
    var capacidadTanqueGls: Double? = null,
    var intervaloMantenimientoHoras: Int = 250,
    var ubicacionActual: String = "",
    var imagenUrl: String = "",
    var observaciones: String = "",
    var fechaRegistro: String = "",
    var fechaActualizacion: String = "",
    var registradoPor: String = ""
) {
    fun aEntity(estadoSync: String = "SINCRONIZADO", timestampLocal: Long = System.currentTimeMillis()) = com.lingomak.lingomakapp.data.local.entity.MaquinariaEntity(
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
        registradoPor = registradoPor,
        estadoSync = estadoSync,
        timestampLocal = timestampLocal
    )
}
