package com.lingomak.lingomakapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.CategoriaEntity
import com.lingomak.lingomakapp.data.local.entity.aEntity
import com.lingomak.lingomakapp.data.model.CategoriaModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ConfiguracionRepository(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val coleccionCategorias = "categorias"
    
    private val database = AppDatabase.getInstance(context)
    private val categoriaDao = database.categoriaDao()

    // ===================================================================
    // LECTURA LOCAL
    // ===================================================================

    fun obtenerCategoriasPorTipoObservable(tipo: String, soloActivas: Boolean = true): LiveData<List<CategoriaModel>> {
        val liveData = if (soloActivas) categoriaDao.obtenerActivasPorTipoObservable(tipo) 
                       else categoriaDao.obtenerTodasPorTipoObservable(tipo)
        return liveData.map { entities -> entities.map { it.aModel() } }
    }

    // ===================================================================
    // ESCRITURA (Remota directa para Admin)
    // ===================================================================

    fun guardarCategoria(categoria: CategoriaModel, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = if (categoria.uid.isBlank()) UUID.randomUUID().toString() else categoria.uid
        val model = categoria.copy(uid = uid)
        
        db.collection(coleccionCategorias).document(uid).set(model)
            .addOnSuccessListener {
                CoroutineScope(Dispatchers.IO).launch {
                    categoriaDao.insertarOActualizar(model.aEntity(estadoSync = "SINCRONIZADO"))
                }
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Error al guardar") }
    }

    fun eliminarCategoria(uid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection(coleccionCategorias).document(uid).delete()
            .addOnSuccessListener {
                CoroutineScope(Dispatchers.IO).launch {
                    // Por ahora eliminamos físicamente de Room también. 
                    // En una versión avanzada haríamos borrado lógico.
                    val entity = categoriaDao.obtenerPorUid(uid)
                    if (entity != null) {
                        // Room no tiene DELETE por UID directo sin @Delete objeto, 
                        // pero podemos disparar un refresco de sincronización.
                        descargarCategoriasDeFirestore()
                    }
                    onSuccess()
                }
            }
            .addOnFailureListener { onError(it.message ?: "Error al eliminar") }
    }

    // ===================================================================
    // SINCRONIZACIÓN
    // ===================================================================

    suspend fun descargarCategoriasDeFirestore() {
        try {
            val snapshot = db.collection(coleccionCategorias).get().await()
            if (snapshot.isEmpty) {
                // Si Firestore está vacío (primera vez), creamos las categorías base
                inicializarCategoriasBase()
                return
            }
            
            val remotos = snapshot.toObjects(CategoriaModel::class.java)
            val uidsRemotos = remotos.map { it.uid }

            val entities = remotos.map { it.aEntity(estadoSync = "SINCRONIZADO") }
            categoriaDao.insertarLista(entities)
            categoriaDao.eliminarSincronizadosNoPresentes(uidsRemotos)
            
        } catch (e: Exception) {
            // Silencioso
        }
    }

    private suspend fun inicializarCategoriasBase() {
        val repuestos = listOf("Aceites", "Filtros", "Frenos", "Eléctrico", "Motor")
        val maquinaria = listOf(
            "Excavadora", "Retroexcavadora", "Volquete", "Cargador Frontal", 
            "Motoniveladora", "Rodillo Compactador", "Tractor Oruga", 
            "Camión Cisterna", "Camión Grúa", "Minicargador", 
            "Compresora", "Generador Eléctrico", "Otro"
        )

        val batch = db.batch()
        val total = mutableListOf<CategoriaEntity>()

        repuestos.forEach { nombre ->
            val uid = UUID.randomUUID().toString()
            val model = CategoriaModel(uid = uid, nombre = nombre, tipo = "REPUESTO")
            batch.set(db.collection(coleccionCategorias).document(uid), model)
            total.add(model.aEntity())
        }

        maquinaria.forEach { nombre ->
            val uid = UUID.randomUUID().toString()
            val model = CategoriaModel(uid = uid, nombre = nombre, tipo = "MAQUINARIA")
            batch.set(db.collection(coleccionCategorias).document(uid), model)
            total.add(model.aEntity())
        }

        try {
            batch.commit().await()
            categoriaDao.insertarLista(total)
        } catch (e: Exception) {
            // Error al inicializar
        }
    }
}
