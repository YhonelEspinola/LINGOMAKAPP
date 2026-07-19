package com.lingomak.lingomakapp.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lingomak.lingomakapp.data.repository.RegistroUsoMaquinariaRepository

class SincronizacionRegistroUsoMaquinariaWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = RegistroUsoMaquinariaRepository(applicationContext)

            repository.sincronizarPendientesConFirestore()
            repository.descargarCambiosDeFirestore()

            Result.success()
        } catch (exception: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val NOMBRE_TRABAJO = "sincronizacion_uso_maquinaria"

        fun encolar(context: Context) {
            val restricciones = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val solicitud = OneTimeWorkRequestBuilder<SincronizacionRegistroUsoMaquinariaWorker>()
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
    }
}
