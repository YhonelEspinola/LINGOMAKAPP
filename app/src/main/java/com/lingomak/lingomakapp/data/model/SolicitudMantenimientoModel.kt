package com.lingomak.lingomakapp.data.model

data class SolicitudMantenimientoModel(

    val uid: String = "",

    val uidMaquinaria: String = "",
    val codigoMaquinaria: String = "",
    val nombreMaquinaria: String = "",
    val tipoMaquinaria: String = "",

    val uidOperario: String = "",
    val nombreOperario: String = "",
    val correoOperario: String = "",

    val horometroActual: Int = 0,
    val horometroUltimoMantenimiento: Int = 0,
    val intervaloMantenimientoHoras: Int = 250,
    val horasDesdeUltimoMantenimiento: Int = 0,
    val horasRestantes: Int = 0,

    val motivo: String = "",

    val estadoSolicitud: String = "PENDIENTE_APROBACION",

    val origen: String = "HOROMETRO_OPERARIO",

    val fechaSugerida: String = "",

    val fechaRegistro: String = "",

    val revisadoPor: String = "",
    val fechaRevision: String = "",
    val motivoRechazo: String = "",
    val uidMantenimientoGenerado: String = ""

)
