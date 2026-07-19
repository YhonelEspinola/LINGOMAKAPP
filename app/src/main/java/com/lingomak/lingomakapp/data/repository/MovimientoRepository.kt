package com.lingomak.lingomakapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.MovimientoEntity
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.worker.SincronizacionRepuestosWorker
import com.lingomak.lingomakapp.worker.SincronizacionMovimientosWorker
import java.util.*

/**
 * Repositorio de Movimientos (Entradas/Salidas).
 * Sigue el patrón offline-first.
 */
class MovimientoRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val movimientosCollection = db.collection("movimientos")
    
    private val database = AppDatabase.getInstance(context)
    private val movimientoDao = database.movimientoDao()
    private val repuestoDao = database.repuestoDao()

    /**
     * Registra un movimiento localmente en Room, ajusta el stock del repuesto
     * y encola la sincronización con Firestore.
     */
    suspend fun registrarMovimiento(movimiento: MovimientoModel) {
        val entity = movimiento.aEntity().copy(estadoSync = "PENDIENTE_CREAR")
        
        // 1. Insertar movimiento en Room
        movimientoDao.insertar(entity)
        
        // 2. Ajustar stock del repuesto localmente
        val delta = if (movimiento.tipo == "ENTRADA") movimiento.cantidad else -movimiento.cantidad
        repuestoDao.ajustarStockLocal(movimiento.repuestoUid, delta, System.currentTimeMillis())

        // 2.5. Encolar sincronización del REPUESTO (el stock actualizado
        //      también debe subir a Firestore, no solo el movimiento)
        SincronizacionRepuestosWorker.encolar(context)

        // 3. Crear alerta de actividad para el administrador si es una salida de operario
        // 4. Encolar Worker de sincronización
        encolarSincronizacion()
    }

    /**
     * Retorna el historial observable de un repuesto desde Room.
     */
    fun obtenerHistorial(repuestoUid: String): LiveData<List<MovimientoModel>> {
        return movimientoDao.obtenerPorRepuestoObservable(repuestoUid).map { entities ->
            entities.map { it.aModel() }
        }
    }

    /**
     * Descarga movimientos de un repuesto específico de forma paginada.
     */
    suspend fun descargarMovimientosPaginadosPorRepuesto(
        repuestoUid: String,
        ultimoTimestamp: Long? = null,
        batchSize: Long = 50
    ) {
        try {
            var query = movimientosCollection
                .whereEqualTo("repuestoUid", repuestoUid)
                .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(batchSize)

            if (ultimoTimestamp != null) {
                query = query.startAfter(Date(ultimoTimestamp))
            }

            val snapshot = query.get().await()
            val remotos = snapshot.toObjects(MovimientoModel::class.java)

            if (remotos.isNotEmpty()) {
                val entities = remotos.map { it.aEntity().copy(estadoSync = "SINCRONIZADO") }
                movimientoDao.insertarLista(entities)
            }
        } catch (e: Exception) {
            // Manejar error
        }
    }

    /**
     * Descarga movimientos desde Firestore para un repuesto específico.
     * Inserta solo los que no existan localmente.
     */
    suspend fun descargarMovimientosDeFirestore(repuestoUid: String) {
        try {
            val snapshot = movimientosCollection
                .whereEqualTo("repuestoUid", repuestoUid)
                .get()
                .await()
            
            val remotos = snapshot.toObjects(MovimientoModel::class.java)
            val uidsRemotos = remotos.map { it.uid }

            if (uidsRemotos.isEmpty()) {
                // Si no hay nada en Firestore para este repuesto, limpiamos los locales ya sincronizados
                movimientoDao.eliminarTodosSincronizadosPorRepuesto(repuestoUid)
            } else {
                val nuevasEntities = mutableListOf<MovimientoEntity>()
                for (model in remotos) {
                    nuevasEntities.add(model.aEntity().copy(estadoSync = "SINCRONIZADO"))
                }
                // Insertamos/Actualizamos todos los remotos
                movimientoDao.insertarLista(nuevasEntities)
                
                // Eliminamos los que están en Room (SINCRONIZADOS) pero ya no en Firestore
                movimientoDao.eliminarSincronizadosNoPresentesPorRepuesto(repuestoUid, uidsRemotos)
            }
        } catch (e: Exception) {
            // Error de red o Firestore, se ignora en offline-first
        }
    }

    /**
     * Retorna el historial observable de todos los repuestos desde Room.
     */
    fun obtenerTodos(): LiveData<List<MovimientoModel>> {
        return movimientoDao.obtenerTodosObservable().map { entities ->
            entities.map { it.aModel() }
        }
    }

    /**
     * Descarga movimientos desde Firestore de forma paginada.
     * @param ultimoDocumento El último timestamp descargado para continuar desde ahí.
     * @param batchSize Cantidad de registros por lote.
     */
    suspend fun descargarMovimientosPaginados(ultimoTimestamp: Long? = null, batchSize: Long = 50) {
        try {
            var query = movimientosCollection
                .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(batchSize)

            if (ultimoTimestamp != null) {
                query = query.startAfter(Date(ultimoTimestamp))
            }

            val snapshot = query.get().await()
            val remotos = snapshot.toObjects(MovimientoModel::class.java)

            if (remotos.isNotEmpty()) {
                val entities = remotos.map { it.aEntity().copy(estadoSync = "SINCRONIZADO") }
                movimientoDao.insertarLista(entities)
            }
        } catch (e: Exception) {
            // Error de red, se queda con lo que tiene en Room
        }
    }

    /**
     * Descarga todos los movimientos desde Firestore (Legacy - se recomienda usar paginado).
     */
    suspend fun descargarTodosDesdeFirestore() {
        try {
            val snapshot = movimientosCollection
                .limit(500)
                .get()
                .await()

            val remotos = snapshot.toObjects(MovimientoModel::class.java)
            val uidsRemotos = remotos.map { it.uid }

            if (uidsRemotos.isEmpty()) {
                // Si la colección está vacía en Firestore, limpiamos locales sincronizados
                movimientoDao.eliminarTodosSincronizados()
            } else {
                val nuevasEntities = mutableListOf<MovimientoEntity>()
                for (model in remotos) {
                    nuevasEntities.add(model.aEntity().copy(estadoSync = "SINCRONIZADO"))
                }
                // Insertamos/Actualizamos todos los remotos
                movimientoDao.insertarLista(nuevasEntities)
                
                // Eliminamos los que están en Room (SINCRONIZADOS) pero ya no en Firestore
                movimientoDao.eliminarSincronizadosNoPresentes(uidsRemotos)
            }
        } catch (e: Exception) {
            // Ignorar en offline-first
        }
    }

    private fun encolarSincronizacion() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SincronizacionMovimientosWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    /**
     * Sincroniza los movimientos pendientes de subir a Firestore.
     */
    suspend fun sincronizarPendientesConFirestore() {
        val pendientes = movimientoDao.obtenerPendientesDeSincronizar()
        for (entity in pendientes) {
            try {
                movimientosCollection.document(entity.uid)
                    .set(entity.aModel())
                    .await()
                movimientoDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) {
                // Silenciamos error para que el Worker reintente después
            }
        }
    }

    suspend fun eliminarMovimiento(uid: String) {
        val entity = movimientoDao.obtenerPorUid(uid) ?: return
        
        // 1. Revertir stock localmente
        val deltaReversion = if (entity.tipo == "ENTRADA") -entity.cantidad else entity.cantidad
        repuestoDao.ajustarStockLocal(entity.repuestoUid, deltaReversion, System.currentTimeMillis())

        // 1.5. Encolar sincronización del REPUESTO (el stock revertido
        //      también debe subir a Firestore, no solo el movimiento)
        SincronizacionRepuestosWorker.encolar(context)

        // 3. Eliminar de Room
        movimientoDao.eliminar(entity)
        
        // 3. Eliminar de Firestore (si existe red) o encolar worker? 
        // Para simplificar según prompt Parte D punto c:
        try {
            movimientosCollection.document(uid).delete().await()
        } catch (e: Exception) {
            // Si falla, el registro local ya no existe. 
            // En un sistema robusto marcaríamos para borrado remoto pendiente.
        }
    }

    suspend fun obtenerPorUid(uid: String): MovimientoModel? {
        return movimientoDao.obtenerPorUid(uid)?.aModel()
    }

    suspend fun actualizarMovimiento(movimiento: MovimientoModel, deltaStock: Int) {
        // 1. Actualizar en Room
        val entity = movimiento.aEntity().copy(estadoSync = "PENDIENTE_ACTUALIZAR")
        movimientoDao.actualizar(entity)
        
        // 2. Ajustar stock del repuesto localmente con la diferencia calculada
        repuestoDao.ajustarStockLocal(movimiento.repuestoUid, deltaStock, System.currentTimeMillis())

        // 2.5. Encolar sincronización del REPUESTO (el stock actualizado
        //      también debe subir a Firestore, no solo el movimiento)
        SincronizacionRepuestosWorker.encolar(context)

        // 4. Encolar Worker
        encolarSincronizacion()
    }
}

// =======================================================================
// MAPPERS
// =======================================================================

private fun MovimientoEntity.aModel(): MovimientoModel {
    return MovimientoModel(
        uid = uid,
        repuestoUid = repuestoUid,
        tipo = tipo,
        cantidad = cantidad,
        fecha = fecha?.let { Date(it) },
        registradoPor = registradoPor,
        observacion = observacion,
        destinoSalida = destinoSalida,
        ordenMantenimientoUid = ordenMantenimientoUid,
        maquinariaUid = maquinariaUid
    )
}

private fun MovimientoModel.aEntity(): MovimientoEntity {
    return MovimientoEntity(
        uid = uid,
        repuestoUid = repuestoUid,
        tipo = tipo,
        cantidad = cantidad,
        fecha = fecha?.time,
        registradoPor = registradoPor,
        observacion = observacion,
        destinoSalida = destinoSalida,
        ordenMantenimientoUid = ordenMantenimientoUid,
        maquinariaUid = maquinariaUid
    )
}
