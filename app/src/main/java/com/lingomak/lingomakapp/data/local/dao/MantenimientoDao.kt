package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lingomak.lingomakapp.data.local.entity.MantenimientoEntity

@Dao
interface MantenimientoDao {

    @Query("SELECT * FROM mantenimientos ORDER BY fechaProgramada DESC")
    fun obtenerTodosObservable(): LiveData<List<MantenimientoEntity>>

    @Query("SELECT * FROM mantenimientos WHERE responsableUid = :userUid OR responsableUid = 'TODOS' ORDER BY fechaProgramada DESC")
    fun obtenerAsignadosObservable(userUid: String): LiveData<List<MantenimientoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLista(lista: List<MantenimientoEntity>)

    @Query("DELETE FROM mantenimientos WHERE estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarSincronizados()
}