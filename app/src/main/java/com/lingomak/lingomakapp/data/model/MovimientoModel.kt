package com.lingomak.lingomakapp.data.model

import androidx.annotation.Keep
import java.util.Date

@Keep
data class MovimientoModel(
    val uid: String = "",
    val repuestoUid: String = "",
    val tipo: String = "", // "ENTRADA" o "SALIDA"
    val cantidad: Int = 0,
    val fecha: Date? = null,
    val registradoPor: String = "",
    val observacion: String = "",
    val destinoSalida: String = "", // "CONSUMO_INTERNO" o "DISTRIBUCION_EXTERNA"
    val ordenMantenimientoUid: String? = null,
    val maquinariaUid: String? = null,
    val nombreRegistradoPor: String = "",
    val codigoMantenimientoAsociado: String? = null,
    val nombreMaquinariaAsociada: String? = null
)
