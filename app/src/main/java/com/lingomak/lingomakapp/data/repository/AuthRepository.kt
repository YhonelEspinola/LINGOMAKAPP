package com.lingomak.lingomakapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.utils.Constants

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseFirestore.getInstance()

    fun login(correo: String, password: String, onSuccess:(UserModel) -> Unit,onError: (String) -> Unit){
        auth.signInWithEmailAndPassword(correo,password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid

                if(uid != null){
                    obtenerUsuario(uid,onSuccess,onError)
                }else{
                    onError("No se pudo obtener el ID del usuario")
                }

            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al iniciar sesión")
            }
    }

    fun verificarSesionActiva(onSuccess: (UserModel) -> Unit, onError: (String) -> Unit){
        val usuarioActual = auth.currentUser

        if(usuarioActual == null){
            onError("No hay sesion activa")
            return
        }

        val uid = usuarioActual.uid

        obtenerUsuario(uid = uid, onSuccess = onSuccess, onError = onError)
    }

    private fun obtenerUsuario(uid: String, onSuccess: (UserModel) -> Unit, onError: (String) -> Unit){
        database.collection(Constants.USUARIOS)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if(document.exists()){
                    val usuario = document.toObject(UserModel::class.java)

                    if(usuario != null){
                        onSuccess(usuario)
                    }else{
                        onError("No se pudo convertir la información del usuario")
                    }
                }else{
                    onError("El usuario no existe en Firestore")
                }
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al obtener datos del usuario")
            }
    }

    fun cerrarSesion(){
        auth.signOut()
    }
}