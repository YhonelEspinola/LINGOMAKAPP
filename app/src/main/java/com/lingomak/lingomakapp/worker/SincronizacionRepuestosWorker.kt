package com.lingomak.lingomakapp.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lingomak.lingomakapp.data.repository.RepuestoRepository

/**
 * Worker responsable de sincronizar el módulo de Repuestos entre
 * Room (local) y Cloud Firestore (remoto), solo cuando hay conexión
 * a internet disponible (ver Constraints en encolar()).
 *
 * Flujo en cada ejecución:
 * 1. Sube los repuestos marcados como pendientes en Room a Firestore
 *    (resolviendo conflictos con last-write-wins).
 * 2. Descarga cambios remotos recientes y los mezcla en Room.
 *
 * Se encola automáticamente cada vez que el repositorio hace una
 * escritura local (ver RepuestoRepository.guardarRepuesto, etc.),
 * y WorkManager se encarga de ejecutarlo apenas haya red, incluso si
 * la app está cerrada.
 */
class SincronizacionRepuestosWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = RepuestoRepository(applicationContext)

            repository.sincronizarPendientesConFirestore()
            repository.descargarCambiosDeFirestore()

            Result.success()
        } catch (exception: Exception) {
            // Reintenta con backoff exponencial (configurado al encolar)
            // en vez de fallar definitivamente — típico en fallos de red.
            Result.retry()
        }
    }

    companion object {

        private const val NOMBRE_TRABAJO = "sincronizacion_repuestos"

        /**
         * Encola el trabajo de sincronización. Usa ExistingWorkPolicy.KEEP
         * para evitar encolar múltiples sincronizaciones simultáneas si
         * el usuario hace varias escrituras seguidas: una sola corrida
         * pendiente es suficiente, ya que sincronizarPendientesConFirestore()
         * recoge TODO lo pendiente en ese momento, no solo el último cambio.
         */
        fun encolar(context: Context) {
            val restricciones = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val solicitud = OneTimeWorkRequestBuilder<SincronizacionRepuestosWorker>()
                .setConstraints(restricciones)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(NOMBRE_TRABAJO, ExistingWorkPolicy.KEEP, solicitud)
        }

        /**
         * Fuerza una sincronización inmediata (ej. al abrir la app o al
         * hacer pull-to-refresh), reemplazando cualquier trabajo en cola.
         */
        fun sincronizarAhora(context: Context) {
            val restricciones = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val solicitud = OneTimeWorkRequestBuilder<SincronizacionRepuestosWorker>()
                .setConstraints(restricciones)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(NOMBRE_TRABAJO, ExistingWorkPolicy.REPLACE, solicitud)
        }
    }
}
