package com.lingomak.lingomakapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.model.MantenimientoModel

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
        // Retorna la fecha actual para actualizar fechaActualizacion.
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
                // La máquina vuelve a estar disponible.
                "estado" to "OPERATIVA",

                // Guardamos el horómetro del último mantenimiento.
                "horometroUltimoMantenimiento" to horometroReal,

                // También actualizamos el horómetro actual,
                // porque es el valor que se muestra en el listado de maquinaria.
                "horometroActual" to horometroReal,

                // Actualizamos la fecha de modificación.
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

}