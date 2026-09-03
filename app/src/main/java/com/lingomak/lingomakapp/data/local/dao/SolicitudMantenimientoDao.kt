package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lingomak.lingomakapp.data.local.entity.SolicitudMantenimientoEntity

@Dao
interface SolicitudMantenimientoDao {

    @Query("SELECT * FROM solicitudes_mantenimiento ORDER BY timestampLocal DESC")
    fun obtenerTodasObservable(): LiveData<List<SolicitudMantenimientoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(solicitud: SolicitudMantenimientoEntity)

    @Query("SELECT * FROM solicitudes_mantenimiento WHERE uidMaquinaria = :uidMaquinaria AND estadoSolicitud = 'PENDIENTE_APROBACION' LIMIT 1")
    suspend fun obtenerPendientePorMaquinaria(uidMaquinaria: String): SolicitudMantenimientoEntity?

    @Query("SELECT * FROM solicitudes_mantenimiento WHERE uid = :uid LIMIT 1")
    suspend fun obtenerPorUid(uid: String): SolicitudMantenimientoEntity?

    @Query("SELECT * FROM solicitudes_mantenimiento WHERE estadoSolicitud = 'PENDIENTE_APROBACION' ORDER BY timestampLocal DESC")
    fun obtenerPendientesObservable(): LiveData<List<SolicitudMantenimientoEntity>>

    @Query("SELECT * FROM solicitudes_mantenimiento WHERE estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSincronizar(): List<SolicitudMantenimientoEntity>

    @Query("UPDATE solicitudes_mantenimiento SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid")
    suspend fun marcarComoSincronizado(uid: String)
    
    @Query("DELETE FROM solicitudes_mantenimiento WHERE estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarSincronizados()
}
