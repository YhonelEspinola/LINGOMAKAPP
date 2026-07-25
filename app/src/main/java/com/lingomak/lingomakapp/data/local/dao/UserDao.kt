package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lingomak.lingomakapp.data.local.entity.UserEntity

@Dao
interface UserDao {

    @Query("SELECT * FROM usuarios ORDER BY nombre ASC")
    fun obtenerTodosObservable(): LiveData<List<UserEntity>>

    @Query("SELECT * FROM usuarios WHERE rol = 'OPERARIO' AND estado = 'ACTIVO' ORDER BY nombre ASC")
    fun obtenerOperariosActivosObservable(): LiveData<List<UserEntity>>

    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    suspend fun obtenerPorUid(uid: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(usuario: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLista(usuarios: List<UserEntity>)

    @Query("DELETE FROM usuarios WHERE estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarSincronizados()

    @Query("DELETE FROM usuarios WHERE estadoSync = 'SINCRONIZADO' AND uid NOT IN (:uidsFirestore)")
    suspend fun eliminarSincronizadosNoPresentes(uidsFirestore: List<String>)

    @Query("SELECT * FROM usuarios WHERE estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSincronizar(): List<UserEntity>

    @Query("UPDATE usuarios SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid")
    suspend fun marcarComoSincronizado(uid: String)
}
