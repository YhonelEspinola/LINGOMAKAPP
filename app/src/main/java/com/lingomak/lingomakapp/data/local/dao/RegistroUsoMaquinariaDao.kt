package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lingomak.lingomakapp.data.local.entity.MaquinariaEntity
import com.lingomak.lingomakapp.data.local.entity.RegistroUsoMaquinariaEntity
import com.lingomak.lingomakapp.data.local.entity.SolicitudMantenimientoEntity
import com.lingomak.lingomakapp.data.local.entity.SuministroEntity

@Dao
interface RegistroUsoMaquinariaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(registro: RegistroUsoMaquinariaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun actualizarMaquinaria(maquinaria: MaquinariaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizarSolicitud(solicitud: SolicitudMantenimientoEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizarSuministro(suministro: SuministroEntity)

    @Query("DELETE FROM suministros WHERE uid = :suministroUid")
    suspend fun eliminarSuministroLocal(suministroUid: String)

    @Transaction
    suspend fun registrarUsoMaquinariaLocal(
        registro: RegistroUsoMaquinariaEntity,
        maquinaria: MaquinariaEntity?,
        solicitud: SolicitudMantenimientoEntity?,
        suministro: SuministroEntity?,
        suministroUidAEliminar: String? = null
    ) {
        insertarOActualizar(registro)
        if (maquinaria != null) {
            actualizarMaquinaria(maquinaria)
        }
        if (solicitud != null) {
            insertarOActualizarSolicitud(solicitud)
        }
        if (suministro != null) {
            insertarOActualizarSuministro(suministro)
        }
        if (suministroUidAEliminar != null) {
            eliminarSuministroLocal(suministroUidAEliminar)
        }
    }

    @Query("SELECT * FROM registros_uso_maquinaria ORDER BY timestampLocal DESC")
    suspend fun obtenerTodos(): List<RegistroUsoMaquinariaEntity>

    @Query("SELECT * FROM registros_uso_maquinaria WHERE uid = :uid LIMIT 1")
    suspend fun obtenerPorUid(uid: String): RegistroUsoMaquinariaEntity?

    @Query("SELECT * FROM registros_uso_maquinaria WHERE estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSincronizar(): List<RegistroUsoMaquinariaEntity>

    @Query("UPDATE registros_uso_maquinaria SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid")
    suspend fun marcarComoSincronizado(uid: String)

    @Query("SELECT COUNT(*) FROM registros_uso_maquinaria WHERE estadoSync != 'SINCRONIZADO'")
    fun obtenerPendientesDeSincronizarCount(): LiveData<Int>

    @Query("SELECT * FROM registros_uso_maquinaria WHERE uidOperario = :uid ORDER BY timestampLocal DESC LIMIT 1")
    fun obtenerUltimoUsoObservable(uid: String): LiveData<RegistroUsoMaquinariaEntity?>
}
