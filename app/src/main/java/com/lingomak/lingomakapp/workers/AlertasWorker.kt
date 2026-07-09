package com.lingomak.lingomakapp.workers

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

            /*
             * El Worker se ejecuta en segundo plano.
             * Aquí ya no mostramos una notificación de prueba.
             * Ahora consultamos las alertas reales usando AlertasService.
             */
            val alertasService = AlertasService(applicationContext)

            val listaAlertas =
                alertasService.obtenerAlertas()

            /*
             * Contamos alertas críticas.
             * Prioridad ALTA = problemas urgentes.
             */
            val totalCriticas =
                listaAlertas.count { alerta ->
                    alerta.prioridad == "ALTA"
                }

            /*
             * Contamos advertencias.
             * Prioridad MEDIA = situaciones que requieren atención.
             */
            val totalAdvertencias =
                listaAlertas.count { alerta ->
                    alerta.prioridad == "MEDIA"
                }

            /*
             * Si no hay alertas importantes,
             * no mostramos notificación.
             */
            if (totalCriticas == 0 && totalAdvertencias == 0) {
                return Result.success()
            }

            /*
             * Mostramos una sola notificación resumen.
             * Usamos el mismo notificationId para evitar llenar
             * la barra de notificaciones.
             */
            NotificationHelper.mostrarNotificacionAlerta(
                context = applicationContext,
                titulo = "Centro de Monitoreo LINGOMAK",
                mensaje =
                    """
                    Se detectaron:
                    
                    🔴 $totalCriticas alertas críticas
                    🟠 $totalAdvertencias advertencias
                    
                    Revise el Centro de Monitoreo para más información.
                    """.trimIndent(),
                notificationId = 1001
            )

            /*
             * Indicamos a WorkManager que el trabajo terminó bien.
             */
            Result.success()

        } catch (exception: Exception) {

            /*
             * Si ocurre un error temporal, pedimos reintento.
             * Ejemplo: error de red al consultar Firestore.
             */
            Result.retry()
        }
    }
}