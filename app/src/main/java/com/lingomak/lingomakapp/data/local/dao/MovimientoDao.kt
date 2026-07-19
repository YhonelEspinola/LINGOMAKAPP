package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lingomak.lingomakapp.data.local.entity.MovimientoEntity

@Dao
interface MovimientoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(movimiento: MovimientoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLista(movimientos: List<MovimientoEntity>)

    /**
     * Historial observable de un repuesto, ordenado del más reciente
     * al más antiguo. La pantalla de Movimientos se suscribe a esto
     * directamente: un movimiento creado offline aparece al instante
     * en la lista, sin esperar sincronización.
     */
    @Query("SELECT * FROM movimientos WHERE repuestoUid = :repuestoUid ORDER BY fecha DESC")
    fun obtenerPorRepuestoObservable(repuestoUid: String): LiveData<List<MovimientoEntity>>

    @Query("SELECT * FROM movimientos WHERE estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSincronizar(): List<MovimientoEntity>

    @Query("UPDATE movimientos SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid")
    suspend fun marcarComoSincronizado(uid: String)

    @Query("SELECT EXISTS(SELECT 1 FROM movimientos WHERE uid = :uid)")
    suspend fun existe(uid: String): Boolean

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC")
    fun obtenerTodosObservable(): LiveData<List<MovimientoEntity>>

    @Query("DELETE FROM movimientos WHERE estadoSync = 'SINCRONIZADO' AND uid NOT IN (:uidsMantener)")
    suspend fun eliminarSincronizadosNoPresentes(uidsMantener: List<String>)

    @Query("DELETE FROM movimientos WHERE estadoSync = 'SINCRONIZADO' AND repuestoUid = :repuestoUid AND uid NOT IN (:uidsMantener)")
    suspend fun eliminarSincronizadosNoPresentesPorRepuesto(repuestoUid: String, uidsMantener: List<String>)

    @Query("DELETE FROM movimientos WHERE estadoSync = 'SINCRONIZADO' AND repuestoUid = :repuestoUid")
    suspend fun eliminarTodosSincronizadosPorRepuesto(repuestoUid: String)

    @Query("DELETE FROM movimientos WHERE estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarTodosSincronizados()

    @Query("SELECT * FROM movimientos WHERE uid = :uid LIMIT 1")
    suspend fun obtenerPorUid(uid: String): MovimientoEntity?

    @androidx.room.Delete
    suspend fun eliminar(movimiento: MovimientoEntity)

    @androidx.room.Update
    suspend fun actualizar(movimiento: MovimientoEntity)

    @Query(
        """
    SELECT * FROM movimientos 
    WHERE tipo = 'SALIDA' 
    AND fecha >= :fechaInicio
    """
    )
    suspend fun obtenerSalidasDesde(fechaInicio: Long): List<MovimientoEntity>

    @Query(
        """
    SELECT * FROM movimientos
    WHERE fecha >= :fechaInicio
    """
    )
    suspend fun obtenerMovimientosDesde(fechaInicio: Long): List<MovimientoEntity>

    @Query("SELECT * FROM movimientos WHERE ordenMantenimientoUid = :ordenUid")
    suspend fun obtenerPorMantenimiento(ordenUid: String): List<MovimientoEntity>

}
