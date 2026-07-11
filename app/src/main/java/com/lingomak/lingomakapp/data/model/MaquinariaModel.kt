package com.lingomak.lingomakapp.data.model

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
    var horometroActual: Int = 0,
    var horometroUltimoMantenimiento: Int = 0,
    var intervaloMantenimientoHoras: Int = 250,
    var ubicacionActual: String = "",
    var imagenUrl: String = "",
    var observaciones: String = "",
    var fechaRegistro: String = "",
    var fechaActualizacion: String = "",
    var registradoPor: String = ""
)