package com.lingomak.lingomakapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.RepuestoEntity
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.data.worker.SincronizacionRepuestosWorker
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositorio de Repuestos, OFFLINE-FIRST.
 *
 * Arquitectura: la UI (vía ViewModel) SIEMPRE lee y escribe contra
 * Room (rápido, funciona sin conexión). Este repositorio nunca llama
 * a Firestore directamente desde sus operaciones de escritura "normales"
 * (guardar, cambiar estado, ajustar stock) — en su lugar, marca el
 * registro local como pendiente de sincronizar y encola un trabajo de
 * WorkManager que se encarga de subirlo cuando haya conexión.
 *
 * Las únicas excepciones son subirImagenQR y subirImagenRepuesto
 * (Storage), que sí requieren red real porque no tiene sentido cachear
 * un archivo binario grande en SQLite; si no hay conexión, esas
 * llamadas fallan con onFailure y el Fragment debe avisar al usuario.
 *
 * `context` se requiere para obtener la instancia de AppDatabase y
 * para encolar trabajos de WorkManager.
 */
class RepuestoRepository(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val repuestosCollection = db.collection("repuestos")
    private val qrStorageRef = storage.reference.child("repuestos_qr")
    private val imagenesStorageRef = storage.reference.child("repuestos_imagenes")

    private val repuestoDao = AppDatabase.getInstance(context).repuestoDao()
    private val appContext = context.applicationContext

    // ===================================================================
    // LECTURA LOCAL (Room) — lo que observa la UI
    // ===================================================================

    /**
     * Lista completa observable directamente desde Room. La UI se
     * suscribe a esto; cualquier escritura local o sincronización
     * remota actualiza la pantalla automáticamente.
     */
    fun obtenerRepuestosObservable(): LiveData<List<RepuestoModel>> {
        return repuestoDao.obtenerTodosObservable().map { lista ->
            lista.map { it.aModel() }
        }
    }

    fun obtenerRepuestoPorUidObservable(uid: String): LiveData<RepuestoModel?> {
        return repuestoDao.obtenerPorUidObservable(uid).map { entity ->
            entity?.aModel()
        }
    }

    suspend fun obtenerRepuestoPorUid(uid: String): RepuestoModel? {
        return repuestoDao.obtenerPorUid(uid)?.aModel()
    }

    // ===================================================================
    // ESCRITURA LOCAL (Room) — instantánea, funciona offline
    // ===================================================================

    /**
     * Guarda (crea o edita) un repuesto en Room de inmediato y encola
     * la sincronización con Firestore. onSuccess se dispara apenas
     * Room confirma la escritura local — NO espera a la red.
     */
    suspend fun guardarRepuesto(
        repuesto: RepuestoModel,
        esNuevo: Boolean,
        imagenLocalPath: String? = null,
        qrLocalPath: String? = null,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val auditado = if (!esNuevo) {
                repuesto.copy(
                    modificadoPorUid = user?.uid,
                    modificadoPorNombre = user?.displayName ?: "Usuario",
                    fechaUltimaModificacion = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
            } else {
                repuesto
            }

            val entity = auditado.aEntity(
                estadoSync = if (esNuevo) "PENDIENTE_CREAR" else "PENDIENTE_ACTUALIZAR",
                timestampLocal = System.currentTimeMillis(),
                imagenLocalPath = imagenLocalPath,
                qrLocalPath = qrLocalPath
            )

            repuestoDao.insertarOActualizar(entity)
            SincronizacionRepuestosWorker.encolar(appContext)
            onSuccess()
        } catch (exception: Exception) {
            onFailure(exception)
        }
    }

    /**
     * Activa/inactiva un repuesto localmente (baja lógica) y encola
     * la sincronización.
     */
    suspend fun cambiarEstadoRepuesto(
        uid: String,
        nuevoEstado: String,
        actualizadoPor: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val actual = repuestoDao.obtenerPorUid(uid)
                ?: throw IllegalStateException("Repuesto no encontrado localmente: $uid")

            val actualizado = actual.copy(
                estado = nuevoEstado,
                actualizadoPor = actualizadoPor,
                fechaActualizacion = System.currentTimeMillis(),
                modificadoPorUid = user?.uid,
                modificadoPorNombre = user?.displayName ?: "Usuario",
                fechaUltimaModificacion = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                estadoSync = if (actual.estadoSync == "SINCRONIZADO") "PENDIENTE_ACTUALIZAR" else actual.estadoSync,
                timestampLocal = System.currentTimeMillis()
            )

            repuestoDao.insertarOActualizar(actualizado)
            SincronizacionRepuestosWorker.encolar(appContext)
            onSuccess()
        } catch (exception: Exception) {
            onFailure(exception)
        }
    }

    /**
     * Ajusta el stock de forma local e inmediata (usado al registrar
     * un movimiento de entrada/salida). delta puede ser positivo o
     * negativo. Funciona sin conexión; se sincroniza después.
     */
    suspend fun ajustarStockRepuesto(
        uid: String,
        delta: Int,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            repuestoDao.ajustarStockLocal(uid, delta, System.currentTimeMillis())

            SincronizacionRepuestosWorker.encolar(appContext)

            onSuccess()
        } catch (exception: Exception) {
            onFailure(exception)
        }
    }

    // ===================================================================
    // SINCRONIZACIÓN CON FIRESTORE — usado SOLO por SincronizacionRepuestosWorker
    // ===================================================================

    /**
     * Sube a Firestore todos los repuestos locales marcados como
     * pendientes. Resuelve conflictos con estrategia "último que
     * escribe gana", comparando timestampLocal contra la
     * fechaActualizacion remota antes de sobreescribir.
     *
     * Esta función es suspend y de uso EXCLUSIVO del Worker — el
     * ViewModel/Fragment nunca la llama directamente.
     */
    suspend fun sincronizarPendientesConFirestore() {
        val pendientes = repuestoDao.obtenerPendientesDeSincronizar()

        for (entity in pendientes) {
            try {
                var modeloParaSubir = entity.aModel()
                var huboCambioLocal = false

                // Si tiene una imagen local pendiente de subir
                if (!entity.imagenLocalPath.isNullOrEmpty()) {
                    try {
                        val urlDescarga = subirImagenDesdeArchivo(entity.uid, entity.imagenLocalPath, esQR = false)
                        modeloParaSubir = modeloParaSubir.copy(imagenUrl = urlDescarga)
                        huboCambioLocal = true
                    } catch (e: Exception) {
                        continue
                    }
                }

                // Si tiene un QR local pendiente de subir
                if (!entity.qrLocalPath.isNullOrEmpty()) {
                    try {
                        val urlDescarga = subirImagenDesdeArchivo(entity.uid, entity.qrLocalPath, esQR = true)
                        modeloParaSubir = modeloParaSubir.copy(codigoQR = urlDescarga)
                        huboCambioLocal = true
                    } catch (e: Exception) {
                        continue
                    }
                }

                if (huboCambioLocal) {

                    val entityActualizada = entity.copy(
                        imagenUrl = modeloParaSubir.imagenUrl,
                        codigoQR = modeloParaSubir.codigoQR,
                        imagenLocalPath = if (entity.imagenLocalPath != null && modeloParaSubir.imagenUrl.isNotEmpty()) null else entity.imagenLocalPath,
                        qrLocalPath = if (entity.qrLocalPath != null && modeloParaSubir.codigoQR.isNotEmpty()) null else entity.qrLocalPath
                    )
                    repuestoDao.insertarOActualizar(entityActualizada)
                }

                val docRemoto = repuestosCollection.document(entity.uid).get().await()

                val timestampRemoto = docRemoto.getTimestamp("fechaActualizacion")
                    ?.toDate()?.time ?: 0L

                // Last-write-wins, cuando el remoto es más nuevo que nuestro
                if (docRemoto.exists() && timestampRemoto > entity.timestampLocal) {
                    repuestoDao.marcarComoSincronizado(entity.uid)
                    continue
                }

                repuestosCollection.document(entity.uid)
                    .set(modeloParaSubir)
                    .await()

                repuestoDao.marcarComoSincronizado(entity.uid)
            } catch (exception: Exception) {
                // Se deja el registro como pendiente; el próximo intento de WorkManager lo reintentará.
            }
        }
    }

    private suspend fun subirImagenDesdeArchivo(uid: String, path: String, esQR: Boolean): String {
        val fileUri = android.net.Uri.fromFile(java.io.File(path))
        val archivoRef = if (esQR) qrStorageRef.child("$uid.png") else imagenesStorageRef.child("$uid.jpg")
        archivoRef.putFile(fileUri).await()
        return archivoRef.downloadUrl.await().toString()
    }

    /**
     * Descarga repuestos desde Firestore de forma paginada.
     */
    suspend fun descargarCambiosPaginados(ultimoNombre: String? = null, batchSize: Long = 50) {
        try {
            var query = repuestosCollection
                .orderBy("nombre", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .limit(batchSize)

            if (ultimoNombre != null) {
                query = query.startAfter(ultimoNombre)
            }

            val snapshot = query.get().await()
            val remotos = snapshot.toObjects(RepuestoModel::class.java)

            if (remotos.isNotEmpty()) {
                val entities = remotos.map { it.aEntity(estadoSync = "SINCRONIZADO", timestampLocal = System.currentTimeMillis()) }
                repuestoDao.insertarOActualizarLista(entities)
            }
        } catch (e: Exception) {
            // Error de red, mantiene datos locales
        }
    }

    /**
     * Trae todos los repuestos de Firestore (Legacy - se recomienda usar paginado).
     */
    suspend fun descargarCambiosDeFirestore() {
        val snapshot = repuestosCollection.get().await()
        val remotos = snapshot.toObjects(RepuestoModel::class.java)
        val uidsRemotos = remotos.map { it.uid }

        if (uidsRemotos.isEmpty()) {
            repuestoDao.eliminarTodosSincronizados()
        } else {
            val contadorDao = AppDatabase.getInstance(appContext).contadorDao()

            for (modelo in remotos) {
                val local = repuestoDao.obtenerPorUid(modelo.uid)

                // Actualizar contador basado en el código remoto
                val prefijo = com.lingomak.lingomakapp.utils.CodigoInternoGenerator.extraerPrefijo(modelo.codigoInterno)
                if (prefijo != null) {
                    val numero = com.lingomak.lingomakapp.utils.CodigoInternoGenerator.extraerNumero(modelo.codigoInterno)
                    contadorDao.actualizarSiEsMayor(prefijo, numero)
                }

                // Conservar cambios locales más recientes
                if (local != null && local.estadoSync != "SINCRONIZADO") {
                    val timestampRemoto = modelo.fechaActualizacion?.time ?: 0L
                    if (local.timestampLocal >= timestampRemoto) {
                        continue
                    }
                }

                repuestoDao.insertarOActualizar(
                    modelo.aEntity(estadoSync = "SINCRONIZADO", timestampLocal = System.currentTimeMillis())
                )
            }
            
            // Eliminar locales sincronizados que ya no están en Firestore
            repuestoDao.eliminarSincronizadosNoPresentes(uidsRemotos)
        }
    }

    // ===================================================================
    // STORAGE (Imágenes / QR) — requiere red real, no se cachea offline
    // ===================================================================

    fun subirImagenQR(
        uid: String,
        qrBitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val baos = ByteArrayOutputStream()
        qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        val archivoRef = qrStorageRef.child("$uid.png")

        archivoRef.putBytes(data)
            .addOnSuccessListener {
                archivoRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }.addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun subirImagenRepuesto(
        uid: String,
        imagenBytes: ByteArray,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val archivoRef = imagenesStorageRef.child("$uid.jpg")

        archivoRef.putBytes(imagenBytes)
            .addOnSuccessListener {
                archivoRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }.addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }
}

// =======================================================================
// MAPPERS — conversión entre las 3 representaciones (Model/Entity/Long-Date)
// =======================================================================

private fun RepuestoModel.aEntity(estadoSync: String, timestampLocal: Long, imagenLocalPath: String? = null, qrLocalPath: String? = null): RepuestoEntity {
    return RepuestoEntity(
        uid = uid,
        codigoInterno = codigoInterno,
        nombre = nombre,
        categoria = categoria,
        marca = marca,
        descripcion = descripcion,
        stockActual = stockActual,
        stockMinimo = stockMinimo,
        stockMaximo = stockMaximo,
        ubicacionAlmacen = ubicacionAlmacen,
        imagenUrl = imagenUrl,
        codigoQR = codigoQR,
        proveedorNombre = proveedorNombre,
        proveedorContacto = proveedorContacto,
        imagenLocalPath = imagenLocalPath,
        qrLocalPath = qrLocalPath,
        estado = estado,
        fechaRegistro = fechaRegistro?.time,
        fechaActualizacion = fechaActualizacion?.time,
        registradoPor = registradoPor,
        actualizadoPor = actualizadoPor,
        modificadoPorUid = modificadoPorUid,
        modificadoPorNombre = modificadoPorNombre,
        fechaUltimaModificacion = fechaUltimaModificacion,
        estadoSync = estadoSync,
        timestampLocal = timestampLocal
    )
}

private fun RepuestoEntity.aModel(): RepuestoModel {
    return RepuestoModel(
        uid = uid,
        codigoInterno = codigoInterno,
        nombre = nombre,
        categoria = categoria,
        marca = marca,
        descripcion = descripcion,
        stockActual = stockActual,
        stockMinimo = stockMinimo,
        stockMaximo = stockMaximo,
        ubicacionAlmacen = ubicacionAlmacen,
        imagenUrl = imagenUrl,
        codigoQR = codigoQR,
        proveedorNombre = proveedorNombre,
        proveedorContacto = proveedorContacto,
        estado = estado,
        fechaRegistro = fechaRegistro?.let { Date(it) },
        fechaActualizacion = fechaActualizacion?.let { Date(it) },
        registradoPor = registradoPor,
        actualizadoPor = actualizadoPor,
        modificadoPorUid = modificadoPorUid,
        modificadoPorNombre = modificadoPorNombre,
        fechaUltimaModificacion = fechaUltimaModificacion
    )
}
