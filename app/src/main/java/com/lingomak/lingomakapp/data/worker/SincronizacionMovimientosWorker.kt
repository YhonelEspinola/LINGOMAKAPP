package com.lingomak.lingomakapp.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.lingomak.lingomakapp.data.repository.MovimientoRepository
import java.util.concurrent.TimeUnit

/**
 * Worker responsable de subir a Firestore los movimientos registrados
 * localmente (Room) mientras no había conexión. Al ser append-only,
 * este Worker solo necesita "subir lo pendiente" — no hay descarga
 * automática de cambios remotos aquí, ya que esa la dispara
 * MovimientosFragment puntualmente vía descargarMovimientosDeFirestore
 * al abrir el historial de un repuesto específico (no tiene sentido
 * descargar TODOS los movimientos de TODOS los repuestos en segundo
 * plano, sería un costo de lectura innecesario en Firestore).
 */
class SincronizacionMovimientosWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = MovimientoRepository(applicationContext)
            repository.sincronizarPendientesConFirestore()
            Result.success()
        } catch (exception: Exception) {
            Result.retry()
        }
    }

    companion object {

        private const val NOMBRE_TRABAJO = "sincronizacion_movimientos"

        fun encolar(context: Context) {
            val restricciones = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val solicitud = OneTimeWorkRequestBuilder<SincronizacionMovimientosWorker>()
                .setConstraints(restricciones)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(NOMBRE_TRABAJO, ExistingWorkPolicy.KEEP, solicitud)
        }
    }
}
