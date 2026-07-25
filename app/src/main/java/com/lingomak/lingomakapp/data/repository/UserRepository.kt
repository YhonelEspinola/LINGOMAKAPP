package com.lingomak.lingomakapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.aEntity
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserRepository(context: Context) {
    private val dataBase = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    
    private val database = AppDatabase.getInstance(context)
    private val userDao = database.userDao()

    // ===================================================================
    // LECTURA LOCAL (Room)
    // ===================================================================

    fun obtenerUsuariosObservable(): LiveData<List<UserModel>> {
        return userDao.obtenerTodosObservable().map { entities ->
            entities.map { it.aModel() }
        }
    }

    fun obtenerOperariosActivosObservable(): LiveData<List<UserModel>> {
        return userDao.obtenerOperariosActivosObservable().map { entities ->
            entities.map { it.aModel() }
        }
    }

    // ===================================================================
    // SINCRONIZACIÓN
    // ===================================================================

    /**
     * Descarga todos los usuarios de Firestore y actualiza Room.
     */
    suspend fun descargarUsuariosDeFirestore() {
        try {
            val snapshot = dataBase.collection(Constants.USUARIOS).get().await()
            val remotos = snapshot.toObjects(UserModel::class.java)
            val uidsRemotos = remotos.map { it.uid }

            if (uidsRemotos.isEmpty()) {
                userDao.eliminarSincronizados()
            } else {
                val entities = remotos.map { it.aEntity(estadoSync = "SINCRONIZADO") }
                userDao.insertarLista(entities)
                userDao.eliminarSincronizadosNoPresentes(uidsRemotos)
            }
        } catch (e: Exception) {
            // Silencioso en offline
        }
    }

    /**
     * Escucha cambios en tiempo real y los vuelca a Room.
     */
    fun iniciarEscuchaUsuarios() {
        dataBase.collection(Constants.USUARIOS)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                
                val lista = snapshots.documents.mapNotNull { it.toObject(UserModel::class.java) }
                val entities = lista.map { it.aEntity(estadoSync = "SINCRONIZADO") }
                
                CoroutineScope(Dispatchers.IO).launch {
                    userDao.insertarLista(entities)
                    userDao.eliminarSincronizadosNoPresentes(lista.map { it.uid })
                }
            }
    }

    // ===================================================================
    // ESCRITURA (Remota -> Room se actualiza vía SnapshotListener o manual)
    // ===================================================================

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
                val data = hashMapOf(
                    "nombre" to nombre,
                    "correo" to correo,
                    "password" to password,
                    "rol" to rol,
                    "creadoPor" to creadoPor,
                    "debeCambiarPassword" to true
                )

                functions.getHttpsCallable("createUserAdmin")
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

    // Legacy (mantener para compatibilidad si es necesario, pero migrar a Room observable)
    fun listarUsuarios(onSuccess:(List<UserModel>) -> Unit, onError:(String) -> Unit){
        dataBase.collection(Constants.USUARIOS)
            .get()
            .addOnSuccessListener { snapshots ->
                val listaUsuarios = snapshots.documents.mapNotNull { it.toObject(UserModel::class.java) }
                onSuccess(listaUsuarios)
            }
            .addOnFailureListener { onError(it.message ?: "Error") }
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
}
