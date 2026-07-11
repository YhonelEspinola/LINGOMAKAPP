package com.lingomak.lingomakapp.data.model

data class MantenimientoModel(
    var uid: String = "",
    var codigoMantenimiento: String = "",
    var uidMaquinaria: String = "",
    var codigoMaquinaria: String = "",
    var nombreMaquinaria: String = "",
    var tipoMaquinaria: String = "",
    var tipoMantenimiento: String = "",
    var descripcion: String = "",
    var fechaProgramada: String = "",
    var fechaRealizada: String = "",
    var estado: String = "",
    var responsable: String = "",
    var responsableUid: String = "", // UID del operario asignado
    var observaciones: String = "",
    var costoEstimado: Double = 0.0,
    var costoReal: Double = 0.0,
    var horometroProgramado: Int = 0,
    var horometroReal: Int = 0,
    var fechaRegistro: String = "",
    var fechaActualizacion: String = "",
    var registradoPor: String = "",
    var actualizadoPor: String = "",
    var prioridad: String = ""
)
