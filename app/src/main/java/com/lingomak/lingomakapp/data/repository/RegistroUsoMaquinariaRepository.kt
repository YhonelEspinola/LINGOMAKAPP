package com.lingomak.lingomakapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.MaquinariaEntity
import com.lingomak.lingomakapp.data.local.entity.RegistroUsoMaquinariaEntity
import com.lingomak.lingomakapp.data.local.entity.SolicitudMantenimientoEntity
import com.lingomak.lingomakapp.data.local.entity.SuministroEntity
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.RegistroUsoMaquinariaModel
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel
import com.lingomak.lingomakapp.data.model.SuministroModel
import com.lingomak.lingomakapp.utils.DateUtils
import com.lingomak.lingomakapp.data.worker.SincronizacionMaquinariaWorker
import com.lingomak.lingomakapp.data.worker.SincronizacionRegistroUsoMaquinariaWorker
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class RendimientoOperarioMes(
    val nombreOperario: String,
    val mes: String,
    val totalHoras: Double
)

class RegistroUsoMaquinariaRepository(context: Context) {

    private val db = FirebaseFirestore.getInstance()

    private val coleccionMaquinaria = "maquinarias"
    private val coleccionRegistrosUso = "registros_uso_maquinaria"
    private val coleccionSolicitudes = "solicitudes_mantenimiento"

    private val database = AppDatabase.getInstance(context)
    private val registroUsoDao = database.registroUsoMaquinariaDao()
    private val maquinariaDao = database.maquinariaDao()
    private val solicitudDao = database.solicitudMantenimientoDao()
    private val suministroDao = database.suministroDao()
    private val categoriaDao = database.categoriaDao()
    private val appContext = context.applicationContext

    suspend fun registrarUsoMaquinaria(
        registroUsoModel: RegistroUsoMaquinariaModel,
        suministroModel: SuministroModel?,
        esEdicion: Boolean,
        horometroFinalOriginal: Double? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val maqEntity = maquinariaDao.obtenerPorUid(registroUsoModel.uidMaquinaria)
                ?: throw Exception("No se encontró la maquinaria localmente")

            // 6.1 Validación de horómetros
            if (registroUsoModel.horometroFinal <= registroUsoModel.horometroAnterior) {
                throw Exception("El horómetro final debe ser mayor al anterior")
            }

            val horometroFinal = registroUsoModel.horometroFinal
            val horasDesdeUltimoMantenimiento = horometroFinal - maqEntity.horometroUltimoMantenimiento
            val horasRestantes = maqEntity.intervaloMantenimientoHoras - horasDesdeUltimoMantenimiento

            // Fix 3.8: Manejo de autoría vs modificación
            val user = FirebaseAuth.getInstance().currentUser
            val registroUsoEntity = if (esEdicion) {
                val existente = registroUsoDao.obtenerPorUid(registroUsoModel.uid) 
                    ?: throw Exception("No se encontró el registro original")
                
                RegistroUsoMaquinariaEntity(
                    uid = existente.uid,
                    uidMaquinaria = existente.uidMaquinaria,
                    codigoMaquinaria = existente.codigoMaquinaria,
                    nombreMaquinaria = existente.nombreMaquinaria,
                    tipoMaquinaria = existente.tipoMaquinaria,
                    uidOperario = existente.uidOperario, // Preservar original
                    nombreOperario = existente.nombreOperario, // Preservar original
                    correoOperario = existente.correoOperario, // Preservar original
                    fechaUso = registroUsoModel.fechaUso,
                    horometroAnterior = registroUsoModel.horometroAnterior,
                    horasUso = registroUsoModel.horasUso,
                    horometroFinal = registroUsoModel.horometroFinal,
                    trabajoRealizado = registroUsoModel.trabajoRealizado,
                    fechaRegistro = existente.fechaRegistro, // Preservar original
                    tipoMovimiento = registroUsoModel.tipoMovimiento,
                    obra = registroUsoModel.obra,
                    contratista = registroUsoModel.contratista,
                    ubicacion = registroUsoModel.ubicacion,
                    modificadoPorUid = user?.uid,
                    modificadoPorNombre = user?.displayName ?: "Usuario",
                    fechaUltimaModificacion = DateUtils.obtenerFechaActual(),
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
                    horometroFinal = registroUsoModel.horometroFinal,
                    trabajoRealizado = registroUsoModel.trabajoRealizado,
                    fechaRegistro = DateUtils.obtenerFechaActual(),
                    tipoMovimiento = registroUsoModel.tipoMovimiento,
                    obra = registroUsoModel.obra,
                    contratista = registroUsoModel.contratista,
                    ubicacion = registroUsoModel.ubicacion,
                    modificadoPorUid = null,
                    modificadoPorNombre = null,
                    fechaUltimaModificacion = null,
                    estadoSync = "PENDIENTE_CREAR",
                    timestampLocal = System.currentTimeMillis()
                )
            }

            // 6.3 Actualización condicional del horómetro de la máquina
            var maqActualizada: MaquinariaEntity? = null
            if (!esEdicion) {
                maqActualizada = maqEntity.copy(
                    horometroActual = horometroFinal,
                    fechaActualizacion = DateUtils.obtenerFechaActual(),
                    estadoSync = "PENDIENTE_ACTUALIZAR",
                    timestampLocal = System.currentTimeMillis()
                )
            } else if (horometroFinalOriginal != null && maqEntity.horometroActual == horometroFinalOriginal) {
                maqActualizada = maqEntity.copy(
                    horometroActual = horometroFinal,
                    fechaActualizacion = DateUtils.obtenerFechaActual(),
                    estadoSync = "PENDIENTE_ACTUALIZAR",
                    timestampLocal = System.currentTimeMillis()
                )
            }

            // 6.2 Alertas de mantenimiento
            var solicitudEntity: SolicitudMantenimientoEntity? = null
            if (horasRestantes <= 20) {
                val solicitudExistente = solicitudDao.obtenerPendientePorMaquinaria(maqEntity.uid)
                if (solicitudExistente == null) {
                    solicitudEntity = SolicitudMantenimientoEntity(
                        uid = UUID.randomUUID().toString(),
                        uidMaquinaria = maqEntity.uid,
                        codigoMaquinaria = maqEntity.codigoMaquinaria,
                        nombreMaquinaria = maqEntity.nombre,
                        tipoMaquinaria = maqEntity.tipo,
                        uidOperario = registroUsoEntity.uidOperario,
                        nombreOperario = registroUsoEntity.nombreOperario,
                        correoOperario = registroUsoEntity.correoOperario,
                        horometroActual = horometroFinal,
                        horometroUltimoMantenimiento = maqEntity.horometroUltimoMantenimiento,
                        intervaloMantenimientoHoras = maqEntity.intervaloMantenimientoHoras,
                        horasDesdeUltimoMantenimiento = horasDesdeUltimoMantenimiento,
                        horasRestantes = horasRestantes,
                        motivo = obtenerMotivoSolicitud(horasRestantes.toInt()),
                        estadoSolicitud = "PENDIENTE_APROBACION",
                        origen = "HOROMETRO_OPERARIO",
                        fechaSugerida = DateUtils.obtenerFechaActual(),
                        fechaRegistro = DateUtils.obtenerFechaActual(),
                        estadoSync = "PENDIENTE_CREAR",
                        timestampLocal = System.currentTimeMillis()
                    )
                } else {
                    solicitudEntity = solicitudExistente.copy(
                        horometroActual = horometroFinal,
                        horasDesdeUltimoMantenimiento = horasDesdeUltimoMantenimiento,
                        horasRestantes = horasRestantes,
                        motivo = obtenerMotivoSolicitud(horasRestantes.toInt()),
                        fechaSugerida = DateUtils.obtenerFechaActual(),
                        estadoSync = "PENDIENTE_ACTUALIZAR",
                        timestampLocal = System.currentTimeMillis()
                    )
                }
            }

            // 6. Repostaje (Suministro)
            var suministroEntity: SuministroEntity? = null
            var suministroUidAEliminar: String? = null

            if (suministroModel != null) {
                if (suministroModel.tipoCarga == "Completa" && maqEntity.capacidadTanqueGls != null) {
                    if (suministroModel.galonesCombustible > maqEntity.capacidadTanqueGls) {
                        throw Exception("Los galones de combustible superan la capacidad del tanque (${maqEntity.capacidadTanqueGls} Gls)")
                    }
                }

                suministroEntity = SuministroEntity(
                    uid = if (suministroModel.uid.isBlank()) UUID.randomUUID().toString() else suministroModel.uid,
                    uidMaquinaria = maqEntity.uid,
                    fecha = suministroModel.fecha,
                    horometroSuministro = suministroModel.horometroSuministro,
                    tipoCarga = suministroModel.tipoCarga,
                    tipoCombustible = suministroModel.tipoCombustible,
                    galonesCombustible = suministroModel.galonesCombustible,
                    galonesAceite = suministroModel.galonesAceite,
                    uidRegistroUso = registroUsoEntity.uid,
                    estadoSync = "PENDIENTE_CREAR",
                    timestampLocal = System.currentTimeMillis()
                )
            } else if (esEdicion) {
                val existente = suministroDao.obtenerPorRegistroUso(registroUsoEntity.uid)
                if (existente != null) {
                    suministroUidAEliminar = existente.uid
                }
            }

            registroUsoDao.registrarUsoMaquinariaLocal(
                registroUsoEntity,
                maqActualizada,
                solicitudEntity,
                suministroEntity,
                suministroUidAEliminar
            )

            SincronizacionRegistroUsoMaquinariaWorker.encolar(appContext)
            if (maqActualizada != null) SincronizacionMaquinariaWorker.encolar(appContext)

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
        val pendientesUso = registroUsoDao.obtenerPendientesDeSincronizar()
        for (entity in pendientesUso) {
            try {
                db.collection(coleccionRegistrosUso).document(entity.uid).set(entity.aModel()).await()
                registroUsoDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) { /* Reintento */ }
        }

        val pendientesSolicitud = solicitudDao.obtenerPendientesDeSincronizar()
        for (entity in pendientesSolicitud) {
            try {
                db.collection(coleccionSolicitudes).document(entity.uid).set(entity.aModel()).await()
                solicitudDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) { /* Reintento */ }
        }

        val pendientesSuministro = suministroDao.obtenerPendientesDeSincronizar()
        for (entity in pendientesSuministro) {
            try {
                db.collection("suministros").document(entity.uid).set(entity.aModel()).await()
                suministroDao.marcarComoSincronizado(entity.uid)
            } catch (e: Exception) { /* Reintento */ }
        }
    }

    suspend fun descargarCambiosDeFirestore() {
        try {
            // 1. Descargar Registros de Uso
            val snapshotUso = db.collection(coleccionRegistrosUso).get().await()
            val remotosUso = snapshotUso.toObjects(RegistroUsoMaquinariaModel::class.java)
            val uidsUso = remotosUso.map { it.uid }
            
            if (uidsUso.isEmpty()) {
                registroUsoDao.eliminarSincronizados()
            } else {
                registroUsoDao.eliminarSincronizadosNoPresentes(uidsUso)
                val entitiesUso = remotosUso.map { it.aEntity("SINCRONIZADO", System.currentTimeMillis()) }
                registroUsoDao.insertarLista(entitiesUso)
            }

            // 2. Descargar Solicitudes de Mantenimiento (Solo las pendientes como antes)
            val snapshotSol = db.collection(coleccionSolicitudes)
                .whereEqualTo("estadoSolicitud", "PENDIENTE_APROBACION")
                .get().await()
            val remotosSol = snapshotSol.toObjects(SolicitudMantenimientoModel::class.java)
            val uidsSol = remotosSol.map { it.uid }

            if (uidsSol.isEmpty()) {
                solicitudDao.eliminarSincronizados()
            } else {
                solicitudDao.eliminarSincronizadosNoPresentes(uidsSol)
                val entitiesSol = remotosSol.map { it.aEntity("SINCRONIZADO", System.currentTimeMillis()) }
                solicitudDao.insertarLista(entitiesSol)
            }

            // 3. Descargar Suministros (Combustible)
            val snapshotSum = db.collection("suministros").get().await()
            val remotosSum = snapshotSum.toObjects(SuministroModel::class.java)
            val uidsSum = remotosSum.map { it.uid }

            if (uidsSum.isEmpty()) {
                suministroDao.eliminarSincronizados()
            } else {
                suministroDao.eliminarSincronizadosNoPresentes(uidsSum)
                val entitiesSum = remotosSum.map { it.aEntity("SINCRONIZADO", System.currentTimeMillis()) }
                suministroDao.insertarLista(entitiesSum)
            }

        } catch (e: Exception) {
            android.util.Log.e("RegistroUsoRepo", "Error al descargar cambios: ${e.message}")
        }
    }

    private fun obtenerMotivoSolicitud(horasRestantes: Int): String {
        return when {
            horasRestantes < 0 -> {
                val horasExcedidas = kotlin.math.abs(horasRestantes)
                "La maquinaria superó el intervalo de mantenimiento por $horasExcedidas horas."
            }
            horasRestantes == 0 -> "La maquinaria alcanzó exactamente el intervalo de mantenimiento preventivo."
            else -> "La maquinaria está próxima a cumplir el intervalo de mantenimiento. Faltan $horasRestantes horas."
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
        fechaUltimaModificacion = fechaUltimaModificacion
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
        estadoSync = estadoSync, timestampLocal = timestampLocal
    )

    private fun SolicitudMantenimientoEntity.aModel() = SolicitudMantenimientoModel(
        uid = uid, uidMaquinaria = uidMaquinaria, codigoMaquinaria = codigoMaquinaria,
        nombreMaquinaria = nombreMaquinaria, tipoMaquinaria = tipoMaquinaria,
        uidOperario = uidOperario, nombreOperario = nombreOperario, correoOperario = correoOperario,
        horometroActual = horometroActual, horometroUltimoMantenimiento = horometroUltimoMantenimiento,
        intervaloMantenimientoHoras = intervaloMantenimientoHoras,
        horasDesdeUltimoMantenimiento = horasDesdeUltimoMantenimiento, horasRestantes = horasRestantes,
        motivo = motivo, estadoSolicitud = estadoSolicitud, origen = origen,
        fechaSugerida = fechaSugerida, fechaRegistro = fechaRegistro,
        revisadoPor = revisadoPor, fechaRevision = fechaRevision,
        motivoRechazo = motivoRechazo, uidMantenimientoGenerado = uidMantenimientoGenerado
    )

    private fun SolicitudMantenimientoModel.aEntity(estadoSync: String, timestampLocal: Long) = SolicitudMantenimientoEntity(
        uid = uid, uidMaquinaria = uidMaquinaria, codigoMaquinaria = codigoMaquinaria,
        nombreMaquinaria = nombreMaquinaria, tipoMaquinaria = tipoMaquinaria,
        uidOperario = uidOperario, nombreOperario = nombreOperario, correoOperario = correoOperario,
        horometroActual = horometroActual, horometroUltimoMantenimiento = horometroUltimoMantenimiento,
        intervaloMantenimientoHoras = intervaloMantenimientoHoras,
        horasDesdeUltimoMantenimiento = horasDesdeUltimoMantenimiento, horasRestantes = horasRestantes,
        motivo = motivo, estadoSolicitud = estadoSolicitud, origen = origen,
        fechaSugerida = fechaSugerida, fechaRegistro = fechaRegistro,
        revisadoPor = revisadoPor, fechaRevision = fechaRevision,
        motivoRechazo = motivoRechazo, uidMantenimientoGenerado = uidMantenimientoGenerado,
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
