package com.lingomak.lingomakapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle. map
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.aEntity
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel
import com.lingomak.lingomakapp.data.worker.SincronizacionSolicitudMantenimientoWorker
import com.lingomak.lingomakapp.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SolicitudMantenimientoRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val coleccionSolicitudes = "solicitudes_mantenimiento"
    private val database = AppDatabase.getInstance(context)
    private val solicitudDao = database.solicitudMantenimientoDao()

    /**
     * Lista en vivo (Room) de solicitudes pendientes de aprobación, para la
     * pantalla de administración. Se alimenta de lo que ya sincroniza
     * SincronizacionSolicitudMantenimientoWorker en segundo plano.
     */
    fun listarPendientesObservable(): LiveData<List<SolicitudMantenimientoModel>> {
        return solicitudDao.obtenerPendientesObservable().map { lista ->
            lista.map { it.aModel() }
        }
    }

    suspend fun rechazarSolicitud(
        uidSolicitud: String,
        uidAdministrador: String,
        motivoRechazo: String
    ) {
        val existente = solicitudDao.obtenerPorUid(uidSolicitud)
            ?: throw Exception("No se encontró la solicitud")

        val actualizada = existente.copy(
            estadoSolicitud = "RECHAZADA",
            revisadoPor = uidAdministrador,
            fechaRevision = DateUtils.obtenerFechaActual(),
            motivoRechazo = motivoRechazo,
            estadoSync = "PENDIENTE_ACTUALIZAR",
            timestampLocal = System.currentTimeMillis()
        )

        solicitudDao.insertarOActualizar(actualizada)
        SincronizacionSolicitudMantenimientoWorker.encolar(context)
    }

    suspend fun marcarComoConvertida(
        uidSolicitud: String,
        uidAdministrador: String,
        uidMantenimientoGenerado: String
    ) {
        val existente = solicitudDao.obtenerPorUid(uidSolicitud)
            ?: throw Exception("No se encontró la solicitud")

        val actualizada = existente.copy(
            estadoSolicitud = "CONVERTIDA_A_MANTENIMIENTO",
            revisadoPor = uidAdministrador,
            fechaRevision = DateUtils.obtenerFechaActual(),
            motivoRechazo = "",
            uidMantenimientoGenerado = uidMantenimientoGenerado,
            estadoSync = "PENDIENTE_ACTUALIZAR",
            timestampLocal = System.currentTimeMillis()
        )

        solicitudDao.insertarOActualizar(actualizada)
        SincronizacionSolicitudMantenimientoWorker.encolar(context)
    }

    suspend fun sincronizarPendientesConFirestore() {
        val pendientes = solicitudDao.obtenerPendientesDeSincronizar()
        for (entity in pendientes) {
            try {
                db.collection(coleccionSolicitudes).document(entity.uid).set(entity.aModel()).await()
                solicitudDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) { }
        }
    }

    suspend fun descargarSolicitudesDeFirestore() {
        try {
            val snapshot = db.collection(coleccionSolicitudes).get().await()
            val remotos = snapshot.toObjects(SolicitudMantenimientoModel::class.java)

            // Por simplicidad, descargamos y actualizamos localmente si no son sincronizados.
            // Para solicitudes, usualmente el Admin las gestiona.
            for (modelo in remotos) {
                solicitudDao.insertarOActualizar(modelo.aEntity("SINCRONIZADO", System.currentTimeMillis()))
            }
        } catch (e: Exception) { }
    }

    fun iniciarEscuchaSolicitudes() {
        db.collection(coleccionSolicitudes).addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener
            
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots.documentChanges) {
                    val modelo = doc.document.toObject(SolicitudMantenimientoModel::class.java)
                    solicitudDao.insertarOActualizar(modelo.aEntity("SINCRONIZADO", System.currentTimeMillis()))
                }
            }
        }
    }
}
