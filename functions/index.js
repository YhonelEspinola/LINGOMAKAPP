const {setGlobalOptions} = require("firebase-functions");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

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