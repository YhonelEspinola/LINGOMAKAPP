package com.lingomak.lingomakapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.ui.auth.LoginActivity

class LingomakFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Manejar mensajes cuando la app está en primer plano o tiene datos
        remoteMessage.notification?.let {
            mostrarNotificacion(it.title ?: "Alerta Lingomak", it.body ?: "")
        }
    }

    override fun onNewToken(token: String) {
        // Guardar el token en Firestore asociado al usuario para enviarle mensajes directos
        // Por ahora lo imprimimos, luego lo vincularemos al Repositorio de Usuarios
        println("FCM Token: $token")
    }

    private fun mostrarNotificacion(titulo: String, mensaje: String) {
        val channelId = "alertas_criticas"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas Críticas de Maquinaria",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
