package com.lingomak.lingomakapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla local que guarda el último correlativo usado por cada
 * categoría de repuesto. Permite generar códigos internos del tipo
 * REP-ACE-0001 de forma offline, sin necesitar Firestore.
 *
 * La clave primaria es el prefijo de 3 letras de la categoría
 * (ej. "ACE", "FIL", "FRE"), no el nombre completo, para que sea
 * consistente aunque el texto de la categoría tenga tildes o
 * variaciones menores.
 */
@Entity(tableName = "contadores")
data class ContadorEntity(
    @PrimaryKey
    val prefijo: String,    // "ACE", "FIL", "FRE", "ELE", "MOT"
    val ultimoNumero: Int = 0
)
