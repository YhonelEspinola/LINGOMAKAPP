package com.lingomak.lingomakapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lingomak.lingomakapp.data.local.entity.ContadorEntity

@Dao
interface ContadorDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inicializarSiNoExiste(contador: ContadorEntity)

    @Query("SELECT ultimoNumero FROM contadores WHERE prefijo = :prefijo")
    suspend fun obtenerUltimoNumero(prefijo: String): Int?

    @Query("UPDATE contadores SET ultimoNumero = ultimoNumero + 1 WHERE prefijo = :prefijo")
    suspend fun incrementar(prefijo: String)

    @Query("UPDATE contadores SET ultimoNumero = :nuevoNumero WHERE prefijo = :prefijo")
    suspend fun actualizar(prefijo: String, nuevoNumero: Int)

    @Transaction
    suspend fun actualizarSiEsMayor(prefijo: String, numero: Int) {
        inicializarSiNoExiste(ContadorEntity(prefijo = prefijo, ultimoNumero = 0))
        val actual = obtenerUltimoNumero(prefijo) ?: 0
        if (numero > actual) {
            actualizar(prefijo, numero)
        }
    }

    @Transaction
    suspend fun obtenerSiguienteNumero(prefijo: String): Int {
        inicializarSiNoExiste(ContadorEntity(prefijo = prefijo, ultimoNumero = 0))
        val ultimo = obtenerUltimoNumero(prefijo) ?: 0
        return ultimo + 1
    }

    /**
     * Reserva el siguiente número para un prefijo dado de forma
     * atómica: si el prefijo no existe lo inicializa en 0, luego
     * incrementa y devuelve el nuevo valor. Todo en una transacción
     * Room para que no haya condición de carrera entre hilos.
     */
    @Transaction
    suspend fun reservarSiguienteNumero(prefijo: String): Int {
        inicializarSiNoExiste(ContadorEntity(prefijo = prefijo, ultimoNumero = 0))
        incrementar(prefijo)
        return obtenerUltimoNumero(prefijo) ?: 1
    }
}
