package com.lingomak.lingomakapp.data.model

import androidx.annotation.Keep

@Keep
data class CategoriaModel(
    val uid: String = "",
    val nombre: String = "",
    val tipo: String = "", // "REPUESTO" o "MAQUINARIA"
    val estado: String = "ACTIVO",
    val fechaRegistro: Long = System.currentTimeMillis()
)
