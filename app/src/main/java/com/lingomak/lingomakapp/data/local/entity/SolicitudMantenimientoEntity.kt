package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel

@Entity(tableName = "solicitudes_mantenimiento")
data class SolicitudMantenimientoEntity(
    @PrimaryKey val uid: String,
    val uidMaquinaria: String,
    val codigoMaquinaria: String,
    val nombreMaquinaria: String,
    val tipoMaquinaria: String,
    val uidOperario: String,
    val nombreOperario: String,
    val correoOperario: String,
    val horometroActual: Double,
    val horometroUltimoMantenimiento: Double,
    val intervaloMantenimientoHoras: Double,
    val horasDesdeUltimoMantenimiento: Double,
    val horasRestantes: Double,
    val motivo: String,
    val estadoSolicitud: String,
    val origen: String,
    val fechaSugerida: String,
    val fechaRegistro: String,
    val revisadoPor: String,
    val fechaRevision: String,
    val motivoRechazo: String,
    val uidMantenimientoGenerado: String,
    
    val estadoSync: String = "SINCRONIZADO",
    val timestampLocal: Long = System.currentTimeMillis()
) {
    fun aModel() = SolicitudMantenimientoModel(
        uid = uid,
        uidMaquinaria = uidMaquinaria,
        codigoMaquinaria = codigoMaquinaria,
        nombreMaquinaria = nombreMaquinaria,
        tipoMaquinaria = tipoMaquinaria,
        uidOperario = uidOperario,
        nombreOperario = nombreOperario,
        correoOperario = correoOperario,
        horometroActual = horometroActual,
        horometroUltimoMantenimiento = horometroUltimoMantenimiento,
        intervaloMantenimientoHoras = intervaloMantenimientoHoras,
        horasDesdeUltimoMantenimiento = horasDesdeUltimoMantenimiento,
        horasRestantes = horasRestantes,
        motivo = motivo,
        estadoSolicitud = estadoSolicitud,
        origen = origen,
        fechaSugerida = fechaSugerida,
        fechaRegistro = fechaRegistro,
        revisadoPor = revisadoPor,
        fechaRevision = fechaRevision,
        motivoRechazo = motivoRechazo,
        uidMantenimientoGenerado = uidMantenimientoGenerado
    )
}

fun SolicitudMantenimientoModel.aEntity(estadoSync: String = "SINCRONIZADO", timestampLocal: Long = System.currentTimeMillis()) = SolicitudMantenimientoEntity(
    uid = uid,
    uidMaquinaria = uidMaquinaria,
    codigoMaquinaria = codigoMaquinaria,
    nombreMaquinaria = nombreMaquinaria,
    tipoMaquinaria = tipoMaquinaria,
    uidOperario = uidOperario,
    nombreOperario = nombreOperario,
    correoOperario = correoOperario,
    horometroActual = horometroActual,
    horometroUltimoMantenimiento = horometroUltimoMantenimiento,
    intervaloMantenimientoHoras = intervaloMantenimientoHoras,
    horasDesdeUltimoMantenimiento = horasDesdeUltimoMantenimiento,
    horasRestantes = horasRestantes,
    motivo = motivo,
    estadoSolicitud = estadoSolicitud,
    origen = origen,
    fechaSugerida = fechaSugerida,
    fechaRegistro = fechaRegistro,
    revisadoPor = revisadoPor,
    fechaRevision = fechaRevision,
    motivoRechazo = motivoRechazo,
    uidMantenimientoGenerado = uidMantenimientoGenerado,
    estadoSync = estadoSync,
    timestampLocal = timestampLocal
)
