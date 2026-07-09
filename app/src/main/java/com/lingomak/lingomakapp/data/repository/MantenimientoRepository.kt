package com.lingomak.lingomakapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.utils.DateUtils

class MantenimientoRepository {

    private val database = FirebaseFirestore.getInstance()

    private val coleccionMantenimientos = "mantenimientos"

    fun agregarMantenimiento(
        mantenimiento: MantenimientoModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        database.collection(coleccionMantenimientos)
            .document(mantenimiento.uid)
            .set(mantenimiento)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al guardar mantenimiento")
            }
    }

    fun listarMantenimientos(
        onSuccess: (List<MantenimientoModel>) -> Unit,
        onError: (String) -> Unit
    ){
        database.collection(coleccionMantenimientos)
            .addSnapshotListener { snapshots, error ->

                if(error != null){
                    onError(error.message ?: "Error al listar mantenimientos")
                    return@addSnapshotListener
                }
                if(snapshots == null){
                    onSuccess(emptyList())
                    return@addSnapshotListener
                }

                val lista = snapshots.documents.mapNotNull { document ->
                    document.toObject(MantenimientoModel::class.java)
                }
                onSuccess(lista)

            }

    }

    fun cambiarEstadoMantenimiento(
        uid: String,
        nuevoEstado: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        database.collection(coleccionMantenimientos)
            .document(uid)
            .update(
                mapOf(
                    "estado" to nuevoEstado,
                    "fechaActualizacion" to obtenerFechaActual()
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al cambiar estado del mantenimiento")

            }
    }

    private fun obtenerFechaActual(): String {
        return java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.getDefault()
        ).format(java.util.Date())
    }

    fun actualizarMantenimiento(
        mantenimiento: MantenimientoModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        database.collection(coleccionMantenimientos)
            .document(mantenimiento.uid)
            .update(
                mapOf(
                    "tipoMantenimiento" to mantenimiento.tipoMantenimiento,
                    "descripcion" to mantenimiento.descripcion,
                    "fechaProgramada" to mantenimiento.fechaProgramada,
                    "prioridad" to mantenimiento.prioridad,
                    "horometroProgramado" to mantenimiento.horometroProgramado,
                    "costoEstimado" to mantenimiento.costoEstimado,
                    "observaciones" to mantenimiento.observaciones,
                    "fechaActualizacion" to obtenerFechaActual()
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al actualizar mantenimiento")
            }
    }

    fun finalizarMantenimiento(
        uid: String,
        uidMaquinaria: String,
        fechaRealizada: String,
        horometroReal: Int,
        costoReal: Double,
        observacionesFinales: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        val batch = database.batch()

        val mantenimientoRef =
            database.collection(coleccionMantenimientos).document(uid)

        val maquinariaRef =
            database.collection("maquinarias").document(uidMaquinaria)

        batch.update(
            mantenimientoRef,
            mapOf(
                "estado" to "FINALIZADO",
                "fechaRealizada" to fechaRealizada,
                "horometroReal" to horometroReal,
                "costoReal" to costoReal,
                "observaciones" to observacionesFinales,
                "fechaActualizacion" to obtenerFechaActual()
            )
        )

        batch.update(
            maquinariaRef,
            mapOf(
                "estado" to "OPERATIVA",

                "horometroUltimoMantenimiento" to horometroReal,

                "horometroActual" to horometroReal,

                "fechaActualizacion" to obtenerFechaActual()
            )
        )

        batch.commit()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al finalizar mantenimiento")
            }
    }

    fun iniciarMantenimiento(
        uidMantenimiento: String,
        uidMaquinaria: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){

        val batch = database.batch()

        val mantenimientoRef =
            database.collection(coleccionMantenimientos).document(uidMantenimiento)

        val maquinariaRef =
            database.collection("maquinarias").document(uidMaquinaria)

        batch.update(
            mantenimientoRef,
            mapOf(
                "estado" to "EN_PROCESO",
                "fechaActualizacion" to obtenerFechaActual()
            )
        )

        batch.update(
            maquinariaRef,
            mapOf(
                "estado" to "EN_MANTENIMIENTO",
                "fechaActualizacion" to obtenerFechaActual()
            )
        )

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al iniciar mantenimiento")
            }

    }

    fun validarMantenimientoActivo(
        uidMaquinaria: String,
        onExiste: () -> Unit,
        onNoExiste: () -> Unit,
        onError: (String) -> Unit
    ) {

        database.collection(coleccionMantenimientos)
            .whereEqualTo("uidMaquinaria", uidMaquinaria)
            .whereIn("estado", listOf("PENDIENTE", "EN_PROCESO"))
            .get()
            .addOnSuccessListener { result ->

                if (result.isEmpty) {
                    onNoExiste()
                } else {
                    onExiste()
                }
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.message
                        ?: "Error al validar mantenimiento activo"
                )
            }
    }

    fun actualizarMantenimientosVencidos(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {


        database.collection(coleccionMantenimientos)
            .whereEqualTo("estado", "PENDIENTE")
            .get()
            .addOnSuccessListener { result ->

                val batch = database.batch()

                var hayCambios = false

                result.documents.forEach { document ->

                    val mantenimiento =
                        document.toObject(MantenimientoModel::class.java)

                    if (
                        mantenimiento != null &&
                        DateUtils.fechaYaPaso(mantenimiento.fechaProgramada)
                    ) {
                        hayCambios = true

                        batch.update(
                            document.reference,
                            mapOf(
                                "estado" to "VENCIDO",
                                "fechaActualizacion" to DateUtils.obtenerFechaActual()
                            )
                        )
                    }
                }

                if (hayCambios) {
                    batch.commit()
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { exception ->
                            onError(
                                exception.message
                                    ?: "Error al actualizar mantenimientos vencidos"
                            )
                        }
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.message
                        ?: "Error al consultar mantenimientos pendientes"
                )
            }
    }

    fun obtenerMantenimientoPorUid(
        uid: String,
        onSuccess: (MantenimientoModel) -> Unit,
        onError: (String) -> Unit
    ) {
        /*
         * Buscamos un mantenimiento específico por su UID.
         * Esto nos servirá cuando el usuario presione
         * "Tomar acción" desde una alerta.
         */
        database.collection(coleccionMantenimientos)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                val mantenimiento =
                    document.toObject(MantenimientoModel::class.java)

                if (mantenimiento != null) {
                    onSuccess(mantenimiento)
                } else {
                    onError("No se encontró el mantenimiento")
                }
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.message ?: "Error al obtener mantenimiento"
                )
            }
    }

}