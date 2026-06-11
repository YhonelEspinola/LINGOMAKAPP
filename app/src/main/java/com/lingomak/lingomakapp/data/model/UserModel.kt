package com.lingomak.lingomakapp.data.model

data class UserModel (

    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val rol: String = "",
    val estado: String = "",
    val debeCambiarPassword: Boolean = false,
    val fechaCreacion : String = "",
    val creadoPor : String = "",
)