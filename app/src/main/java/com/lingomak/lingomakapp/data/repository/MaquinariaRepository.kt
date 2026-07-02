package com.lingomak.lingomakapp.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lingomak.lingomakapp.data.model.MaquinariaModel

class MaquinariaRepository {

    private val database = FirebaseFirestore.getInstance()

    private val colectionMaquinarias = "maquinarias"

    private val storage = FirebaseStorage.getInstance()

    fun listarMaquinarias(onSuccess: (List<MaquinariaModel>) -> Unit, onError: (String) -> Unit){

        database.collection(colectionMaquinarias)
            .addSnapshotListener { snapshots, error ->

                if(error != null){
                    onError(error.message ?: "Error al listar maquinas")
                    return@addSnapshotListener
                }

                if(snapshots == null ){
                    onSuccess(emptyList())
                    return@addSnapshotListener
                }
                val lista = snapshots.documents.mapNotNull { document ->
                    document.toObject(MaquinariaModel::class.java)
                }
                onSuccess(lista)
            }
    }

    fun agregarMaquinaria(
        maquinaria : MaquinariaModel,
        onSuccess: () -> Unit,
        onError : (String) -> Unit
    ) {
        database.collection(colectionMaquinarias)
            .document(maquinaria.uid)
            .set(maquinaria)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al agregar maquinaria")
            }
    }

    fun actualizarMaquinaria(
        maquinaria : MaquinariaModel,
        onSuccess: () -> Unit,
        onError : (String) -> Unit
    ){
        database.collection(colectionMaquinarias)
            .document(maquinaria.uid)
            .set(maquinaria)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al actualizar maquinaria")
            }
    }

    fun cambiarEstadoMaquinaria(
        uid: String,
        nuevoEstado: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        database.collection(colectionMaquinarias)
            .document(uid)
            .update("estado", nuevoEstado)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al cambiar estado de maquinaria")
            }
    }

    fun subirImagenMaquinaria(
        imagenUri: Uri,
        uid: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Ruta donde se guardará la imagen en Firebase Storage.
        val referencia = storage.reference
            .child("maquinarias")
            .child("$uid.jpg")

        referencia.putFile(imagenUri)
            .addOnSuccessListener {
                referencia.downloadUrl
                    .addOnSuccessListener { uri ->
                        // Retornamos la URL pública para guardarla en Firestore.
                        onSuccess(uri.toString())
                    }
                    .addOnFailureListener { exception ->
                        onError(exception.message ?: "Error al obtener URL de imagen")
                    }
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al subir imagen")
            }
    }


}