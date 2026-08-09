package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lingomak.lingomakapp.data.local.entity.SolicitudMantenimientoEntity

@Dao
interface SolicitudMantenimientoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(solicitud: SolicitudMantenimientoEntity)

    @Query("SELECT * FROM solicitudes_mantenimiento WHERE uid = :uid LIMIT 1")
    suspend fun obtenerPorUid(uid: String): SolicitudMantenimientoEntity?

    @Query("SELECT * FROM solicitudes_mantenimiento WHERE uidMaquinaria = :uidMaquinaria AND estadoSolicitud = 'PENDIENTE_APROBACION' LIMIT 1")
    suspend fun obtenerPendientePorMaquinaria(uidMaquinaria: String): SolicitudMantenimientoEntity?

    @Query("SELECT * FROM solicitudes_mantenimiento WHERE estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSincronizar(): List<SolicitudMantenimientoEntity>

    @Query("UPDATE solicitudes_mantenimiento SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid")
    suspend fun marcarComoSincronizado(uid: String)

    @Query("DELETE FROM solicitudes_mantenimiento WHERE estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarSincronizados()

    @Query("DELETE FROM solicitudes_mantenimiento WHERE uid NOT IN (:uids) AND estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarSincronizadosNoPresentes(uids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLista(solicitudes: List<SolicitudMantenimientoEntity>)

    @Query("SELECT COUNT(*) FROM solicitudes_mantenimiento WHERE estadoSync != 'SINCRONIZADO'")
    fun obtenerPendientesDeSincronizarCount(): LiveData<Int>
}
