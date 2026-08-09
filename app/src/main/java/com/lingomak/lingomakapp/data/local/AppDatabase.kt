package com.lingomak.lingomakapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        SolicitudMantenimientoEntity::class,
        UserEntity::class,
        AlertaEntity::class,
        CategoriaEntity::class,
        SuministroEntity::class
    ],
    version = 27,
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
    abstract fun suministroDao(): SuministroDao
    abstract fun userDao(): UserDao
    abstract fun alertaDao(): AlertaDao
    abstract fun categoriaDao(): CategoriaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE maquinarias ADD COLUMN capacidadTanqueGls REAL DEFAULT NULL")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migración de horómetros Int a Double en maquinarias
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `maquinarias_new` (
                        `uid` TEXT NOT NULL, `codigoMaquinaria` TEXT NOT NULL, `nombre` TEXT NOT NULL, 
                        `tipo` TEXT NOT NULL, `marca` TEXT NOT NULL, `modelo` TEXT NOT NULL, 
                        `placaSerie` TEXT NOT NULL, `anio` INTEGER NOT NULL, `estado` TEXT NOT NULL, 
                        `horometroActual` REAL NOT NULL, `horometroUltimoMantenimiento` REAL NOT NULL, 
                        `capacidadTanqueGls` REAL, `intervaloMantenimientoHoras` INTEGER NOT NULL, 
                        `ubicacionActual` TEXT NOT NULL, `imagenUrl` TEXT NOT NULL, 
                        `observaciones` TEXT NOT NULL, `fechaRegistro` TEXT NOT NULL, 
                        `fechaActualizacion` TEXT NOT NULL, `registradoPor` TEXT NOT NULL, 
                        `estadoSync` TEXT NOT NULL, `timestampLocal` INTEGER NOT NULL, 
                        PRIMARY KEY(`uid`)
                    )
                """.trimIndent())
                
                db.execSQL("""
                    INSERT INTO maquinarias_new (
                        uid, codigoMaquinaria, nombre, tipo, marca, modelo, placaSerie, anio, estado, 
                        horometroActual, horometroUltimoMantenimiento, capacidadTanqueGls, 
                        intervaloMantenimientoHoras, ubicacionActual, imagenUrl, observaciones, 
                        fechaRegistro, fechaActualizacion, registradoPor, estadoSync, timestampLocal
                    )
                    SELECT 
                        uid, codigoMaquinaria, nombre, tipo, marca, modelo, placaSerie, anio, estado, 
                        CAST(horometroActual AS REAL), CAST(horometroUltimoMantenimiento AS REAL), capacidadTanqueGls, 
                        intervaloMantenimientoHoras, ubicacionActual, imagenUrl, observaciones, 
                        fechaRegistro, fechaActualizacion, registradoPor, estadoSync, timestampLocal
                    FROM maquinarias
                """.trimIndent())
                
                db.execSQL("DROP TABLE maquinarias")
                db.execSQL("ALTER TABLE maquinarias_new RENAME TO maquinarias")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migración de registros_uso_maquinaria: rename observacion -> trabajoRealizado, nuevos campos, horómetros Double
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `registros_uso_maquinaria_new` (
                        `uid` TEXT NOT NULL, `uidMaquinaria` TEXT NOT NULL, `codigoMaquinaria` TEXT NOT NULL, 
                        `nombreMaquinaria` TEXT NOT NULL, `tipoMaquinaria` TEXT NOT NULL, 
                        `uidOperario` TEXT NOT NULL, `nombreOperario` TEXT NOT NULL, 
                        `correoOperario` TEXT NOT NULL, `fechaUso` TEXT NOT NULL, 
                        `horometroAnterior` REAL NOT NULL, `horasUso` REAL NOT NULL, 
                        `horometroFinal` REAL NOT NULL, `trabajoRealizado` TEXT NOT NULL, 
                        `fechaRegistro` TEXT NOT NULL, `tipoMovimiento` TEXT NOT NULL DEFAULT 'Trabajo', 
                        `obra` TEXT, `contratista` TEXT, `ubicacion` TEXT, 
                        `estadoSync` TEXT NOT NULL, `timestampLocal` INTEGER NOT NULL, 
                        PRIMARY KEY(`uid`)
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO registros_uso_maquinaria_new (
                        uid, uidMaquinaria, codigoMaquinaria, nombreMaquinaria, tipoMaquinaria, 
                        uidOperario, nombreOperario, correoOperario, fechaUso, 
                        horometroAnterior, horasUso, horometroFinal, trabajoRealizado, 
                        fechaRegistro, estadoSync, timestampLocal
                    )
                    SELECT 
                        uid, uidMaquinaria, codigoMaquinaria, nombreMaquinaria, tipoMaquinaria, 
                        uidOperario, nombreOperario, correoOperario, fechaUso, 
                        CAST(horometroAnterior AS REAL), CAST(horasUso AS REAL), CAST(horometroFinal AS REAL), 
                        observacion, fechaRegistro, estadoSync, timestampLocal
                    FROM registros_uso_maquinaria
                """.trimIndent())

                db.execSQL("DROP TABLE registros_uso_maquinaria")
                db.execSQL("ALTER TABLE registros_uso_maquinaria_new RENAME TO registros_uso_maquinaria")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `suministros`")
                db.execSQL("""
                    CREATE TABLE `suministros` (
                        `uid` TEXT NOT NULL, `uidMaquinaria` TEXT NOT NULL, `fecha` TEXT NOT NULL, 
                        `horometroSuministro` REAL NOT NULL, `tipoCarga` TEXT NOT NULL, 
                        `tipoCombustible` TEXT NOT NULL, `galonesCombustible` REAL NOT NULL, 
                        `galonesAceite` REAL NOT NULL, `uidRegistroUso` TEXT, 
                        `estadoSync` TEXT NOT NULL, `timestampLocal` INTEGER NOT NULL, 
                        PRIMARY KEY(`uid`)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                db.execSQL("""
                    INSERT INTO categorias (uid, nombre, tipo, estado, fechaRegistro, estadoSync, timestampLocal)
                    VALUES 
                    ('diesel-initial', 'Diesel', 'COMBUSTIBLE', 'ACTIVO', $now, 'SINCRONIZADO', $now),
                    ('gasolina-initial', 'Gasolina', 'COMBUSTIBLE', 'ACTIVO', $now, 'SINCRONIZADO', $now)
                """.trimIndent())
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE registros_uso_maquinaria ADD COLUMN modificadoPorUid TEXT")
                db.execSQL("ALTER TABLE registros_uso_maquinaria ADD COLUMN modificadoPorNombre TEXT")
                db.execSQL("ALTER TABLE registros_uso_maquinaria ADD COLUMN fechaUltimaModificacion TEXT")
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Auditoría detallada para repuestos
                db.execSQL("ALTER TABLE repuestos ADD COLUMN modificadoPorUid TEXT")
                db.execSQL("ALTER TABLE repuestos ADD COLUMN modificadoPorNombre TEXT")
                db.execSQL("ALTER TABLE repuestos ADD COLUMN fechaUltimaModificacion TEXT")
                
                // Auditoría detallada para mantenimientos
                db.execSQL("ALTER TABLE mantenimientos ADD COLUMN modificadoPorUid TEXT")
                db.execSQL("ALTER TABLE mantenimientos ADD COLUMN modificadoPorNombre TEXT")
                db.execSQL("ALTER TABLE mantenimientos ADD COLUMN fechaUltimaModificacion TEXT")
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Maquinarias: intervaloMantenimientoHoras Int -> REAL
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `maquinarias_new` (
                        `uid` TEXT NOT NULL, `codigoMaquinaria` TEXT NOT NULL, `nombre` TEXT NOT NULL, 
                        `tipo` TEXT NOT NULL, `marca` TEXT NOT NULL, `modelo` TEXT NOT NULL, 
                        `placaSerie` TEXT NOT NULL, `anio` INTEGER NOT NULL, `estado` TEXT NOT NULL, 
                        `horometroActual` REAL NOT NULL, `horometroUltimoMantenimiento` REAL NOT NULL, 
                        `capacidadTanqueGls` REAL, `intervaloMantenimientoHoras` REAL NOT NULL, 
                        `ubicacionActual` TEXT NOT NULL, `imagenUrl` TEXT NOT NULL, 
                        `observaciones` TEXT NOT NULL, `fechaRegistro` TEXT NOT NULL, 
                        `fechaActualizacion` TEXT NOT NULL, `registradoPor` TEXT NOT NULL, 
                        `estadoSync` TEXT NOT NULL, `timestampLocal` INTEGER NOT NULL, 
                        PRIMARY KEY(`uid`)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO maquinarias_new SELECT 
                        uid, codigoMaquinaria, nombre, tipo, marca, modelo, placaSerie, anio, estado, 
                        horometroActual, horometroUltimoMantenimiento, capacidadTanqueGls, 
                        CAST(intervaloMantenimientoHoras AS REAL), ubicacionActual, imagenUrl, observaciones, 
                        fechaRegistro, fechaActualizacion, registradoPor, estadoSync, timestampLocal 
                    FROM maquinarias
                """.trimIndent())
                db.execSQL("DROP TABLE maquinarias")
                db.execSQL("ALTER TABLE maquinarias_new RENAME TO maquinarias")

                // 2. Mantenimientos: horometroProgramado, horometroReal Int -> REAL
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `mantenimientos_new` (
                        `uid` TEXT NOT NULL, `codigoMantenimiento` TEXT NOT NULL, `uidMaquinaria` TEXT NOT NULL, 
                        `codigoMaquinaria` TEXT NOT NULL, `nombreMaquinaria` TEXT NOT NULL, `tipoMaquinaria` TEXT NOT NULL, 
                        `tipoMantenimiento` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `fechaProgramada` TEXT NOT NULL, 
                        `fechaRealizada` TEXT NOT NULL, `estado` TEXT NOT NULL, `responsable` TEXT NOT NULL, 
                        `responsableUid` TEXT NOT NULL, `observaciones` TEXT NOT NULL, `costoEstimado` REAL NOT NULL, 
                        `costoReal` REAL NOT NULL, `horometroProgramado` REAL NOT NULL, `horometroReal` REAL NOT NULL, 
                        `fechaRegistro` TEXT NOT NULL, `fechaActualizacion` TEXT NOT NULL, `registradoPor` TEXT NOT NULL, 
                        `actualizadoPor` TEXT NOT NULL, `prioridad` TEXT NOT NULL, `resolutorNombre` TEXT NOT NULL, 
                        `modificadoPorUid` TEXT, `modificadoPorNombre` TEXT, `fechaUltimaModificacion` TEXT, 
                        `imagenesReporte` TEXT NOT NULL, `imagenesFinalizacion` TEXT NOT NULL, 
                        `imagenesReporteLocal` TEXT NOT NULL, `imagenesFinalizacionLocal` TEXT NOT NULL, 
                        `reporteIA` TEXT NOT NULL, `estadoSync` TEXT NOT NULL, `timestampLocal` INTEGER NOT NULL, 
                        PRIMARY KEY(`uid`)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO mantenimientos_new SELECT 
                        uid, codigoMantenimiento, uidMaquinaria, codigoMaquinaria, nombreMaquinaria, tipoMaquinaria, 
                        tipoMantenimiento, descripcion, fechaProgramada, fechaRealizada, estado, responsable, 
                        responsableUid, observaciones, costoEstimado, costoReal, 
                        CAST(horometroProgramado AS REAL), CAST(horometroReal AS REAL), 
                        fechaRegistro, fechaActualizacion, registradoPor, actualizadoPor, prioridad, resolutorNombre, 
                        modificadoPorUid, modificadoPorNombre, fechaUltimaModificacion, 
                        imagenesReporte, imagenesFinalizacion, imagenesReporteLocal, imagenesFinalizacionLocal, 
                        reporteIA, estadoSync, timestampLocal 
                    FROM mantenimientos
                """.trimIndent())
                db.execSQL("DROP TABLE mantenimientos")
                db.execSQL("ALTER TABLE mantenimientos_new RENAME TO mantenimientos")

                // 3. Solicitudes: varios Int -> REAL
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `solicitudes_mantenimiento_new` (
                        `uid` TEXT NOT NULL, `uidMaquinaria` TEXT NOT NULL, `codigoMaquinaria` TEXT NOT NULL, 
                        `nombreMaquinaria` TEXT NOT NULL, `tipoMaquinaria` TEXT NOT NULL, `uidOperario` TEXT NOT NULL, 
                        `nombreOperario` TEXT NOT NULL, `correoOperario` TEXT NOT NULL, `horometroActual` REAL NOT NULL, 
                        `horometroUltimoMantenimiento` REAL NOT NULL, `intervaloMantenimientoHoras` REAL NOT NULL, 
                        `horasDesdeUltimoMantenimiento` REAL NOT NULL, `horasRestantes` REAL NOT NULL, 
                        `motivo` TEXT NOT NULL, `estadoSolicitud` TEXT NOT NULL, `origen` TEXT NOT NULL, 
                        `fechaSugerida` TEXT NOT NULL, `fechaRegistro` TEXT NOT NULL, `revisadoPor` TEXT NOT NULL, 
                        `fechaRevision` TEXT NOT NULL, `motivoRechazo` TEXT NOT NULL, `uidMantenimientoGenerado` TEXT NOT NULL, 
                        `estadoSync` TEXT NOT NULL, `timestampLocal` INTEGER NOT NULL, PRIMARY KEY(`uid`)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO solicitudes_mantenimiento_new SELECT 
                        uid, uidMaquinaria, codigoMaquinaria, nombreMaquinaria, tipoMaquinaria, uidOperario, 
                        nombreOperario, correoOperario, CAST(horometroActual AS REAL), 
                        CAST(horometroUltimoMantenimiento AS REAL), CAST(intervaloMantenimientoHoras AS REAL), 
                        CAST(horasDesdeUltimoMantenimiento AS REAL), CAST(horasRestantes AS REAL), 
                        motivo, estadoSolicitud, origen, fechaSugerida, fechaRegistro, revisadoPor, 
                        fechaRevision, motivoRechazo, uidMantenimientoGenerado, estadoSync, timestampLocal 
                    FROM solicitudes_mantenimiento
                """.trimIndent())
                db.execSQL("DROP TABLE solicitudes_mantenimiento")
                db.execSQL("ALTER TABLE solicitudes_mantenimiento_new RENAME TO solicitudes_mantenimiento")
            }
        }

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
                    .addMigrations(MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            val now = System.currentTimeMillis()
                            db.execSQL("""
                                INSERT INTO categorias (uid, nombre, tipo, estado, fechaRegistro, estadoSync, timestampLocal)
                                VALUES 
                                ('diesel-initial', 'Diesel', 'COMBUSTIBLE', 'ACTIVO', $now, 'SINCRONIZADO', $now),
                                ('gasolina-initial', 'Gasolina', 'COMBUSTIBLE', 'ACTIVO', $now, 'SINCRONIZADO', $now)
                            """.trimIndent())
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
