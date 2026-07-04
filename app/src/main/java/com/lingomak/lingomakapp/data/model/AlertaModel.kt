package com.lingomak.lingomakapp.data.model

data class AlertaModel (

    val uid : String = "",

    val titulo : String = "",

    val mensaje : String = "",
    var tipo: String = "",

    var prioridad: String = "",

    var fecha: String = "",

    var estadoRelacionado: String = "",

    var uidMantenimiento: String = "",
    var codigoMantenimiento: String = "",

    var uidMaquinaria: String = "",
    var nombreMaquinaria: String = ""

)