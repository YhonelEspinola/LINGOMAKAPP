package com.lingomak.lingomakapp.data.model

data class RegistroUsoMaquinariaModel (
    val uid: String = "",

    val uidMaquinaria: String = "",
    val codigoMaquinaria: String = "",
    val nombreMaquinaria: String = "",
    val tipoMaquinaria: String = "",

    val uidOperario: String = "",
    val nombreOperario: String = "",
    val correoOperario: String = "",

    val fechaUso: String = "",
    val horometroAnterior: Double = 0.0,
    val horasUso: Double = 0.0,
    val horometroFinal: Double = 0.0,
    val trabajoRealizado: String = "",
    val fechaRegistro: String = "",

    // Nuevos campos
    val tipoMovimiento: String = "Trabajo",
    val obra: String? = null,
    val contratista: String? = null,
    val ubicacion: String? = null,

    // Auditoría
    val modificadoPorUid: String? = null,
    val modificadoPorNombre: String? = null,
    val fechaUltimaModificacion: String? = null,

    // Datos de Repostaje Unificados
    val galonesCombustible: Double = 0.0,
    val galonesAceite: Double = 0.0,
    val tipoCombustible: String = "",
    val tipoCarga: String = "" // Completa o Parcial
)
