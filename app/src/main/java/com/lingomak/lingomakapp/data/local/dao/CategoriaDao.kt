package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lingomak.lingomakapp.data.local.entity.CategoriaEntity

@Dao
interface CategoriaDao {

    @Query("SELECT * FROM categorias WHERE tipo = :tipo AND estado = 'ACTIVO' ORDER BY nombre ASC")
    fun obtenerActivasPorTipoObservable(tipo: String): LiveData<List<CategoriaEntity>>

    @Query("SELECT * FROM categorias WHERE tipo = :tipo ORDER BY nombre ASC")
    fun obtenerTodasPorTipoObservable(tipo: String): LiveData<List<CategoriaEntity>>

    @Query("SELECT * FROM categorias WHERE uid = :uid LIMIT 1")
    suspend fun obtenerPorUid(uid: String): CategoriaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(categoria: CategoriaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLista(categorias: List<CategoriaEntity>)

    @Query("DELETE FROM categorias WHERE estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarSincronizados()

    @Query("DELETE FROM categorias WHERE estadoSync = 'SINCRONIZADO' AND uid NOT IN (:uidsFirestore)")
    suspend fun eliminarSincronizadosNoPresentes(uidsFirestore: List<String>)

    @Query("SELECT * FROM categorias WHERE estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSincronizar(): List<CategoriaEntity>

    @Query("UPDATE categorias SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid")
    suspend fun marcarComoSincronizado(uid: String)
    
    @Query("SELECT COUNT(*) FROM categorias WHERE estadoSync != 'SINCRONIZADO'")
    fun obtenerPendientesDeSincronizarCount(): LiveData<Int>
}
