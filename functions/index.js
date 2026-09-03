const {setGlobalOptions} = require("firebase-functions");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");

admin.initializeApp();

setGlobalOptions({maxInstances: 10});

exports.createUserAdmin = onCall(async (request) => {

  if (!request.auth) {
    throw new HttpsError(
        "unauthenticated",
        "Debe iniciar sesión para crear usuarios."
    );
  }


  const uidAdmin = request.auth.uid;


  const adminDoc = await admin.firestore()
      .collection("usuarios")
      .doc(uidAdmin)
      .get();


  if (!adminDoc.exists) {
    throw new HttpsError(
        "permission-denied",
        "El usuario no existe en Firestore."
    );
  }


  const adminData = adminDoc.data();


  if (adminData.rol !== "ADMIN") {
    throw new HttpsError(
        "permission-denied",
        "Solo un administrador puede crear usuarios."
    );
  }


  const nombre = request.data.nombre;
  const correo = request.data.correo;
  const password = request.data.password;
  const rol = request.data.rol;
  const creadoPor = adminData.nombre || "ADMIN";

  if (!nombre || !correo || !password || !rol) {
    throw new HttpsError(
        "invalid-argument",
        "Nombre, correo, contraseña y rol son obligatorios."
    );
  }

  try {

    const userRecord = await admin.auth().createUser({
      email: correo,
      password: password,
      displayName: nombre,
      disabled: false,
    });

    const uid = userRecord.uid;


    await admin.firestore().collection("usuarios").doc(uid).set({
      uid: uid,
      nombre: nombre,
      correo: correo,
      rol: rol,
      estado: "ACTIVO",
      debeCambiarPassword: true,
      fechaCreacion: new Date().toISOString().substring(0, 10),
      creadoPor: creadoPor,
    });

    return {
      success: true,
      uid: uid,
      message: "Usuario creado correctamente.",
    };
  } catch (error) {
    throw new HttpsError(
        "internal",
        error.message || "Error al crear usuario."
    );
  }
});

exports.notificarNuevaSolicitudMantenimiento = onDocumentCreated(
    {
      document: "solicitudes_mantenimiento/{solicitudId}",
      region: "us-central1",
    },
    async (event) => {
      const snapshot = event.data;

      if (!snapshot) {
        console.log("La solicitud no contiene información.");
        return;
      }

      const solicitud = snapshot.data();

      if (solicitud.estadoSolicitud !== "PENDIENTE_APROBACION") {
        return;
      }

      const nombreMaquinaria =
          solicitud.nombreMaquinaria || "maquinarias";

      const horasRestantes =
          Number(solicitud.horasRestantes ?? 0);

      let mensaje;

      if (horasRestantes < 0) {
        mensaje =
            `${nombreMaquinaria} superó el intervalo por ` +
            `${Math.abs(horasRestantes).toFixed(1)} horas.`;
      } else if (horasRestantes === 0) {
        mensaje =
            `${nombreMaquinaria} alcanzó el límite de mantenimiento.`;
      } else {
        mensaje =
            `${nombreMaquinaria} requiere revisión. ` +
            `Faltan ${horasRestantes.toFixed(1)} horas para mantenimiento.`;
      }

      const notificationMessage = {
        topic: "administradores",

        notification: {
          title: "Solicitud de mantenimiento pendiente",
          body: mensaje,
        },

        data: {
          tipo: "SOLICITUD_MANTENIMIENTO",
          uidSolicitud: event.params.solicitudId,
          uidMaquinaria: solicitud.uidMaquinaria || "",
        },

        android: {
          priority: "high",

          notification: {
            channelId: "alertas_lingomak",
            sound: "default",
          },
        },
      };

      try {
        const response =
            await admin.messaging().send(notificationMessage);

        console.log(
            "Notificación de solicitud enviada correctamente:",
            response
        );
      } catch (error) {
        console.error(
            "Error enviando notificación de solicitud:",
            error
        );

        throw error;
      }
    }
);