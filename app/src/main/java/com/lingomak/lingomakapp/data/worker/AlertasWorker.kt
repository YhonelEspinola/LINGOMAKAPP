package com.lingomak.lingomakapp.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lingomak.lingomakapp.data.service.AlertasService
import com.lingomak.lingomakapp.utils.NotificationHelper

class AlertasWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        return try {
            val alertasService = AlertasService(applicationContext)
            val listaAlertas = alertasService.obtenerAlertas()

            val totalCriticas = listaAlertas.count { it.prioridad == "ALTA" }
            val totalAdvertencias = listaAlertas.count { it.prioridad == "MEDIA" }

            if (totalCriticas == 0 && totalAdvertencias == 0) {
                return Result.success()
            }

            NotificationHelper.mostrarNotificacionAlerta(
                context = applicationContext,
                titulo = "Centro de Monitoreo LINGOMAK",
                mensaje = "Se detectaron $totalCriticas alertas críticas y $totalAdvertencias advertencias.",
                notificationId = 1001
            )

            Result.success()

        } catch (exception: Exception) {
            Result.retry()
        }
    }
}
