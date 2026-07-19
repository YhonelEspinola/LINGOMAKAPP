package com.lingomak.lingomakapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lingomak.lingomakapp.data.local.entity.MaquinariaEntity
import com.lingomak.lingomakapp.data.local.entity.RegistroUsoMaquinariaEntity
import com.lingomak.lingomakapp.data.local.entity.SolicitudMantenimientoEntity

@Dao
interface RegistroUsoMaquinariaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(registro: RegistroUsoMaquinariaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun actualizarMaquinaria(maquinaria: MaquinariaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizarSolicitud(solicitud: SolicitudMantenimientoEntity)

    @Transaction
    suspend fun registrarUsoMaquinariaLocal(
        registro: RegistroUsoMaquinariaEntity,
        maquinaria: MaquinariaEntity,
        solicitud: SolicitudMantenimientoEntity?
    ) {
        insertarOActualizar(registro)
        actualizarMaquinaria(maquinaria)
        if (solicitud != null) {
            insertarOActualizarSolicitud(solicitud)
        }
    }

    @Query("SELECT * FROM registros_uso_maquinaria WHERE estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSincronizar(): List<RegistroUsoMaquinariaEntity>

    @Query("UPDATE registros_uso_maquinaria SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid")
    suspend fun marcarComoSincronizado(uid: String)
}
