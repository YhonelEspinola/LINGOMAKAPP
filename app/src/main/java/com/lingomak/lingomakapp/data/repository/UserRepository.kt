package com.lingomak.lingomakapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.utils.Constants

class UserRepository {
    private val dataBase = FirebaseFirestore.getInstance()

    private val functions = FirebaseFunctions.getInstance()

    fun listarUsuarios(onSuccess:(List<UserModel>) -> Unit, onError:(String) -> Unit){
        dataBase.collection(Constants.USUARIOS)
            .addSnapshotListener { snapshots, error ->
                if(error != null) {
                    onError(error.message ?: "Error al listar usuarios")
                    return@addSnapshotListener
                }
                if (snapshots == null){
                    onSuccess(emptyList())
                    return@addSnapshotListener
                }
                val listaUsuarios = snapshots.documents.mapNotNull { document ->
                    document.toObject(UserModel::class.java)
                }
                onSuccess(listaUsuarios)
            }
    }

    fun listarOperarios(onSuccess: (List<UserModel>) -> Unit, onError: (String) -> Unit) {
        dataBase.collection(Constants.USUARIOS)
            .whereEqualTo("rol", "OPERARIO")
            .whereEqualTo("estado", "ACTIVO")
            .get()
            .addOnSuccessListener { result ->
                val lista = result.documents.mapNotNull { it.toObject(UserModel::class.java) }
                onSuccess(lista)
            }
            .addOnFailureListener {
                onError(it.message ?: "Error al listar operarios")
            }
    }

    fun crearUsuario(
        usuario: UserModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        dataBase.collection(Constants.USUARIOS)
            .document(usuario.uid)
            .set(usuario)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al crear usuario")
            }
    }

    fun cambiarEstadoUsuario(
        uid: String,
        nuevoEstado: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        dataBase.collection(Constants.USUARIOS)
            .document(uid)
            .update("estado", nuevoEstado)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al cambiar el estado del usuario")
            }
    }

    fun actualizarUsuario(
        uid: String,
        nombre : String,
        correo : String,
        rol: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        val datosActualizados = mapOf(
            "nombre" to nombre,
            "correo" to correo,
            "rol" to rol
        )

        dataBase.collection(Constants.USUARIOS)
            .document(uid)
            .update(datosActualizados)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al actualizar usuarios")
            }
    }

    fun crearUsuarioConFunction(
        nombre: String,
        correo: String,
        password: String,
        rol: String,
        creadoPor: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val usuarioActual = FirebaseAuth.getInstance().currentUser

        if (usuarioActual == null) {
            onError("No hay usuario autenticado en Firebase Auth")
            return
        }

        usuarioActual.getIdToken(true)
            .addOnSuccessListener {

                // Datos que enviaremos a la Cloud Function.
                val data = hashMapOf(
                    "nombre" to nombre,
                    "correo" to correo,
                    "password" to password,
                    "rol" to rol,
                    "creadoPor" to creadoPor,
                    "debeCambiarPassword" to true
                )

                FirebaseFunctions.getInstance("us-central1")
                    .getHttpsCallable("createUserAdmin")
                    .call(data)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        onError(exception.message ?: "Error al crear usuario")
                    }
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al obtener token del usuario")
            }
    }

}