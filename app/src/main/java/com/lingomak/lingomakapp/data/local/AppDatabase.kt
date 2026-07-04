package com.lingomak.lingomakapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lingomak.lingomakapp.data.local.dao.ContadorDao
import com.lingomak.lingomakapp.data.local.dao.MovimientoDao
import com.lingomak.lingomakapp.data.local.dao.RepuestoDao
import com.lingomak.lingomakapp.data.local.entity.ContadorEntity
import com.lingomak.lingomakapp.data.local.entity.MovimientoEntity
import com.lingomak.lingomakapp.data.local.entity.RepuestoEntity

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
          ContadorEntity::class
      ],
      version = 6,
      exportSchema = false
  )
  abstract class AppDatabase : RoomDatabase() {
  
      abstract fun repuestoDao(): RepuestoDao
      abstract fun movimientoDao(): MovimientoDao
      abstract fun contadorDao(): ContadorDao

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
