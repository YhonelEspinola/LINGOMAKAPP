package com.lingomak.lingomakapp.data.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.MantenimientoEntity
import com.lingomak.lingomakapp.data.local.entity.MovimientoEntity
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.utils.DateUtils
import com.lingomak.lingomakapp.worker.SincronizacionMaquinariaWorker
import com.lingomak.lingomakapp.worker.SincronizacionMantenimientoWorker
import com.lingomak.lingomakapp.worker.SincronizacionMovimientosWorker
import com.lingomak.lingomakapp.worker.SincronizacionRepuestosWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*

data class ConsumoRepuesto(
    val repuestoUid: String,
    val nombre: String,
    val cantidad: Int
)

class MantenimientoRepository(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val coleccionMantenimientos = "mantenimientos"
    private val storageReportes = "mantenimientos_reportes"
    private val storageFinalizacion = "mantenimientos_finalizaciones"
    
    private val database = AppDatabase.getInstance(context)
    private val maintenanceDao = database.mantenimientoDao()
    private val maquinariaDao = database.maquinariaDao()
    private val repuestoDao = database.repuestoDao()
    private val movimientoDao = database.movimientoDao()
    private val appContext = context.applicationContext

    // ===================================================================
    // LECTURA LOCAL
    // ===================================================================

    fun obtenerTodosObservable(): LiveData<List<MantenimientoModel>> {
        return maintenanceDao.obtenerTodosObservable().map { entities ->
            entities.map { it.aModel() }
        }
    }

    fun obtenerAsignadosObservable(userUid: String): LiveData<List<MantenimientoModel>> {
        return maintenanceDao.obtenerAsignadosObservable(userUid).map { entities ->
            entities.map { it.aModel() }
        }
    }

    fun obtenerMantenimientoPorUid(
        uid: String,
        onSuccess: (MantenimientoModel) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entity = maintenanceDao.obtenerPorUid(uid)
                if (entity != null) {
                    withContextMain { onSuccess(entity.aModel()) }
                } else {
                    withContextMain { onError("No se encontró el mantenimiento localmente") }
                }
            } catch (e: Exception) {
                withContextMain { onError(e.message ?: "Error al obtener mantenimiento") }
            }
        }
    }

    // ===================================================================
    // ESCRITURA LOCAL
    // ===================================================================

    suspend fun agregarMantenimiento(
        mantenimiento: MantenimientoModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        try {
            val entity = mantenimiento.aEntity("PENDIENTE_CREAR", System.currentTimeMillis())
            maintenanceDao.insertarOActualizar(entity)
            SincronizacionMantenimientoWorker.encolar(appContext)
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al guardar mantenimiento localmente")
        }
    }

    suspend fun cambiarEstadoMantenimiento(
        uid: String,
        nuevoEstado: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "SISTEMA"
            maintenanceDao.cambiarEstadoLocal(
                uid = uid,
                nuevoEstado = nuevoEstado,
                actualizadoPor = userEmail,
                fechaActualizacion = obtenerFechaActual(),
                timestamp = System.currentTimeMillis()
            )
            SincronizacionMantenimientoWorker.encolar(appContext)
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al cambiar estado localmente")
        }
    }

    suspend fun actualizarMantenimiento(
        mantenimiento: MantenimientoModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val actual = maintenanceDao.obtenerPorUid(mantenimiento.uid)
                ?: throw IllegalStateException("No se encontró el mantenimiento")
            
            val actualizado = mantenimiento.aEntity(
                estadoSync = if (actual.estadoSync == "SINCRONIZADO") "PENDIENTE_ACTUALIZAR" else actual.estadoSync,
                timestampLocal = System.currentTimeMillis()
            )
            maintenanceDao.insertarOActualizar(actualizado)
            SincronizacionMantenimientoWorker.encolar(appContext)
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al actualizar mantenimiento")
        }
    }

    suspend fun iniciarMantenimiento(
        uidMantenimiento: String,
        uidMaquinaria: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        try {
            val mActual = maintenanceDao.obtenerPorUid(uidMantenimiento)
                ?: throw Exception("No se encontró mantenimiento")
            val maqActual = maquinariaDao.obtenerPorUid(uidMaquinaria)
                ?: throw Exception("No se encontró maquinaria")

            val mActualizado = mActual.copy(
                estado = "EN_PROCESO",
                fechaActualizacion = obtenerFechaActual(),
                estadoSync = if (mActual.estadoSync == "SINCRONIZADO") "PENDIENTE_ACTUALIZAR" else mActual.estadoSync,
                timestampLocal = System.currentTimeMillis()
            )

            val maqActualizada = maqActual.copy(
                estado = "EN_MANTENIMIENTO",
                fechaActualizacion = obtenerFechaActual(),
                estadoSync = if (maqActual.estadoSync == "SINCRONIZADO") "PENDIENTE_ACTUALIZAR" else maqActual.estadoSync,
                timestampLocal = System.currentTimeMillis()
            )

            // Transacción atómica en Room
            maintenanceDao.iniciarMantenimientoLocal(mActualizado, maqActualizada)

            SincronizacionMantenimientoWorker.encolar(appContext)
            SincronizacionMaquinariaWorker.encolar(appContext)
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al iniciar mantenimiento")
        }
    }

    suspend fun finalizarMantenimiento(
        uid: String,
        uidMaquinaria: String,
        fechaRealizada: String,
        horometroReal: Int,
        costoReal: Double,
        observacionesFinales: String,
        insumos: List<ConsumoRepuesto>,
        imagenesFinalizacionLocal: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        try {
            val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            database.withTransaction {
                val mActual = maintenanceDao.obtenerPorUid(uid)
                    ?: throw Exception("No se encontró mantenimiento")
                val maqActual = maquinariaDao.obtenerPorUid(uidMaquinaria)
                    ?: throw Exception("No se encontró maquinaria")

                // Validación de horómetro: no puede retroceder
                if (horometroReal < maqActual.horometroActual) {
                    throw Exception("El horómetro ingresado ($horometroReal) es menor al actual (${maqActual.horometroActual})")
                }

                // 1. Validar stock de todos los insumos primero
                for (insumo in insumos) {
                    val repuesto = repuestoDao.obtenerPorUid(insumo.repuestoUid)
                        ?: throw Exception("Repuesto no encontrado: ${insumo.nombre}")
                    if (repuesto.stockActual < insumo.cantidad) {
                        throw Exception("Stock insuficiente para ${insumo.nombre}. Disponible: ${repuesto.stockActual}")
                    }
                }

                // 2. Actualizar Mantenimiento
                val mActualizado = mActual.copy(
                    estado = "FINALIZADO",
                    fechaRealizada = fechaRealizada,
                    horometroReal = horometroReal,
                    costoReal = costoReal,
                    observaciones = observacionesFinales,
                    fechaActualizacion = obtenerFechaActual(),
                    imagenesFinalizacionLocal = imagenesFinalizacionLocal,
                    estadoSync = if (mActual.estadoSync == "SINCRONIZADO") "PENDIENTE_ACTUALIZAR" else mActual.estadoSync,
                    timestampLocal = System.currentTimeMillis()
                )
                maintenanceDao.insertarOActualizar(mActualizado)

                // 3. Actualizar Maquinaria
                val maqActualizada = maqActual.copy(
                    estado = "OPERATIVA",
                    horometroUltimoMantenimiento = horometroReal,
                    horometroActual = horometroReal,
                    fechaActualizacion = obtenerFechaActual(),
                    estadoSync = if (maqActual.estadoSync == "SINCRONIZADO") "PENDIENTE_ACTUALIZAR" else maqActual.estadoSync,
                    timestampLocal = System.currentTimeMillis()
                )
                maquinariaDao.insertarOActualizar(maqActualizada)

                // 4. Registrar Movimientos y Ajustar Stock
                for (insumo in insumos) {
                    val movimiento = MovimientoEntity(
                        uid = UUID.randomUUID().toString(),
                        repuestoUid = insumo.repuestoUid,
                        tipo = "SALIDA",
                        cantidad = insumo.cantidad,
                        fecha = System.currentTimeMillis(),
                        registradoPor = userUid,
                        observacion = "Consumo en mantenimiento $uid",
                        destinoSalida = "CONSUMO_INTERNO",
                        ordenMantenimientoUid = uid,
                        maquinariaUid = uidMaquinaria,
                        estadoSync = "PENDIENTE_CREAR",
                        timestampLocal = System.currentTimeMillis()
                    )
                    movimientoDao.insertar(movimiento)
                    repuestoDao.ajustarStockLocal(insumo.repuestoUid, -insumo.cantidad, System.currentTimeMillis())
                }
            }

            // 5. Encolar Workers de sincronización
            SincronizacionMantenimientoWorker.encolar(appContext)
            SincronizacionMaquinariaWorker.encolar(appContext)
            SincronizacionMovimientosWorker.encolar(appContext)
            SincronizacionRepuestosWorker.encolar(appContext)

            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al finalizar mantenimiento")
        }
    }

    // ===================================================================
    // SINCRONIZACIÓN CON FIRESTORE
    // ===================================================================

    suspend fun sincronizarPendientesConFirestore() {
        val pendientes = maintenanceDao.obtenerPendientesDeSincronizar()
        for (entity in pendientes) {
            try {
                var modelParaSubir = entity.aModel()
                var huboCambioLocal = false

                // Subir imágenes de reporte pendientes
                if (entity.imagenesReporteLocal.isNotEmpty()) {
                    val urlsReporte = mutableListOf<String>()
                    for (path in entity.imagenesReporteLocal) {
                        try {
                            val url = subirImagenDesdeArchivo(entity.uid, path, "reporte")
                            if (url.isNotEmpty()) urlsReporte.add(url)
                        } catch (e: Exception) { /* Reintento */ }
                    }
                    if (urlsReporte.isNotEmpty()) {
                        modelParaSubir = modelParaSubir.copy(imagenesReporte = urlsReporte)
                        huboCambioLocal = true
                    }
                }

                // Subir imágenes de finalización pendientes
                if (entity.imagenesFinalizacionLocal.isNotEmpty()) {
                    val urlsFinal = mutableListOf<String>()
                    for (path in entity.imagenesFinalizacionLocal) {
                        try {
                            val url = subirImagenDesdeArchivo(entity.uid, path, "finalizacion")
                            if (url.isNotEmpty()) urlsFinal.add(url)
                        } catch (e: Exception) { /* Reintento */ }
                    }
                    if (urlsFinal.isNotEmpty()) {
                        modelParaSubir = modelParaSubir.copy(imagenesFinalizacion = urlsFinal)
                        huboCambioLocal = true
                    }
                }

                // Si subimos fotos, actualizamos el registro local antes de subir a Firestore
                // para que los campos Local queden vacíos una vez procesados
                if (huboCambioLocal) {
                    val entityLimpia = entity.copy(
                        imagenesReporte = modelParaSubir.imagenesReporte,
                        imagenesFinalizacion = modelParaSubir.imagenesFinalizacion,
                        imagenesReporteLocal = emptyList(),
                        imagenesFinalizacionLocal = emptyList()
                    )
                    maintenanceDao.insertarOActualizar(entityLimpia)
                    
                    // Actualizamos el modelo para subir con las listas locales ya limpias
                    modelParaSubir = modelParaSubir.copy(
                        imagenesReporteLocal = emptyList(),
                        imagenesFinalizacionLocal = emptyList()
                    )
                }

                val docRemoto = db.collection(coleccionMantenimientos).document(entity.uid).get().await()
                val fechaRemotaStr = docRemoto.getString("fechaActualizacion") ?: ""
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val timestampRemoto = try { format.parse(fechaRemotaStr)?.time ?: 0L } catch(e: Exception) { 0L }

                if (docRemoto.exists() && timestampRemoto > entity.timestampLocal) {
                    maintenanceDao.marcarComoSincronizado(entity.uid)
                    continue
                }

                // Nos aseguramos de que el modelo que sube a Firestore NO lleve las rutas locales
                val finalModel = modelParaSubir.copy(
                    imagenesReporteLocal = emptyList(),
                    imagenesFinalizacionLocal = emptyList()
                )

                db.collection(coleccionMantenimientos).document(entity.uid).set(finalModel).await()
                maintenanceDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) {
                // Reintento
            }
        }
    }

    private suspend fun subirImagenDesdeArchivo(uid: String, path: String, tipo: String): String {
        val file = File(path)
        if (!file.exists()) return ""
        val fileUri = Uri.fromFile(file)
        val folder = if (tipo == "reporte") storageReportes else storageFinalizacion
        
        val ts = System.currentTimeMillis()
        val randomId = UUID.randomUUID().toString().take(8)
        val filename = "${uid}_${ts}_${randomId}.jpg"
        
        val ref = storage.reference.child(folder).child(filename)
        ref.putFile(fileUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun descargarCambiosDeFirestore() {
        try {
            val snapshot = db.collection(coleccionMantenimientos).get().await()
            val remotos = snapshot.toObjects(MantenimientoModel::class.java)
            val uidsRemotos = remotos.map { it.uid }

            if (uidsRemotos.isEmpty()) {
                maintenanceDao.eliminarSincronizados()
            } else {
                for (modelo in remotos) {
                    val local = maintenanceDao.obtenerPorUid(modelo.uid)
                    
                    if (local != null && local.estadoSync != "SINCRONIZADO") {
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val timestampRemoto = try { format.parse(modelo.fechaActualizacion)?.time ?: 0L } catch(e: Exception) { 0L }
                        if (local.timestampLocal >= timestampRemoto) {
                            continue
                        }
                    }

                    maintenanceDao.insertarOActualizar(modelo.aEntity("SINCRONIZADO", System.currentTimeMillis()))
                }
                maintenanceDao.eliminarSincronizadosNoPresentes(uidsRemotos)
            }
        } catch (e: Exception) {
            android.util.Log.e("MantenimientoRepo", "Error al descargar cambios: ${e.message}", e)
            throw e
        }
    }

    suspend fun descargarCambiosPaginados(ultimaFecha: String? = null, batchSize: Long = 50) {
        try {
            var query = db.collection(coleccionMantenimientos)
                .orderBy("fechaProgramada", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(batchSize)

            if (ultimaFecha != null) {
                query = query.startAfter(ultimaFecha)
            }

            val snapshot = query.get().await()
            val remotos = snapshot.toObjects(MantenimientoModel::class.java)

            if (remotos.isNotEmpty()) {
                val entities = remotos.map { it.aEntity(estadoSync = "SINCRONIZADO", timestampLocal = System.currentTimeMillis()) }
                maintenanceDao.insertarLista(entities)
            }
        } catch (e: Exception) {
            // Manejar error
        }
    }

    // ===================================================================
    // OTROS (Mantenidos de lógica original)
    // ===================================================================

    fun actualizarMantenimientosVencidos(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val todos = maintenanceDao.obtenerTodos()
                val pendientes = todos.filter { it.estado == "PENDIENTE" }
                
                var huboCambios = false
                for (mEntity in pendientes) {
                    if (DateUtils.fechaYaPaso(mEntity.fechaProgramada)) {
                        maintenanceDao.insertarOActualizar(mEntity.copy(
                            estado = "VENCIDO",
                            fechaActualizacion = obtenerFechaActual(),
                            estadoSync = "PENDIENTE_ACTUALIZAR",
                            timestampLocal = System.currentTimeMillis()
                        ))
                        huboCambios = true
                    }
                }

                if (huboCambios) {
                    SincronizacionMantenimientoWorker.encolar(appContext)
                }
                withContextMain { onSuccess() }
            } catch (e: Exception) {
                withContextMain { onError(e.message ?: "Error al actualizar vencidos") }
            }
        }
    }

    private fun obtenerFechaActual(): String {
        return java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.getDefault()
        ).format(java.util.Date())
    }

    fun validarMantenimientoActivo(
        uidMaquinaria: String,
        onExiste: () -> Unit,
        onNoExiste: () -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val todos = maintenanceDao.obtenerTodos()
                val existe = todos.any { 
                    it.uidMaquinaria == uidMaquinaria && (it.estado == "PENDIENTE" || it.estado == "EN_PROCESO") 
                }
                
                if (existe) withContextMain { onExiste() } else withContextMain { onNoExiste() }
            } catch (e: Exception) {
                onError(e.message ?: "Error al validar")
            }
        }
    }

    private suspend fun withContextMain(action: () -> Unit) {
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            action()
        }
    }

    // =======================================================================
    // MAPPERS
    // =======================================================================

    private fun MantenimientoModel.aEntity(estadoSync: String, timestampLocal: Long): MantenimientoEntity {
        return MantenimientoEntity(
            uid = uid,
            codigoMantenimiento = codigoMantenimiento,
            uidMaquinaria = uidMaquinaria,
            codigoMaquinaria = codigoMaquinaria,
            nombreMaquinaria = nombreMaquinaria,
            tipoMaquinaria = tipoMaquinaria,
            tipoMantenimiento = tipoMantenimiento,
            descripcion = descripcion,
            fechaProgramada = fechaProgramada,
            fechaRealizada = fechaRealizada,
            estado = estado,
            responsable = responsable,
            responsableUid = responsableUid,
            observaciones = observaciones,
            costoEstimado = costoEstimado,
            costoReal = costoReal,
            horometroProgramado = horometroProgramado,
            horometroReal = horometroReal,
            fechaRegistro = fechaRegistro,
            fechaActualizacion = fechaActualizacion,
            registradoPor = registradoPor,
            actualizadoPor = actualizadoPor,
            prioridad = prioridad,
            imagenesReporte = imagenesReporte,
            imagenesFinalizacion = imagenesFinalizacion,
            imagenesReporteLocal = imagenesReporteLocal,
            imagenesFinalizacionLocal = imagenesFinalizacionLocal,
            reporteIA = reporteIA,
            estadoSync = estadoSync,
            timestampLocal = timestampLocal
        )
    }

    private fun MantenimientoEntity.aModel(): MantenimientoModel {
        return MantenimientoModel(
            uid = uid,
            codigoMantenimiento = codigoMantenimiento,
            uidMaquinaria = uidMaquinaria,
            codigoMaquinaria = codigoMaquinaria,
            nombreMaquinaria = nombreMaquinaria,
            tipoMaquinaria = tipoMaquinaria,
            tipoMantenimiento = tipoMantenimiento,
            descripcion = descripcion,
            fechaProgramada = fechaProgramada,
            fechaRealizada = fechaRealizada,
            estado = estado,
            responsable = responsable,
            responsableUid = responsableUid,
            observaciones = observaciones,
            costoEstimado = costoEstimado,
            costoReal = costoReal,
            horometroProgramado = horometroProgramado,
            horometroReal = horometroReal,
            fechaRegistro = fechaRegistro,
            fechaActualizacion = fechaActualizacion,
            registradoPor = registradoPor,
            actualizadoPor = actualizadoPor,
            prioridad = prioridad,
            imagenesReporte = imagenesReporte,
            imagenesFinalizacion = imagenesFinalizacion,
            imagenesReporteLocal = imagenesReporteLocal,
            imagenesFinalizacionLocal = imagenesFinalizacionLocal,
            reporteIA = reporteIA
        )
    }

    suspend fun guardarReporteIA(uid: String, textoReporte: String) {
        try {
            val actual = maintenanceDao.obtenerPorUid(uid) ?: return
            val actualizado = actual.copy(
                reporteIA = textoReporte,
                estadoSync = if (actual.estadoSync == "SINCRONIZADO") "PENDIENTE_ACTUALIZAR" else actual.estadoSync,
                timestampLocal = System.currentTimeMillis()
            )
            maintenanceDao.insertarOActualizar(actualizado)
            SincronizacionMantenimientoWorker.encolar(appContext)
        } catch (e: Exception) {
            // Manejar error
        }
    }
}
