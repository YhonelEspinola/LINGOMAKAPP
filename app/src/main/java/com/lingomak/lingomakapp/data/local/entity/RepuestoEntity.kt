package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla local (SQLite vía Room) que espeja RepuestoModel, más los
 * campos de control necesarios para la sincronización offline-first
 * con Cloud Firestore.
 *
 * FUENTE DE VERDAD LOCAL: la UI (Fragments/ViewModel) SIEMPRE lee y
 * escribe contra esta tabla, nunca contra Firestore directamente.
 * SincronizacionRepuestosWorker es el único punto que habla con
 * Firestore, subiendo lo pendiente y bajando cambios remotos.
 *
 * Campos de control:
 * - estadoSync: estado de sincronización de ESTE registro.
 *   "SINCRONIZADO"  -> ya existe igual en Firestore, nada pendiente.
 *   "PENDIENTE_CREAR" -> se creó offline, falta subirlo (primera vez).
 *   "PENDIENTE_ACTUALIZAR" -> existe en Firestore pero tiene cambios
 *      locales más recientes que aún no se subieron.
 *   "PENDIENTE_ELIMINAR" -> baja lógica hecha offline, falta propagar.
 * - timestampLocal: epoch millis de la última escritura LOCAL. Se usa
 *   para resolver conflictos con estrategia "último que escribe gana"
 *   al comparar contra el timestamp remoto de Firestore.
 */
@Entity(
    tableName = "repuestos",
    indices = [androidx.room.Index(value = ["codigoInterno"], unique = true)]
)
data class RepuestoEntity(
    @PrimaryKey
    val uid: String,
    val codigoInterno: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val marca: String = "",
    val descripcion: String = "",
    val stockActual: Int = 0,
    val stockMinimo: Int = 0,
    val stockMaximo: Int = 0,
    val ubicacionAlmacen: String = "",
    val imagenUrl: String = "",
    val codigoQR: String = "",
    val proveedorNombre: String = "",
    val proveedorContacto: String = "",
    val imagenLocalPath: String? = null, // Path local de la imagen para subir en background
    val qrLocalPath: String? = null, // Path local del QR para subir en background
    val estado: String = "ACTIVO",
    val fechaRegistro: Long? = null, // epoch millis (Room no mapea Date nativamente sin un TypeConverter)
    val fechaActualizacion: Long? = null,
    val registradoPor: String = "",
    val actualizadoPor: String = "",

    // ----- Control de sincronización (NO existen en RepuestoModel/Firestore) -----
    val estadoSync: String = "SINCRONIZADO",
    val timestampLocal: Long = System.currentTimeMillis()
)
