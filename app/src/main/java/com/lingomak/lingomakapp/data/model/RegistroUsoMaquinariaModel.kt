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
    val horometroAnterior: Int = 0,
    val horasUso: Int = 0,
    val horometroFinal: Int = 0,
    val observacion: String = "",
    val fechaRegistro: String = ""

)