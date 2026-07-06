package com.lingomak.lingomakapp.data.model

data class AlertaModel(
    var uid: String = "",

    var categoria: String = "",
    var icono: String = "",
    var titulo: String = "",
    var mensaje: String = "",
    var tipo: String = "",
    var prioridad: String = "",
    var fecha: String = "",
    var estadoRelacionado: String = "",
    var uidMantenimiento: String = "",
    var codigoMantenimiento: String = "",
    var uidMaquinaria: String = "",
    var nombreMaquinaria: String = "",
    var uidRepuesto: String = "",
    var codigoRepuesto: String = "",
    var nombreRepuesto: String = "",
    var stockActual: Int = 0,
    var stockMinimo: Int = 0
)