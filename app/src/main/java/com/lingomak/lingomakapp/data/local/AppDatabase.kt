package com.lingomak.lingomakapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lingomak.lingomakapp.data.local.dao.*
import com.lingomak.lingomakapp.data.local.entity.*

/**
 * Base de datos principal de la aplicación (Room).
 * Offline-first: los DAOs proporcionan los datos que la UI observa
 * mediante LiveData. La sincronización con la red ocurre en segundo
 * plano.
 */
  @Database(
      entities = [
          RepuestoEntity::class,
          MovimientoEntity::class,
          ContadorEntity::class,
          MaquinariaEntity::class,
          MantenimientoEntity::class,
          RegistroUsoMaquinariaEntity::class,
          SolicitudMantenimientoEntity::class
      ],
      version = 14,
      exportSchema = false
  )
  @TypeConverters(Converters::class)
  abstract class AppDatabase : RoomDatabase() {
  
      abstract fun repuestoDao(): RepuestoDao
      abstract fun movimientoDao(): MovimientoDao
      abstract fun maquinariaDao(): MaquinariaDao
      abstract fun mantenimientoDao(): MantenimientoDao
      abstract fun contadorDao(): ContadorDao
      abstract fun registroUsoMaquinariaDao(): RegistroUsoMaquinariaDao
      abstract fun solicitudMantenimientoDao(): SolicitudMantenimientoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene el Singleton de la base de datos.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lingomak_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
