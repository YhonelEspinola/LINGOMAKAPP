package com.lingomak.lingomakapp.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.MaquinariaEntity
import com.lingomak.lingomakapp.data.local.entity.RegistroUsoMaquinariaEntity
import com.lingomak.lingomakapp.data.local.entity.SolicitudMantenimientoEntity
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.RegistroUsoMaquinariaModel
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel
import com.lingomak.lingomakapp.utils.DateUtils
import com.lingomak.lingomakapp.worker.SincronizacionMaquinariaWorker
import com.lingomak.lingomakapp.worker.SincronizacionRegistroUsoMaquinariaWorker
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RegistroUsoMaquinariaRepository(context: Context) {

    private val db = FirebaseFirestore.getInstance()

    private val coleccionMaquinaria = "maquinarias"
    private val coleccionRegistrosUso = "registros_uso_maquinaria"
    private val coleccionSolicitudes = "solicitudes_mantenimiento"

    private val database = AppDatabase.getInstance(context)
    private val registroUsoDao = database.registroUsoMaquinariaDao()
    private val maquinariaDao = database.maquinariaDao()
    private val solicitudDao = database.solicitudMantenimientoDao()
    private val appContext = context.applicationContext

    suspend fun registrarUsoMaquinaria(
        uidMaquinaria: String,
        uidOperario: String,
        nombreOperario: String,
        correoOperario: String,
        horasUso: Int,
        observacion: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val maqEntity = maquinariaDao.obtenerPorUid(uidMaquinaria)
                ?: throw Exception("No se encontró la maquinaria localmente")

            val horometroAnterior = maqEntity.horometroActual
            val horometroFinal = horometroAnterior + horasUso
            val horasDesdeUltimoMantenimiento = horometroFinal - maqEntity.horometroUltimoMantenimiento
            val horasRestantes = maqEntity.intervaloMantenimientoHoras - horasDesdeUltimoMantenimiento

            val registroUso = RegistroUsoMaquinariaEntity(
                uid = UUID.randomUUID().toString(),
                uidMaquinaria = maqEntity.uid,
                codigoMaquinaria = maqEntity.codigoMaquinaria,
                nombreMaquinaria = maqEntity.nombre,
                tipoMaquinaria = maqEntity.tipo,
                uidOperario = uidOperario,
                nombreOperario = nombreOperario,
                correoOperario = correoOperario,
                fechaUso = DateUtils.obtenerFechaActual(),
                horometroAnterior = horometroAnterior,
                horasUso = horasUso,
                horometroFinal = horometroFinal,
                observacion = observacion,
                fechaRegistro = DateUtils.obtenerFechaActual(),
                estadoSync = "PENDIENTE_CREAR",
                timestampLocal = System.currentTimeMillis()
            )

            val maqActualizada = maqEntity.copy(
                horometroActual = horometroFinal,
                fechaActualizacion = DateUtils.obtenerFechaActual(),
                estadoSync = "PENDIENTE_ACTUALIZAR",
                timestampLocal = System.currentTimeMillis()
            )

            var solicitudEntity: SolicitudMantenimientoEntity? = null

            if (horasRestantes <= 20) {
                val solicitudExistente = solicitudDao.obtenerPendientePorMaquinaria(uidMaquinaria)
                
                if (solicitudExistente == null) {
                    solicitudEntity = SolicitudMantenimientoEntity(
                        uid = UUID.randomUUID().toString(),
                        uidMaquinaria = maqEntity.uid,
                        codigoMaquinaria = maqEntity.codigoMaquinaria,
                        nombreMaquinaria = maqEntity.nombre,
                        tipoMaquinaria = maqEntity.tipo,
                        uidOperario = uidOperario,
                        nombreOperario = nombreOperario,
                        correoOperario = correoOperario,
                        horometroActual = horometroFinal,
                        horometroUltimoMantenimiento = maqEntity.horometroUltimoMantenimiento,
                        intervaloMantenimientoHoras = 250, // Asumido por el modelo original
                        horasDesdeUltimoMantenimiento = horasDesdeUltimoMantenimiento,
                        horasRestantes = horasRestantes,
                        motivo = obtenerMotivoSolicitud(horasRestantes),
                        estadoSolicitud = "PENDIENTE_APROBACION",
                        origen = "HOROMETRO_OPERARIO",
                        fechaSugerida = DateUtils.obtenerFechaActual(),
                        fechaRegistro = DateUtils.obtenerFechaActual(),
                        estadoSync = "PENDIENTE_CREAR",
                        timestampLocal = System.currentTimeMillis()
                    )
                } else {
                    solicitudEntity = solicitudExistente.copy(
                        horometroActual = horometroFinal,
                        horasDesdeUltimoMantenimiento = horasDesdeUltimoMantenimiento,
                        horasRestantes = horasRestantes,
                        motivo = obtenerMotivoSolicitud(horasRestantes),
                        fechaSugerida = DateUtils.obtenerFechaActual(),
                        estadoSync = "PENDIENTE_ACTUALIZAR",
                        timestampLocal = System.currentTimeMillis()
                    )
                }
            }

            // Escritura atómica en Room
            registroUsoDao.registrarUsoMaquinariaLocal(registroUso, maqActualizada, solicitudEntity)

            // Encolar sincronización
            SincronizacionRegistroUsoMaquinariaWorker.encolar(appContext)
            SincronizacionMaquinariaWorker.encolar(appContext)
            // No creamos worker separado para solicitudes, el de uso puede encargarse de sincronizarlas si queremos,
            // pero para ser consistentes, vamos a añadir el método de sincronización de solicitudes en este repositorio o en uno nuevo.

            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al registrar uso localmente")
        }
    }

    // ===================================================================
    // SINCRONIZACIÓN CON FIRESTORE
    // ===================================================================

    suspend fun sincronizarPendientesConFirestore() {
        // 1. Sincronizar Registros de Uso
        val pendientesUso = registroUsoDao.obtenerPendientesDeSincronizar()
        for (entity in pendientesUso) {
            try {
                // Los registros de uso suelen ser inmutables (solo creación), pero validamos por si acaso
                db.collection(coleccionRegistrosUso).document(entity.uid).set(entity.aModel()).await()
                registroUsoDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) { /* Reintento */ }
        }

        // 2. Sincronizar Solicitudes (creadas/actualizadas en este flujo)
        val pendientesSolicitud = solicitudDao.obtenerPendientesDeSincronizar()
        for (entity in pendientesSolicitud) {
            try {
                val docRemoto = db.collection(coleccionSolicitudes).document(entity.uid).get().await()
                val fechaRemotaStr = docRemoto.getString("fechaActualizacion") ?: docRemoto.getString("fechaRegistro") ?: ""
                // Lógica simplificada de timestamp para solicitudes
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val timestampRemoto = try { format.parse(fechaRemotaStr)?.time ?: 0L } catch(e: Exception) { 0L }

                if (docRemoto.exists() && timestampRemoto > entity.timestampLocal) {
                    solicitudDao.marcarComoSincronizado(entity.uid)
                    continue
                }

                db.collection(coleccionSolicitudes).document(entity.uid).set(entity.aModel()).await()
                solicitudDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) { /* Reintento */ }
        }
    }

    suspend fun descargarCambiosDeFirestore() {
        // En este módulo, el operario raramente necesita descargar el histórico de usos.
        // Pero podríamos descargar las solicitudes pendientes para que la UI local sepa si hay una activa.
        try {
            val snapshot = db.collection(coleccionSolicitudes)
                .whereEqualTo("estadoSolicitud", "PENDIENTE_APROBACION")
                .get().await()
            
            val remotos = snapshot.toObjects(SolicitudMantenimientoModel::class.java)
            for (modelo in remotos) {
                val local = solicitudDao.obtenerPorUid(modelo.uid)
                if (local != null && local.estadoSync != "SINCRONIZADO") continue
                
                solicitudDao.insertarOActualizar(modelo.aEntity("SINCRONIZADO", System.currentTimeMillis()))
            }
        } catch (e: Exception) { /* Offline */ }
    }

    private fun obtenerMotivoSolicitud(horasRestantes: Int): String {
        return when {
            horasRestantes < 0 -> {
                val horasExcedidas = kotlin.math.abs(horasRestantes)
                "La maquinaria superó el intervalo de mantenimiento por $horasExcedidas horas."
            }
            horasRestantes == 0 -> "La maquinaria alcanzó exactamente el intervalo de mantenimiento preventivo."
            else -> "La maquinaria está próxima a cumplir el intervalo de mantenimiento. Faltan $horasRestantes horas."
        }
    }

    // =======================================================================
    // MAPPERS
    // =======================================================================

    private fun RegistroUsoMaquinariaEntity.aModel() = RegistroUsoMaquinariaModel(
        uid = uid, uidMaquinaria = uidMaquinaria, codigoMaquinaria = codigoMaquinaria,
        nombreMaquinaria = nombreMaquinaria, tipoMaquinaria = tipoMaquinaria,
        uidOperario = uidOperario, nombreOperario = nombreOperario, correoOperario = correoOperario,
        fechaUso = fechaUso, horometroAnterior = horometroAnterior, horasUso = horasUso,
        horometroFinal = horometroFinal, observacion = observacion, fechaRegistro = fechaRegistro
    )

    private fun SolicitudMantenimientoEntity.aModel() = SolicitudMantenimientoModel(
        uid = uid, uidMaquinaria = uidMaquinaria, codigoMaquinaria = codigoMaquinaria,
        nombreMaquinaria = nombreMaquinaria, tipoMaquinaria = tipoMaquinaria,
        uidOperario = uidOperario, nombreOperario = nombreOperario, correoOperario = correoOperario,
        horometroActual = horometroActual, horometroUltimoMantenimiento = horometroUltimoMantenimiento,
        intervaloMantenimientoHoras = intervaloMantenimientoHoras,
        horasDesdeUltimoMantenimiento = horasDesdeUltimoMantenimiento, horasRestantes = horasRestantes,
        motivo = motivo, estadoSolicitud = estadoSolicitud, origen = origen,
        fechaSugerida = fechaSugerida, fechaRegistro = fechaRegistro,
        revisadoPor = revisadoPor, fechaRevision = fechaRevision,
        motivoRechazo = motivoRechazo, uidMantenimientoGenerado = uidMantenimientoGenerado
    )

    private fun SolicitudMantenimientoModel.aEntity(estadoSync: String, timestampLocal: Long) = SolicitudMantenimientoEntity(
        uid = uid, uidMaquinaria = uidMaquinaria, codigoMaquinaria = codigoMaquinaria,
        nombreMaquinaria = nombreMaquinaria, tipoMaquinaria = tipoMaquinaria,
        uidOperario = uidOperario, nombreOperario = nombreOperario, correoOperario = correoOperario,
        horometroActual = horometroActual, horometroUltimoMantenimiento = horometroUltimoMantenimiento,
        intervaloMantenimientoHoras = intervaloMantenimientoHoras,
        horasDesdeUltimoMantenimiento = horasDesdeUltimoMantenimiento, horasRestantes = horasRestantes,
        motivo = motivo, estadoSolicitud = estadoSolicitud, origen = origen,
        fechaSugerida = fechaSugerida, fechaRegistro = fechaRegistro,
        revisadoPor = revisadoPor, fechaRevision = fechaRevision,
        motivoRechazo = motivoRechazo, uidMantenimientoGenerado = uidMantenimientoGenerado,
        estadoSync = estadoSync, timestampLocal = timestampLocal
    )
}
