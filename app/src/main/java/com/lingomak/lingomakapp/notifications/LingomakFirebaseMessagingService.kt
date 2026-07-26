package com.lingomak.lingomakapp.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lingomak.lingomakapp.utils.NotificationHelper

class LingomakFirebaseMessagingService :
    FirebaseMessagingService() {

    /**
     * Se ejecuta cuando el mensaje llega mientras
     * la aplicación está en primer plano.
     *
     * Cuando contiene notification + data y la app
     * está en segundo plano, Android muestra la
     * notificación automáticamente.
     */
    override fun onMessageReceived(
        message: RemoteMessage
    ) {
        super.onMessageReceived(message)

        val titulo =
            message.notification?.title
                ?: "Centro de Monitoreo LINGOMAK"

        val contenido =
            message.notification?.body
                ?: "Existe una nueva solicitud pendiente."


        val tipoDestino =
            message.data["tipo"].orEmpty()

        val uidSolicitud =
            message.data["uidSolicitud"].orEmpty()


        val notificationId =
            if (uidSolicitud.isNotBlank()) {
                uidSolicitud.hashCode()
            } else {
                System.currentTimeMillis().toInt()
            }

        NotificationHelper.mostrarNotificacionAlerta(
            context = applicationContext,
            titulo = titulo,
            mensaje = contenido,
            notificationId = notificationId,
            tipoDestino = tipoDestino,
            uidSolicitud = uidSolicitud
        )
    }

    /**
     * Firebase puede renovar el token del dispositivo.
     *
     * Como actualmente utilizamos un topic, no necesitamos
     * guardar manualmente el token en Firestore.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}