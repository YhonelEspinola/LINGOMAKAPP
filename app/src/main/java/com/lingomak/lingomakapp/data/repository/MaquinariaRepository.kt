package com.lingomak.lingomakapp.data.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.MaquinariaEntity
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.worker.SincronizacionMaquinariaWorker
import com.lingomak.lingomakapp.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MaquinariaRepository(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val maquinariasCollection = db.collection("maquinarias")
    private val storageFolder = "maquinarias_imagenes"

    private val maquinariaDao = AppDatabase.getInstance(context).maquinariaDao()
    private val appContext = context.applicationContext

    // ===================================================================
    // LECTURA LOCAL (Room)
    // ===================================================================

    fun obtenerMaquinariasObservable(): LiveData<List<MaquinariaModel>> {
        return maquinariaDao.obtenerTodasObservable().map { entities ->
            entities.map { it.aModel() }
        }
    }

    fun obtenerMaquinariasActivasObservable(): LiveData<List<MaquinariaModel>> {
        return maquinariaDao.obtenerActivasObservable().map { entities ->
            entities.map { it.aModel() }
        }
    }

    fun obtenerMaquinariaPorUidObservable(uid: String): LiveData<MaquinariaModel?> {
        return maquinariaDao.obtenerPorUidObservable(uid).map { it?.aModel() }
    }

    suspend fun obtenerMaquinariaPorUid(uid: String): MaquinariaModel? {
        return maquinariaDao.obtenerPorUid(uid)?.aModel()
    }

    // ===================================================================
    // ESCRITURA LOCAL (Room)
    // ===================================================================

    suspend fun agregarMaquinaria(
        maquinaria: MaquinariaModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val entity = maquinaria.aEntity(
                estadoSync = "PENDIENTE_CREAR",
                timestampLocal = System.currentTimeMillis()
            )
            maquinariaDao.insertarOActualizar(entity)
            SincronizacionMaquinariaWorker.encolar(appContext)
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al agregar maquinaria localmente")
        }
    }

    suspend fun actualizarMaquinaria(
        maquinaria: MaquinariaModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val entity = maquinaria.aEntity(
                estadoSync = "PENDIENTE_ACTUALIZAR",
                timestampLocal = System.currentTimeMillis()
            )
            maquinariaDao.insertarOActualizar(entity)
            SincronizacionMaquinariaWorker.encolar(appContext)
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al actualizar maquinaria localmente")
        }
    }

    suspend fun cambiarEstadoMaquinaria(
        uid: String,
        nuevoEstado: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val fechaActual = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            maquinariaDao.cambiarEstadoLocal(
                uid = uid,
                nuevoEstado = nuevoEstado,
                fechaActualizacion = fechaActual,
                timestamp = System.currentTimeMillis()
            )
            SincronizacionMaquinariaWorker.encolar(appContext)
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al cambiar estado localmente")
        }
    }

    // ===================================================================
    // SINCRONIZACIÓN CON FIRESTORE
    // ===================================================================

    suspend fun sincronizarPendientesConFirestore() {
        val pendientes = maquinariaDao.obtenerPendientesDeSincronizar()
        for (entity in pendientes) {
            try {
                val docRemoto = maquinariasCollection.document(entity.uid).get().await()
                
                val fechaRemotaStr = docRemoto.getString("fechaActualizacion") ?: ""
                val format = if (fechaRemotaStr.contains(":")) 
                    java.text.SimpleDateFormat(DateUtils.FORMATO_PRECISO, java.util.Locale.getDefault())
                    else java.text.SimpleDateFormat(DateUtils.FORMATO_ESTANDAR, java.util.Locale.getDefault())
                
                val timestampRemoto = try { format.parse(fechaRemotaStr)?.time ?: 0L } catch(e: Exception) { 0L }

                // Si el remoto es estrictamente más nuevo (por fecha), no subimos el local (evita sobreescribir cambios externos)
                if (docRemoto.exists() && timestampRemoto > entity.timestampLocal) {
                    maquinariaDao.marcarComoSincronizado(entity.uid)
                    continue
                }

                maquinariasCollection.document(entity.uid).set(entity.aModel()).await()
                maquinariaDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) {
                // Reintento en la próxima corrida
            }
        }
    }

    suspend fun descargarMaquinariasDeFirestore() {
        try {
            val snapshot = maquinariasCollection.get().await()
            val remotos = snapshot.toObjects(MaquinariaModel::class.java)
            val uidsRemotos = remotos.map { it.uid }

            if (uidsRemotos.isEmpty()) {
                maquinariaDao.eliminarTodasSincronizadas()
            } else {
                for (modelo in remotos) {
                    val local = maquinariaDao.obtenerPorUid(modelo.uid)
                    
                    val format = if (modelo.fechaActualizacion.contains(":")) 
                        java.text.SimpleDateFormat(DateUtils.FORMATO_PRECISO, java.util.Locale.getDefault())
                        else java.text.SimpleDateFormat(DateUtils.FORMATO_ESTANDAR, java.util.Locale.getDefault())
                    
                    val timestampRemoto = try { format.parse(modelo.fechaActualizacion)?.time ?: 0L } catch(e: Exception) { 0L }
                    
                    if (local != null) {
                        // REGLA DE ORO: Si el local es más nuevo que el remoto, NO sobrescribir.
                        // Esto protege cambios recién hechos que aún no se reflejan en el GET de Firestore
                        if (local.timestampLocal >= timestampRemoto) {
                            continue
                        }
                    }

                    maquinariaDao.insertarOActualizar(modelo.aEntity("SINCRONIZADO", System.currentTimeMillis()))
                }
                maquinariaDao.eliminarSincronizadosNoPresentes(uidsRemotos)
            }
        } catch (e: Exception) {
            // Ignorar en offline
        }
    }

    fun iniciarEscuchaMaquinaria() {
        maquinariasCollection.addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener
            
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots.documentChanges) {
                    val modelo = doc.document.toObject(MaquinariaModel::class.java)
                    val local = maquinariaDao.obtenerPorUid(modelo.uid)
                    
                    val format = if (modelo.fechaActualizacion.contains(":")) 
                        java.text.SimpleDateFormat(DateUtils.FORMATO_PRECISO, java.util.Locale.getDefault())
                        else java.text.SimpleDateFormat(DateUtils.FORMATO_ESTANDAR, java.util.Locale.getDefault())
                    
                    val timestampRemoto = try { format.parse(modelo.fechaActualizacion)?.time ?: 0L } catch(ex: Exception) { 0L }
                    
                    if (local != null && local.estadoSync != "SINCRONIZADO") {
                        // Respetamos cambios locales pendientes si el remoto es más antiguo
                        if (local.timestampLocal >= timestampRemoto) {
                            continue
                        }
                    }
                    
                    if (doc.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        // Opcional: Podrías eliminarlo localmente si lo deseas
                        // maquinariaDao.eliminarSincronizadosNoPresentes(listOf()) // No muy eficiente
                    } else {
                        maquinariaDao.insertarOActualizar(modelo.aEntity("SINCRONIZADO", System.currentTimeMillis()))
                    }
                }
            }
        }
    }

    // ===================================================================
    // STORAGE
    // ===================================================================

    fun subirImagenMaquinaria(
        imagenUri: Uri,
        uid: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val referencia = storage.reference
            .child(storageFolder)
            .child("$uid.jpg")

        referencia.putFile(imagenUri)
            .addOnSuccessListener {
                referencia.downloadUrl
                    .addOnSuccessListener { uri ->
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
