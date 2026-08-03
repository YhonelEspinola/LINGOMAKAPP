package com.lingomak.lingomakapp.data.model

import androidx.annotation.Keep
import java.util.Date

@Keep
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
    val proveedorNombre: String = "",
    val proveedorContacto: String = "",
    val estado: String = "ACTIVO",
    val fechaRegistro: Date? = null,
    val fechaActualizacion: Date? = null,
    val registradoPor: String = "",
    val actualizadoPor: String = "",

    // Auditoría detallada
    val modificadoPorUid: String? = null,
    val modificadoPorNombre: String? = null,
    val fechaUltimaModificacion: String? = null
)
