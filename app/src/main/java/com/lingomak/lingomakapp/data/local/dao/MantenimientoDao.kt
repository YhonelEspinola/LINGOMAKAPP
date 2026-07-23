package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lingomak.lingomakapp.data.local.entity.MaquinariaEntity
import com.lingomak.lingomakapp.data.local.entity.MantenimientoEntity

@Dao
interface MantenimientoDao {

    @Query("SELECT * FROM mantenimientos ORDER BY fechaProgramada DESC")
    fun obtenerTodosObservable(): LiveData<List<MantenimientoEntity>>

    @Query("SELECT * FROM mantenimientos")
    suspend fun obtenerTodos(): List<MantenimientoEntity>

    @Query("SELECT * FROM mantenimientos WHERE responsableUid = :userUid OR responsableUid = 'TODOS' ORDER BY fechaProgramada DESC")
    fun obtenerAsignadosObservable(userUid: String): LiveData<List<MantenimientoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(mantenimiento: MantenimientoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLista(lista: List<MantenimientoEntity>)

    @Query("SELECT * FROM mantenimientos WHERE uid = :uid LIMIT 1")
    suspend fun obtenerPorUid(uid: String): MantenimientoEntity?

    @Query("SELECT * FROM mantenimientos WHERE estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSincronizar(): List<MantenimientoEntity>

    @Query("UPDATE mantenimientos SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid")
    suspend fun marcarComoSincronizado(uid: String)

    @Query("UPDATE mantenimientos SET estado = :nuevoEstado, actualizadoPor = :actualizadoPor, fechaActualizacion = :fechaActualizacion, estadoSync = 'PENDIENTE_ACTUALIZAR', timestampLocal = :timestamp WHERE uid = :uid")
    suspend fun cambiarEstadoLocal(uid: String, nuevoEstado: String, actualizadoPor: String, fechaActualizacion: String, timestamp: Long)

    @Query("DELETE FROM mantenimientos WHERE estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarSincronizados()

    @Query("DELETE FROM mantenimientos WHERE estadoSync = 'SINCRONIZADO' AND uid NOT IN (:uidsMantener)")
    suspend fun eliminarSincronizadosNoPresentes(uidsMantener: List<String>)

    @Transaction
    suspend fun iniciarMantenimientoLocal(
        mantenimiento: MantenimientoEntity,
        maquinaria: MaquinariaEntity
    ) {
        insertarOActualizar(mantenimiento)
        insertarMaquinaria(maquinaria)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarMaquinaria(maquinaria: MaquinariaEntity)

    @Query("SELECT COUNT(*) FROM mantenimientos WHERE estadoSync != 'SINCRONIZADO'")
    fun obtenerPendientesDeSincronizarCount(): LiveData<Int>
}