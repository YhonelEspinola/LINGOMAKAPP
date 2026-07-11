package com.lingomak.lingomakapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.MantenimientoEntity
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.utils.DateUtils
import kotlinx.coroutines.tasks.await

class MantenimientoRepository(context: Context) {

    private val database = FirebaseFirestore.getInstance()
    private val coleccionMantenimientos = "mantenimientos"
    
    private val maintenanceDao = AppDatabase.getInstance(context).mantenimientoDao()

    fun obtenerTodosObservable(): LiveData<List<MantenimientoModel>> {
        return maintenanceDao.obtenerTodosObservable().map { entities ->
            entities.map { it.aModel() }
        }
    }

    fun obtenerAsignadosObservable(userUid: String): LiveData<List<MantenimientoModel>> {
        return maintenanceDao.obtenerAsignadosObservable(userUid).map { entities ->
            entities.map { it.aModel() }
        }
    }

    suspend fun descargarCambiosDeFirestore() {
        try {
            val snapshot = database.collection(coleccionMantenimientos).get().await()
            val remotos = snapshot.toObjects(MantenimientoModel::class.java)
            
            if (remotos.isNotEmpty()) {
                val entities = remotos.map { it.aEntity(estadoSync = "SINCRONIZADO") }
                maintenanceDao.insertarLista(entities)
            }
        } catch (e: Exception) {
            // Error de red
        }
    }

    suspend fun descargarCambiosPaginados(ultimaFecha: String? = null, batchSize: Long = 50) {
        try {
            var query = database.collection(coleccionMantenimientos)
                .orderBy("fechaProgramada", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(batchSize)

            if (ultimaFecha != null) {
                query = query.startAfter(ultimaFecha)
            }

            val snapshot = query.get().await()
            val remotos = snapshot.toObjects(MantenimientoModel::class.java)

            if (remotos.isNotEmpty()) {
                val entities = remotos.map { it.aEntity(estadoSync = "SINCRONIZADO") }
                maintenanceDao.insertarLista(entities)
            }
        } catch (e: Exception) {
            // Manejar error
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
                var huboCambios = false

                for (document in result) {
                    val mantenimiento = document.toObject(MantenimientoModel::class.java)
                    if (DateUtils.fechaYaPaso(mantenimiento.fechaProgramada)) {
                        batch.update(
                            document.reference,
                            mapOf(
                                "estado" to "VENCIDO",
                                "fechaActualizacion" to obtenerFechaActual()
                            )
                        )
                        huboCambios = true
                    }
                }

                if (huboCambios) {
                    batch.commit()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e ->
                            onError(e.message ?: "Error al actualizar mantenimientos vencidos")
                        }
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error al consultar mantenimientos")
            }
    }

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

    fun obtenerMantenimientoPorUid(
        uid: String,
        onSuccess: (MantenimientoModel) -> Unit,
        onError: (String) -> Unit
    ) {
        database.collection(coleccionMantenimientos)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val mantenimiento = document.toObject(MantenimientoModel::class.java)
                if (mantenimiento != null) {
                    onSuccess(mantenimiento)
                } else {
                    onError("No se encontró el mantenimiento")
                }
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al obtener mantenimiento")
            }
    }

    private fun MantenimientoModel.aEntity(estadoSync: String): MantenimientoEntity {
        return MantenimientoEntity(
            uid = uid,
            codigoMantenimiento = codigoMantenimiento,
            uidMaquinaria = uidMaquinaria,
            codigoMaquinaria = codigoMaquinaria,
            nombreMaquinaria = nombreMaquinaria,
            tipoMaquinaria = tipoMaquinaria,
            tipoMantenimiento = tipoMantenimiento,
            descripcion = descripcion,
            fechaProgramada = fechaProgramada,
            fechaRealizada = fechaRealizada,
            estado = estado,
            responsable = responsable,
            responsableUid = responsableUid,
            observaciones = observaciones,
            costoEstimado = costoEstimado,
            costoReal = costoReal,
            horometroProgramado = horometroProgramado,
            horometroReal = horometroReal,
            fechaRegistro = fechaRegistro,
            fechaActualizacion = fechaActualizacion,
            registradoPor = registradoPor,
            actualizadoPor = actualizadoPor,
            prioridad = prioridad,
            estadoSync = estadoSync
        )
    }

    private fun MantenimientoEntity.aModel(): MantenimientoModel {
        return MantenimientoModel(
            uid = uid,
            codigoMantenimiento = codigoMantenimiento,
            uidMaquinaria = uidMaquinaria,
            codigoMaquinaria = codigoMaquinaria,
            nombreMaquinaria = nombreMaquinaria,
            tipoMaquinaria = tipoMaquinaria,
            tipoMantenimiento = tipoMantenimiento,
            descripcion = descripcion,
            fechaProgramada = fechaProgramada,
            fechaRealizada = fechaRealizada,
            estado = estado,
            responsable = responsable,
            responsableUid = responsableUid,
            observaciones = observaciones,
            costoEstimado = costoEstimado,
            costoReal = costoReal,
            horometroProgramado = horometroProgramado,
            horometroReal = horometroReal,
            fechaRegistro = fechaRegistro,
            fechaActualizacion = fechaActualizacion,
            registradoPor = registradoPor,
            actualizadoPor = actualizadoPor,
            prioridad = prioridad
        )
    }
}