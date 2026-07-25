package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lingomak.lingomakapp.data.local.entity.AlertaEntity

@Dao
interface AlertaDao {

    @Query("SELECT * FROM alertas ORDER BY fecha DESC")
    fun obtenerTodasObservable(): LiveData<List<AlertaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLista(alertas: List<AlertaEntity>)

    @Query("DELETE FROM alertas")
    suspend fun limpiarAlertas()
    
    @Query("SELECT COUNT(*) FROM alertas")
    fun obtenerTotalAlertasObservable(): LiveData<Int>
}
