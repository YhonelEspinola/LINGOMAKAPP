package com.lingomak.lingomakapp.data.model

import androidx.annotation.Keep

@Keep
data class SolicitudMantenimientoModel(
    var uid: String = "",
    var uidMaquinaria: String = "",
    var codigoMaquinaria: String = "",
    var nombreMaquinaria: String = "",
    var tipoMaquinaria: String = "",
    var uidOperario: String = "",
    var nombreOperario: String = "",
    var correoOperario: String = "",
    var horometroActual: Double = 0.0,
    var horometroUltimoMantenimiento: Double = 0.0,
    var intervaloMantenimientoHoras: Double = 0.0,
    var horasDesdeUltimoMantenimiento: Double = 0.0,
    var horasRestantes: Double = 0.0,
    var motivo: String = "",
    var estadoSolicitud: String = "PENDIENTE_APROBACION",
    var origen: String = "AUTO_HOROMETRO",
    var fechaSugerida: String = "",
    var fechaRegistro: String = "",
    var revisadoPor: String = "",
    var fechaRevision: String = "",
    var motivoRechazo: String = "",
    var uidMantenimientoGenerado: String = ""
)
