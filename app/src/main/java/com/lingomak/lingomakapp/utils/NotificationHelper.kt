package com.lingomak.lingomakapp.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lingomak.lingomakapp.R

object NotificationHelper {

    private const val CHANNEL_ID = "alertas_lingomak"
    private const val CHANNEL_NAME = "Alertas LINGOMAK"
    private const val CHANNEL_DESCRIPTION =
        "Notificaciones de mantenimiento, inventario y movimientos"

    fun crearCanalNotificaciones(context: Context) {

        // Android 8 o superior necesita un canal para mostrar notificaciones.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val canal = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(canal)
        }
    }

    fun mostrarNotificacionAlerta(
        context: Context,
        titulo: String,
        mensaje: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        crearCanalNotificaciones(context)

        // Android 13+ necesita permiso POST_NOTIFICATIONS.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificacion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(mensaje)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(notificationId, notificacion)
    }
}
