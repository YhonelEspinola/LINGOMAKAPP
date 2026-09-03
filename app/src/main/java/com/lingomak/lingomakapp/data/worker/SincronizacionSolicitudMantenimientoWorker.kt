package com.lingomak.lingomakapp.data.worker

import android.content.Context
import androidx.work.*
import com.lingomak.lingomakapp.data.repository.SolicitudMantenimientoRepository
import java.util.concurrent.TimeUnit

class SincronizacionSolicitudMantenimientoWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = SolicitudMantenimientoRepository(applicationContext)
            repository.sincronizarPendientesConFirestore()
            repository.descargarSolicitudesDeFirestore()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val NOMBRE_TRABAJO = "sincronizacion_solicitudes"

        fun encolar(context: Context) {
            val restricciones = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val solicitud = OneTimeWorkRequestBuilder<SincronizacionSolicitudMantenimientoWorker>()
                .setConstraints(restricciones)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(NOMBRE_TRABAJO, ExistingWorkPolicy.KEEP, solicitud)
        }
    }
}
