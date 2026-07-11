package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lingomak.lingomakapp.data.local.entity.MaquinariaEntity

@Dao
interface MaquinariaDao {

    @Query("SELECT * FROM maquinarias ORDER BY nombre ASC")
    fun obtenerTodasObservable(): LiveData<List<MaquinariaEntity>>

    @Query("SELECT * FROM maquinarias WHERE uid = :uid")
    suspend fun obtenerPorUid(uid: String): MaquinariaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(maquinaria: MaquinariaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLista(maquinarias: List<MaquinariaEntity>)

    @Query("DELETE FROM maquinarias WHERE estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarTodasSincronizadas()

    @Query("DELETE FROM maquinarias WHERE estadoSync = 'SINCRONIZADO' AND uid NOT IN (:uidsRemotos)")
    suspend fun eliminarSincronizadosNoPresentes(uidsRemotos: List<String>)

    @Query("SELECT * FROM maquinarias WHERE estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSincronizar(): List<MaquinariaEntity>

    @Query("UPDATE maquinarias SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid")
    suspend fun marcarComoSincronizado(uid: String)
}