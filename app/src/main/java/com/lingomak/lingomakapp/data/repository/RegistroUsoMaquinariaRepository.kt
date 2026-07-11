package com.lingomak.lingomakapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.RegistroUsoMaquinariaModel
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel
import com.lingomak.lingomakapp.utils.DateUtils
import java.util.UUID

class RegistroUsoMaquinariaRepository {

    private val database = FirebaseFirestore.getInstance()

    private val coleccionMaquinaria = "maquinarias"
    private val coleccionRegistrosUso = "registros_uso_maquinaria"
    private val coleccionSolicitudes = "solicitudes_mantenimiento"

    fun registrarUsoMaquinaria(
        uidMaquinaria: String,
        uidOperario: String,
        nombreOperario: String,
        correoOperario: String,
        horasUso: Int,
        observacion: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        database.collection(coleccionMaquinaria)
            .document(uidMaquinaria)
            .get()
            .addOnSuccessListener { document ->

                val maquinaria =
                    document.toObject(MaquinariaModel::class.java)

                if (maquinaria == null) {
                    onError("No se encontró la maquinaria")
                    return@addOnSuccessListener
                }


                val horometroAnterior =
                    maquinaria.horometroActual

                val horometroFinal =
                    horometroAnterior + horasUso

                val horasDesdeUltimoMantenimiento =
                    horometroFinal - maquinaria.horometroUltimoMantenimiento

                val horasRestantes =
                    maquinaria.intervaloMantenimientoHoras - horasDesdeUltimoMantenimiento


                val registroUso = RegistroUsoMaquinariaModel(
                    uid = UUID.randomUUID().toString(),

                    uidMaquinaria = maquinaria.uid,
                    codigoMaquinaria = maquinaria.codigoMaquinaria,
                    nombreMaquinaria = maquinaria.nombre,
                    tipoMaquinaria = maquinaria.tipo,

                    uidOperario = uidOperario,
                    nombreOperario = nombreOperario,
                    correoOperario = correoOperario,

                    fechaUso = DateUtils.obtenerFechaActual(),
                    horometroAnterior = horometroAnterior,
                    horasUso = horasUso,
                    horometroFinal = horometroFinal,
                    observacion = observacion,
                    fechaRegistro = DateUtils.obtenerFechaActual()
                )


                val batch = database.batch()

                val registroRef =
                    database.collection(coleccionRegistrosUso)
                        .document(registroUso.uid)

                batch.set(registroRef, registroUso)

                val maquinariaRef =
                    database.collection(coleccionMaquinaria)
                        .document(maquinaria.uid)

                batch.update(
                    maquinariaRef,
                    mapOf(
                        "horometroActual" to horometroFinal,
                        "fechaActualizacion" to DateUtils.obtenerFechaActual()
                    )
                )


                if (horasRestantes > 20) {

                    batch.commit()
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { exception ->
                            onError(
                                exception.message
                                    ?: "Error al registrar uso de maquinaria"
                            )
                        }

                    return@addOnSuccessListener
                }

                buscarSolicitudPendiente(
                    uidMaquinaria = maquinaria.uid,

                    onResult = { solicitudPendiente ->

                        if (solicitudPendiente == null) {

                            val solicitud = SolicitudMantenimientoModel(
                                uid = UUID.randomUUID().toString(),
                                uidMaquinaria = maquinaria.uid,
                                codigoMaquinaria = maquinaria.codigoMaquinaria,
                                nombreMaquinaria = maquinaria.nombre,
                                tipoMaquinaria = maquinaria.tipo,
                                uidOperario = uidOperario,
                                nombreOperario = nombreOperario,
                                correoOperario = correoOperario,
                                horometroActual = horometroFinal,
                                horometroUltimoMantenimiento =
                                    maquinaria.horometroUltimoMantenimiento,
                                intervaloMantenimientoHoras =
                                    maquinaria.intervaloMantenimientoHoras,
                                horasDesdeUltimoMantenimiento =
                                    horasDesdeUltimoMantenimiento,
                                horasRestantes = horasRestantes,
                                motivo = obtenerMotivoSolicitud(horasRestantes),
                                estadoSolicitud = "PENDIENTE_APROBACION",
                                origen = "HOROMETRO_OPERARIO",
                                fechaSugerida = DateUtils.obtenerFechaActual(),
                                fechaRegistro = DateUtils.obtenerFechaActual()
                            )

                            val solicitudRef =
                                database.collection(coleccionSolicitudes)
                                    .document(solicitud.uid)

                            batch.set(solicitudRef, solicitud)

                        } else {

                            val solicitudRef =
                                database.collection(coleccionSolicitudes)
                                    .document(solicitudPendiente.id)

                            batch.update(
                                solicitudRef,
                                mapOf(
                                    "horometroActual" to horometroFinal,
                                    "horasDesdeUltimoMantenimiento" to
                                            horasDesdeUltimoMantenimiento,
                                    "horasRestantes" to horasRestantes,
                                    "motivo" to obtenerMotivoSolicitud(
                                        horasRestantes
                                    ),
                                    "fechaSugerida" to
                                            DateUtils.obtenerFechaActual()
                                )
                            )
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                onSuccess()
                            }
                            .addOnFailureListener { exception ->
                                onError(
                                    exception.message
                                        ?: "Error al registrar uso de maquinaria"
                                )
                            }
                    },

                    onError = { mensaje ->
                        onError(mensaje)
                    }
                )
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.message
                        ?: "Error al obtener la maquinaria"
                )
            }
    }


    private fun buscarSolicitudPendiente(
        uidMaquinaria: String,
        onResult: (com.google.firebase.firestore.DocumentSnapshot?) -> Unit,
        onError: (String) -> Unit
    ) {
        database.collection(coleccionSolicitudes)
            .whereEqualTo("uidMaquinaria", uidMaquinaria)
            .whereEqualTo("estadoSolicitud", "PENDIENTE_APROBACION")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshots ->
                onResult(snapshots.documents.firstOrNull())
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.message
                        ?: "Error al verificar solicitudes pendientes"
                )
            }
    }


    private fun obtenerMotivoSolicitud(
        horasRestantes: Int
    ): String {

        return when {

            horasRestantes < 0 -> {
                val horasExcedidas = kotlin.math.abs(horasRestantes)

                "La maquinaria superó el intervalo de mantenimiento " +
                        "por $horasExcedidas horas."
            }

            horasRestantes == 0 -> {
                "La maquinaria alcanzó exactamente el intervalo " +
                        "de mantenimiento preventivo."
            }

            else -> {
                "La maquinaria está próxima a cumplir el intervalo " +
                        "de mantenimiento. Faltan $horasRestantes horas."
            }
        }
    }

}