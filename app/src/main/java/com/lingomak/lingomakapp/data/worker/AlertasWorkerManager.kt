package com.lingomak.lingomakapp.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AlertasWorkerManager {

    private const val NOMBRE_TRABAJO_ALERTAS =
        "trabajo_periodico_alertas_lingomak"

    fun programarRevisionAlertas(context: Context){

        val trabajoAlertas =
            PeriodicWorkRequestBuilder<AlertasWorker>(
                15,
                TimeUnit.MINUTES
            ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOMBRE_TRABAJO_ALERTAS,
            ExistingPeriodicWorkPolicy.KEEP,
            trabajoAlertas
        )
    }

    fun ejecutarRevisionAhora(context: Context) {

        val trabajoPrueba =

            OneTimeWorkRequestBuilder<AlertasWorker>()
                .build()

        WorkManager
            .getInstance(context)
            .enqueue(trabajoPrueba)
    }

}