package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingomak.lingomakapp.data.model.AlertaModel

@Entity(tableName = "alertas")
data class AlertaEntity(
    @PrimaryKey val uid: String,
    val categoria: String,
    val icono: String,
    val titulo: String,
    val mensaje: String,
    val tipo: String,
    val prioridad: String,
    val fecha: String,
    val estadoRelacionado: String,
    val uidMantenimiento: String,
    val codigoMantenimiento: String,
    val uidMaquinaria: String,
    val nombreMaquinaria: String,
    val uidRepuesto: String,
    val codigoRepuesto: String,
    val nombreRepuesto: String,
    val stockActual: Int,
    val stockMinimo: Int,
    val uidSolicitudMantenimiento: String,
    
    val timestampLocal: Long = System.currentTimeMillis()
) {
    fun aModel() = AlertaModel(
        uid = uid,
        categoria = categoria,
        icono = icono,
        titulo = titulo,
        mensaje = mensaje,
        tipo = tipo,
        prioridad = prioridad,
        fecha = fecha,
        estadoRelacionado = estadoRelacionado,
        uidMantenimiento = uidMantenimiento,
        codigoMantenimiento = codigoMantenimiento,
        uidMaquinaria = uidMaquinaria,
        nombreMaquinaria = nombreMaquinaria,
        uidRepuesto = uidRepuesto,
        codigoRepuesto = codigoRepuesto,
        nombreRepuesto = nombreRepuesto,
        stockActual = stockActual,
        stockMinimo = stockMinimo,
        uidSolicitudMantenimiento = uidSolicitudMantenimiento
    )
}

fun AlertaModel.aEntity() = AlertaEntity(
    uid = uid,
    categoria = categoria,
    icono = icono,
    titulo = titulo,
    mensaje = mensaje,
    tipo = tipo,
    prioridad = prioridad,
    fecha = fecha,
    estadoRelacionado = estadoRelacionado,
    uidMantenimiento = uidMantenimiento,
    codigoMantenimiento = codigoMantenimiento,
    uidMaquinaria = uidMaquinaria,
    nombreMaquinaria = nombreMaquinaria,
    uidRepuesto = uidRepuesto,
    codigoRepuesto = codigoRepuesto,
    nombreRepuesto = nombreRepuesto,
    stockActual = stockActual,
    stockMinimo = stockMinimo,
    uidSolicitudMantenimiento = uidSolicitudMantenimiento
)
