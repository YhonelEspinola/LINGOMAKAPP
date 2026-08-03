package com.lingomak.lingomakapp.data.model

data class SuministroModel(
    val uid: String = "",
    val uidMaquinaria: String = "",
    val fecha: String = "",
    val horometroSuministro: Double = 0.0,
    val tipoCarga: String = "",
    val tipoCombustible: String = "",
    val galonesCombustible: Double = 0.0,
    val galonesAceite: Double = 0.0,
    val uidRegistroUso: String? = null
)
