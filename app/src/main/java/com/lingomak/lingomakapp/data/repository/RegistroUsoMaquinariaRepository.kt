package com.lingomak.lingomakapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.MaquinariaEntity
import com.lingomak.lingomakapp.data.local.entity.RegistroUsoMaquinariaEntity
import com.lingomak.lingomakapp.data.local.entity.SuministroEntity
import com.lingomak.lingomakapp.data.local.entity.SolicitudMantenimientoEntity
import com.lingomak.lingomakapp.data.local.entity.aEntity
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.RegistroUsoMaquinariaModel
import com.lingomak.lingomakapp.data.model.SuministroModel
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel
import com.lingomak.lingomakapp.utils.DateUtils
import com.lingomak.lingomakapp.data.worker.SincronizacionMaquinariaWorker
import com.lingomak.lingomakapp.data.worker.SincronizacionRegistroUsoMaquinariaWorker
import com.lingomak.lingomakapp.data.worker.SincronizacionMantenimientoWorker
import com.lingomak.lingomakapp.data.worker.SincronizacionSolicitudMantenimientoWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class RegistroUsoMaquinariaRepository(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val database = AppDatabase.getInstance(context)
    private val registroUsoDao = database.registroUsoMaquinariaDao()
    private val maquinariaDao = database.maquinariaDao()
    private val suministroDao = database.suministroDao()
    private val categoriaDao = database.categoriaDao()
    private val maintenanceDao = database.mantenimientoDao()
    private val solicitudDao = database.solicitudMantenimientoDao()
    private val appContext = context.applicationContext

    fun obtenerRegistrosPorMaquinaria(uidMaquinaria: String): LiveData<List<RegistroUsoMaquinariaModel>> {
        return registroUsoDao.obtenerPorMaquinariaObservable(uidMaquinaria).map { entities ->
            entities.map { it.aModel() }
        }
    }

    fun obtenerMaquinariasActivasObservable(): LiveData<List<MaquinariaModel>> {
        return maquinariaDao.obtenerTodasObservable().map { entities ->
            entities.map { it.aModel() }
        }
    }

    fun obtenerTodosObservable(): LiveData<List<RegistroUsoMaquinariaModel>> {
        return registroUsoDao.obtenerTodosObservable().map { entities ->
            entities.map { it.aModel() }
        }
    }

    suspend fun registrarUsoMaquinaria(
        registroUsoModel: RegistroUsoMaquinariaModel,
        suministroModel: SuministroModel?,
        esEdicion: Boolean,
        horometroFinalOriginal: Double? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val user = FirebaseAuth.getInstance().currentUser

            // 1. Obtener la entidad de la maquinaria
            val maqEntity = maquinariaDao.obtenerPorUid(registroUsoModel.uidMaquinaria)
                ?: throw Exception("Maquinaria no encontrada")

            // 2. Validaciones de horómetro
            val horometroFinal = registroUsoModel.horometroFinal
            if (horometroFinal < registroUsoModel.horometroAnterior) {
                throw Exception("El horómetro final no puede ser menor al inicial")
            }

            // 3. Preparar entidad de Registro de Uso
            val registroUsoEntity = if (esEdicion) {
                val existente = registroUsoDao.obtenerPorUid(registroUsoModel.uid)
                    ?: throw Exception("Registro original no encontrado")
                
                existente.copy(
                    horometroAnterior = registroUsoModel.horometroAnterior,
                    horasUso = registroUsoModel.horasUso,
                    horometroFinal = horometroFinal,
                    trabajoRealizado = registroUsoModel.trabajoRealizado,
                    tipoMovimiento = registroUsoModel.tipoMovimiento,
                    obra = registroUsoModel.obra,
                    contratista = registroUsoModel.contratista,
                    ubicacion = registroUsoModel.ubicacion,
                    modificadoPorUid = user?.uid,
                    modificadoPorNombre = user?.displayName ?: "Usuario",
                    fechaUltimaModificacion = DateUtils.obtenerFechaActual(),
                    galonesCombustible = suministroModel?.galonesCombustible ?: 0.0,
                    galonesAceite = suministroModel?.galonesAceite ?: 0.0,
                    tipoCombustible = suministroModel?.tipoCombustible ?: "",
                    tipoCarga = suministroModel?.tipoCarga ?: "",
                    estadoSync = "PENDIENTE_ACTUALIZAR",
                    timestampLocal = System.currentTimeMillis()
                )
            } else {
                RegistroUsoMaquinariaEntity(
                    uid = UUID.randomUUID().toString(),
                    uidMaquinaria = registroUsoModel.uidMaquinaria,
                    codigoMaquinaria = registroUsoModel.codigoMaquinaria,
                    nombreMaquinaria = registroUsoModel.nombreMaquinaria,
                    tipoMaquinaria = registroUsoModel.tipoMaquinaria,
                    uidOperario = user?.uid ?: "",
                    nombreOperario = user?.displayName ?: "Operario",
                    correoOperario = user?.email ?: "",
                    fechaUso = registroUsoModel.fechaUso,
                    horometroAnterior = registroUsoModel.horometroAnterior,
                    horasUso = registroUsoModel.horasUso,
                    horometroFinal = horometroFinal,
                    trabajoRealizado = registroUsoModel.trabajoRealizado,
                    fechaRegistro = DateUtils.obtenerFechaActual(),
                    tipoMovimiento = registroUsoModel.tipoMovimiento,
                    obra = registroUsoModel.obra,
                    contratista = registroUsoModel.contratista,
                    ubicacion = registroUsoModel.ubicacion,
                    galonesCombustible = suministroModel?.galonesCombustible ?: 0.0,
                    galonesAceite = suministroModel?.galonesAceite ?: 0.0,
                    tipoCombustible = suministroModel?.tipoCombustible ?: "",
                    tipoCarga = suministroModel?.tipoCarga ?: "",
                    modificadoPorUid = null,
                    modificadoPorNombre = null,
                    fechaUltimaModificacion = null,
                    estadoSync = "PENDIENTE_CREAR",
                    timestampLocal = System.currentTimeMillis()
                )
            }

            // 4. Actualización condicional del horómetro de la máquina
            var maqActualizada: MaquinariaEntity? = null
            if (!esEdicion) {
                maqActualizada = maqEntity.copy(
                    horometroActual = horometroFinal,
                    fechaActualizacion = DateUtils.obtenerFechaHoraActual(),
                    estadoSync = "PENDIENTE_ACTUALIZAR",
                    timestampLocal = System.currentTimeMillis()
                )
            } else if (horometroFinalOriginal != null && maqEntity.horometroActual == horometroFinalOriginal) {
                maqActualizada = maqEntity.copy(
                    horometroActual = horometroFinal,
                    fechaActualizacion = DateUtils.obtenerFechaHoraActual(),
                    estadoSync = "PENDIENTE_ACTUALIZAR",
                    timestampLocal = System.currentTimeMillis()
                )
            }

            // 5. Preparar entidad de Suministro (Repostaje) si aplica
            // ... (suministro stuff) ...
            var suministroEntity: SuministroEntity? = null
            if (suministroModel != null && (suministroModel.galonesCombustible > 0 || suministroModel.galonesAceite > 0)) {
                suministroEntity = SuministroEntity(
                    uid = if (esEdicion) {
                        suministroDao.obtenerPorRegistroUso(registroUsoEntity.uid)?.uid ?: UUID.randomUUID().toString()
                    } else UUID.randomUUID().toString(),
                    uidMaquinaria = registroUsoEntity.uidMaquinaria,
                    fecha = registroUsoEntity.fechaUso,
                    horometroSuministro = registroUsoEntity.horometroFinal,
                    tipoCarga = suministroModel.tipoCarga,
                    tipoCombustible = suministroModel.tipoCombustible,
                    galonesCombustible = suministroModel.galonesCombustible,
                    galonesAceite = suministroModel.galonesAceite,
                    uidRegistroUso = registroUsoEntity.uid,
                    estadoSync = if (esEdicion) "PENDIENTE_ACTUALIZAR" else "PENDIENTE_CREAR",
                    timestampLocal = System.currentTimeMillis()
                )
            }

            // 5.5 Lógica de Mantenimiento Preventivo Automático
            var solicitudEntity: SolicitudMantenimientoEntity? = null
            if (maqActualizada != null) {
                val horasDesdeUltimo = maqActualizada.horometroActual - maqActualizada.horometroUltimoMantenimiento
                val horasRestantes = maqActualizada.intervaloMantenimientoHoras - horasDesdeUltimo
                
                if (horasRestantes <= 20) {
                    // Verificar si ya existe un mantenimiento activo o una solicitud pendiente
                    val mantenimientoActivo = maintenanceDao.obtenerMantenimientoActivoPorMaquinaria(maqActualizada.uid)
                    val solicitudExistente = solicitudDao.obtenerPendientePorMaquinaria(maqActualizada.uid)
                    
                    if (mantenimientoActivo == null && solicitudExistente == null) {
                        val motivo = if (horasRestantes <= 0) 
                            "ALERTA: Maquinaria excedió el límite de mantenimiento (${(-horasRestantes).toInt()} h de exceso)." 
                            else "Mantenimiento preventivo próximo (faltan ${horasRestantes.toInt()} h)."
                        
                        val nuevaSolicitud = SolicitudMantenimientoModel(
                            uid = UUID.randomUUID().toString(),
                            uidMaquinaria = maqActualizada.uid,
                            codigoMaquinaria = maqActualizada.codigoMaquinaria,
                            nombreMaquinaria = maqActualizada.nombre,
                            tipoMaquinaria = maqActualizada.tipo,
                            uidOperario = user?.uid ?: "",
                            nombreOperario = user?.displayName ?: "Operario",
                            correoOperario = user?.email ?: "",
                            horometroActual = maqActualizada.horometroActual,
                            horometroUltimoMantenimiento = maqActualizada.horometroUltimoMantenimiento,
                            intervaloMantenimientoHoras = maqActualizada.intervaloMantenimientoHoras,
                            horasDesdeUltimoMantenimiento = horasDesdeUltimo,
                            horasRestantes = horasRestantes,
                            motivo = motivo,
                            fechaRegistro = DateUtils.obtenerFechaActual(),
                            origen = "AUTO_HOROMETRO"
                        )
                        solicitudEntity = nuevaSolicitud.aEntity("PENDIENTE_CREAR", System.currentTimeMillis())
                    }
                }
            }

            // 6. Persistencia Atómica en Room
            registroUsoDao.registrarUsoMaquinariaLocal(
                registroUsoEntity,
                maqActualizada,
                suministroEntity,
                solicitudEntity,
                if (suministroEntity == null && esEdicion) registroUsoEntity.uid else null
            )

            // 7. Actualización inmediata de Maquinaria en Firestore (Crítico para evitar regresión de horómetro)
            if (maqActualizada != null) {
                try {
                    db.collection("maquinarias").document(maqActualizada.uid).set(maqActualizada.aModel()).await()
                    maquinariaDao.marcarComoSincronizado(maqActualizada.uid)
                } catch (e: Exception) {
                    // Si falla el envío inmediato, el Worker se encargará después
                }
            }

            SincronizacionMaquinariaWorker.encolar(appContext)
            SincronizacionRegistroUsoMaquinariaWorker.encolar(appContext)
            if (solicitudEntity != null) {
                SincronizacionSolicitudMantenimientoWorker.encolar(appContext)
            }

            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al registrar uso localmente")
        }
    }

    suspend fun obtenerSuministroAsociado(uidRegistroUso: String): SuministroModel? {
        return suministroDao.obtenerPorRegistroUso(uidRegistroUso)?.aModel()
    }

    fun obtenerTiposCombustible(): LiveData<List<String>> {
        return categoriaDao.obtenerActivasPorTipoObservable("COMBUSTIBLE").map { lista ->
            lista.map { it.nombre }
        }
    }

    suspend fun obtenerTodosLosRegistros(): List<RegistroUsoMaquinariaModel> {
        return registroUsoDao.obtenerTodos().map { it.aModel() }
    }

    suspend fun obtenerTodasLasMaquinarias(): List<MaquinariaModel> {
        return maquinariaDao.obtenerTodas().map { it.aModel() }
    }

    suspend fun obtenerTodosLosSuministros(): List<SuministroModel> {
        return suministroDao.obtenerTodos().map { it.aModel() }
    }

    suspend fun obtenerHorasPorOperarioYMes(): List<RendimientoOperarioMes> {
        val registros = registroUsoDao.obtenerTodos()
        return registros.groupBy { it.nombreOperario to it.fechaUso.take(7) }
            .map { (key, group) ->
                RendimientoOperarioMes(
                    nombreOperario = key.first,
                    mes = key.second,
                    totalHoras = group.sumOf { it.horasUso }
                )
            }
            .sortedWith(compareByDescending<RendimientoOperarioMes> { it.mes }.thenByDescending { it.totalHoras })
    }

    // ===================================================================
    // SINCRONIZACIÓN CON FIRESTORE
    // ===================================================================

    suspend fun sincronizarPendientesConFirestore() {
        val pendientes = registroUsoDao.obtenerPendientesDeSincronizar()
        for (entity in pendientes) {
            try {
                db.collection("registros_uso_maquinaria").document(entity.uid).set(entity.aModel()).await()
                registroUsoDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) { }
        }

        val suministrosPendientes = suministroDao.obtenerPendientesDeSincronizar()
        for (entity in suministrosPendientes) {
            try {
                db.collection("suministros").document(entity.uid).set(entity.aModel()).await()
                suministroDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) { }
        }
    }

    suspend fun descargarCambiosDeFirestore() {
        try {
            val snapshot = db.collection("registros_uso_maquinaria").get().await()
            val remotos = snapshot.toObjects(RegistroUsoMaquinariaModel::class.java)
            for (modelo in remotos) {
                registroUsoDao.insertarOActualizar(modelo.aEntity("SINCRONIZADO", System.currentTimeMillis()))
            }

            val snapshotSuministros = db.collection("suministros").get().await()
            val remotosSuministros = snapshotSuministros.toObjects(SuministroModel::class.java)
            for (modelo in remotosSuministros) {
                suministroDao.insertarOActualizar(modelo.aEntity("SINCRONIZADO", System.currentTimeMillis()))
            }
        } catch (e: Exception) { }
    }

    fun iniciarEscuchaBitacora() {
        db.collection("registros_uso_maquinaria").addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots.documentChanges) {
                    val modelo = doc.document.toObject(RegistroUsoMaquinariaModel::class.java)
                    registroUsoDao.insertarOActualizar(modelo.aEntity("SINCRONIZADO", System.currentTimeMillis()))
                }
            }
        }

        db.collection("suministros").addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots.documentChanges) {
                    val modelo = doc.document.toObject(SuministroModel::class.java)
                    suministroDao.insertarOActualizar(modelo.aEntity("SINCRONIZADO", System.currentTimeMillis()))
                }
            }
        }
    }

    // =======================================================================
    // MAPPERS
    // =======================================================================

    private fun RegistroUsoMaquinariaEntity.aModel() = RegistroUsoMaquinariaModel(
        uid = uid, uidMaquinaria = uidMaquinaria, codigoMaquinaria = codigoMaquinaria,
        nombreMaquinaria = nombreMaquinaria, tipoMaquinaria = tipoMaquinaria,
        uidOperario = uidOperario, nombreOperario = nombreOperario, correoOperario = correoOperario,
        fechaUso = fechaUso, horometroAnterior = horometroAnterior, horasUso = horasUso,
        horometroFinal = horometroFinal, trabajoRealizado = trabajoRealizado, fechaRegistro = fechaRegistro,
        tipoMovimiento = tipoMovimiento, obra = obra, contratista = contratista, ubicacion = ubicacion,
        modificadoPorUid = modificadoPorUid, modificadoPorNombre = modificadoPorNombre,
        fechaUltimaModificacion = fechaUltimaModificacion,
        galonesCombustible = galonesCombustible, galonesAceite = galonesAceite,
        tipoCombustible = tipoCombustible, tipoCarga = tipoCarga
    )

    private fun RegistroUsoMaquinariaModel.aEntity(estadoSync: String, timestampLocal: Long) = RegistroUsoMaquinariaEntity(
        uid = uid, uidMaquinaria = uidMaquinaria, codigoMaquinaria = codigoMaquinaria,
        nombreMaquinaria = nombreMaquinaria, tipoMaquinaria = tipoMaquinaria,
        uidOperario = uidOperario, nombreOperario = nombreOperario, correoOperario = correoOperario,
        fechaUso = fechaUso, horometroAnterior = horometroAnterior, horasUso = horasUso,
        horometroFinal = horometroFinal, trabajoRealizado = trabajoRealizado, fechaRegistro = fechaRegistro,
        tipoMovimiento = tipoMovimiento, obra = obra, contratista = contratista, ubicacion = ubicacion,
        modificadoPorUid = modificadoPorUid, modificadoPorNombre = modificadoPorNombre,
        fechaUltimaModificacion = fechaUltimaModificacion,
        galonesCombustible = galonesCombustible, galonesAceite = galonesAceite,
        tipoCombustible = tipoCombustible, tipoCarga = tipoCarga,
        estadoSync = estadoSync, timestampLocal = timestampLocal
    )

    private fun SuministroEntity.aModel() = SuministroModel(
        uid = uid, uidMaquinaria = uidMaquinaria, fecha = fecha, 
        horometroSuministro = horometroSuministro, tipoCarga = tipoCarga, 
        tipoCombustible = tipoCombustible, galonesCombustible = galonesCombustible, 
        galonesAceite = galonesAceite, uidRegistroUso = uidRegistroUso
    )

    private fun SuministroModel.aEntity(estadoSync: String, timestampLocal: Long) = SuministroEntity(
        uid = uid, uidMaquinaria = uidMaquinaria, fecha = fecha,
        horometroSuministro = horometroSuministro, tipoCarga = tipoCarga,
        tipoCombustible = tipoCombustible, galonesCombustible = galonesCombustible,
        galonesAceite = galonesAceite, uidRegistroUso = uidRegistroUso,
        estadoSync = estadoSync, timestampLocal = timestampLocal
    )
}

data class RendimientoOperarioMes(
    val nombreOperario: String,
    val mes: String,
    val totalHoras: Double
)
