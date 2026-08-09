package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lingomak.lingomakapp.data.local.entity.SuministroEntity

@Dao
interface SuministroDao {

    @Query("SELECT * FROM suministros WHERE uidRegistroUso = :uidRegistroUso LIMIT 1")
    suspend fun obtenerPorRegistroUso(uidRegistroUso: String): SuministroEntity?

    @Query("SELECT * FROM suministros WHERE uidRegistroUso = :uidRegistroUso LIMIT 1")
    fun obtenerPorRegistroUsoObservable(uidRegistroUso: String): LiveData<SuministroEntity?>

    @Query("SELECT * FROM suministros")
    suspend fun obtenerTodos(): List<SuministroEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(suministro: SuministroEntity)

    @Delete
    suspend fun eliminar(suministro: SuministroEntity)

    @Query("DELETE FROM suministros WHERE uid = :uid")
    suspend fun eliminarPorUid(uid: String)

    @Query("SELECT * FROM suministros WHERE uidMaquinaria = :uidMaquinaria AND fecha >= :fechaInicio")
    suspend fun obtenerPorMaquinaYRango(uidMaquinaria: String, fechaInicio: String): List<SuministroEntity>

    @Query("SELECT * FROM suministros WHERE estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSincronizar(): List<SuministroEntity>

    @Query("UPDATE suministros SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid")
    suspend fun marcarComoSincronizado(uid: String)

    @Query("DELETE FROM suministros WHERE estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarSincronizados()

    @Query("DELETE FROM suministros WHERE uid NOT IN (:uids) AND estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarSincronizadosNoPresentes(uids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLista(suministros: List<SuministroEntity>)
}
