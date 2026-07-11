package com.lingomak.lingomakapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel
import com.lingomak.lingomakapp.utils.DateUtils

class SolicitudMantenimientoRepository {

    private val database = FirebaseFirestore.getInstance()

    private val coleccionSolicitudes =
        "solicitudes_mantenimiento"

    fun listarSolicitudesPendientes(
        onSuccess: (List<SolicitudMantenimientoModel>) -> Unit,
        onError: (String) -> Unit
    ) {
        database.collection(coleccionSolicitudes)
            .whereEqualTo(
                "estadoSolicitud",
                "PENDIENTE_APROBACION"
            )
            .addSnapshotListener { snapshots, error ->

                if (error != null) {
                    onError(
                        error.message
                            ?: "Error al listar solicitudes"
                    )
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    onSuccess(emptyList())
                    return@addSnapshotListener
                }

                val solicitudes =
                    snapshots.documents.mapNotNull { document ->

                        document.toObject(
                            SolicitudMantenimientoModel::class.java
                        )
                    }.sortedBy { solicitud ->

                        solicitud.horasRestantes
                    }

                onSuccess(solicitudes)
            }
    }

    fun rechazarSolicitud(
        uidSolicitud: String,
        uidAdministrador: String,
        motivoRechazo: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val datosRevision = mapOf(
            "estadoSolicitud" to "RECHAZADA",
            "revisadoPor" to uidAdministrador,
            "fechaRevision" to DateUtils.obtenerFechaActual(),
            "motivoRechazo" to motivoRechazo
        )

        database.collection(coleccionSolicitudes)
            .document(uidSolicitud)
            .update(datosRevision)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.message
                        ?: "Error al rechazar la solicitud"
                )
            }
    }

    fun marcarComoConvertida(
        uidSolicitud: String,
        uidAdministrador: String,
        uidMantenimientoGenerado: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val datosRevision = mapOf(
            "estadoSolicitud" to "CONVERTIDA_A_MANTENIMIENTO",
            "revisadoPor" to uidAdministrador,
            "fechaRevision" to DateUtils.obtenerFechaActual(),
            "motivoRechazo" to "",
            "uidMantenimientoGenerado" to uidMantenimientoGenerado
        )

        database.collection(coleccionSolicitudes)
            .document(uidSolicitud)
            .update(datosRevision)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.message
                        ?: "Error al actualizar la solicitud"
                )
            }
    }
}