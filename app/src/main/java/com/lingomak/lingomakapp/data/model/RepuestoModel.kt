package com.lingomak.lingomakapp.data.model

import java.util.Date

data class RepuestoModel(
    val uid: String = "",
    val codigoInterno: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val marca: String = "",
    val descripcion: String = "",
    val stockActual: Int = 0,
    val stockMinimo: Int = 0,
    val stockMaximo: Int = 0,
    val ubicacionAlmacen: String = "",
    val imagenUrl: String = "",
    val codigoQR: String = "",
    val estado: String = "ACTIVO",
    val fechaRegistro: Date? = null,
    val fechaActualizacion: Date? = null,
    val registradoPor: String = "",
    val actualizadoPor: String = ""
)
