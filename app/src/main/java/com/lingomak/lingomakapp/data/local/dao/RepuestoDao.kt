package com.lingomak.lingomakapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lingomak.lingomakapp.data.local.entity.RepuestoEntity

@Dao
interface RepuestoDao {

    /**
     * Inserta o reemplaza un repuesto local. Usado tanto al crear/editar
     * offline como al recibir datos sincronizados desde Firestore.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(repuesto: RepuestoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizarLista(repuestos: List<RepuestoEntity>)

    @Update
    suspend fun actualizar(repuesto: RepuestoEntity)

    /**
     * Lista completa observable. La UI (vía ViewModel) observa esto
     * directamente: cualquier cambio en Room (local o sincronizado)
     * se refleja en pantalla de forma automática y reactiva.
     */
    @Query("SELECT * FROM repuestos ORDER BY nombre ASC")
    fun obtenerTodosObservable(): LiveData<List<RepuestoEntity>>

    @Query("SELECT * FROM repuestos WHERE uid = :uid LIMIT 1")
    suspend fun obtenerPorUid(uid: String): RepuestoEntity?

    @Query("SELECT * FROM repuestos WHERE uid = :uid LIMIT 1")
    fun obtenerPorUidObservable(uid: String): LiveData<RepuestoEntity?>

    /**
     * Repuestos con cambios locales que aún no llegaron a Firestore.
     * Usado por SincronizacionRepuestosWorker para saber qué subir.
     */
    @Query("SELECT * FROM repuestos WHERE estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSincronizar(): List<RepuestoEntity>

    /**
     * Marca un repuesto como ya sincronizado, una vez que el Worker
     * confirma que Firestore aceptó el cambio.
     */
    @Query("UPDATE repuestos SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid")
    suspend fun marcarComoSincronizado(uid: String)

    /**
     * Ajusta el stock localmente de forma inmediata (offline-friendly).
     * El delta puede ser positivo (entrada) o negativo (salida).
     * Marca el registro como pendiente de actualizar para que el
     * Worker propague el cambio a Firestore cuando haya conexión.
     */
    @Query(
        "UPDATE repuestos SET stockActual = stockActual + :delta, " +
            "estadoSync = CASE WHEN estadoSync = 'SINCRONIZADO' THEN 'PENDIENTE_ACTUALIZAR' ELSE estadoSync END, " +
            "timestampLocal = :timestamp " +
            "WHERE uid = :uid"
    )
    suspend fun ajustarStockLocal(uid: String, delta: Int, timestamp: Long = System.currentTimeMillis())
}
