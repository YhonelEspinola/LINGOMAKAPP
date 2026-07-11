package com.lingomak.lingomakapp.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity

object NotificationHelper {

    /*
     * ID del canal.
     *
     * Debe coincidir con el channelId enviado
     * desde la Cloud Function.
     */
    private const val CHANNEL_ID = "alertas_lingomak"

    private const val CHANNEL_NAME =
        "Alertas LINGOMAK"

    private const val CHANNEL_DESCRIPTION =
        "Notificaciones de mantenimiento, inventario y movimientos"

    /**
     * Crea el canal utilizado por las notificaciones.
     *
     * Android 8 o superior exige que cada notificación
     * pertenezca a un NotificationChannel.
     */
    fun crearCanalNotificaciones(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val canal = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }

            val notificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            notificationManager.createNotificationChannel(canal)
        }
    }

    /**
     * Muestra una notificación local.
     *
     * Esta función es utilizada cuando el mensaje FCM
     * llega mientras la aplicación está en primer plano.
     */
    fun mostrarNotificacionAlerta(
        context: Context,
        titulo: String,
        mensaje: String,
        notificationId: Int =
            System.currentTimeMillis().toInt(),
        tipoDestino: String = "",
        uidSolicitud: String = ""
    ) {

        crearCanalNotificaciones(context)

        /*
         * Android 13 o superior necesita el permiso
         * POST_NOTIFICATIONS.
         */
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        /*
         * Este Intent se ejecutará cuando el administrador
         * pulse la notificación.
         */
        val intentDestino = Intent(
            context,
            DashboardAdminActivity::class.java
        ).apply {

            /*
             * CLEAR_TOP:
             * Si el Dashboard ya existe, elimina las pantallas
             * que estén encima.
             *
             * SINGLE_TOP:
             * Si el Dashboard ya está arriba, reutiliza esa
             * instancia y ejecuta onNewIntent().
             */
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            /*
             * Indicamos qué pantalla debe abrirse.
             */
            putExtra("tipoDestino", tipoDestino)

            /*
             * Conservamos el UID para usarlo posteriormente,
             * por ejemplo para resaltar la solicitud concreta.
             */
            putExtra("uidSolicitud", uidSolicitud)
        }

        /*
         * RequestCode diferente por solicitud.
         *
         * Esto evita que los PendingIntent de diferentes
         * notificaciones se reemplacen accidentalmente.
         */
        val requestCode =
            uidSolicitud
                .takeIf { it.isNotBlank() }
                ?.hashCode()
                ?: notificationId

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intentDestino,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(mensaje)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                    NotificationCompat.CATEGORY_REMINDER
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(notificationId, notificacion)
    }
}