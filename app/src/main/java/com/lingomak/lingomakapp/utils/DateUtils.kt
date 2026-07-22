package com.lingomak.lingomakapp.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


object DateUtils {

    const val FORMATO_ESTANDAR = "yyyy-MM-dd"

    fun obtenerFechaActual(): String {

        return SimpleDateFormat(
            FORMATO_ESTANDAR,
            Locale.getDefault()
        ).format(Date())
    }


    fun fechaYaPaso(
        fechaTexto: String
    ): Boolean {

        return try {

            val formato =
                SimpleDateFormat(
                    FORMATO_ESTANDAR,
                    Locale.getDefault()
                )

            val fechaProgramada =
                formato.parse(fechaTexto)

            val hoy = Calendar.getInstance()

            hoy.set(
                Calendar.HOUR_OF_DAY,
                0
            )

            hoy.set(
                Calendar.MINUTE,
                0
            )

            hoy.set(
                Calendar.SECOND,
                0
            )

            hoy.set(
                Calendar.MILLISECOND,
                0
            )

            fechaProgramada != null &&
                    fechaProgramada.before(hoy.time)

        } catch (e: Exception) {

            false
        }
    }

    fun mantenimientoProximo(
        fechaTexto: String
    ): Boolean {

        return try {

            val formato =
                SimpleDateFormat(
                    FORMATO_ESTANDAR,
                    Locale.getDefault()
                )

            val fechaProgramada =
                formato.parse(fechaTexto)

            if (fechaProgramada == null)
                return false

            val hoy =
                Calendar.getInstance()

            hoy.set(
                Calendar.HOUR_OF_DAY,
                0
            )

            hoy.set(
                Calendar.MINUTE,
                0
            )

            hoy.set(
                Calendar.SECOND,
                0
            )

            hoy.set(
                Calendar.MILLISECOND,
                0
            )

            val hoyDate = hoy.time

            val limite =
                Calendar.getInstance()

            limite.add(
                Calendar.DAY_OF_YEAR,
                3
            )

            val limiteDate = limite.time

            fechaProgramada.after(hoyDate)
                    &&
                    fechaProgramada.before(limiteDate)

        } catch (e: Exception) {

            false
        }
    }

    fun convertirFecha(
        fechaTexto: String
    ): Date? {

        return try {

            SimpleDateFormat(
                FORMATO_ESTANDAR,
                Locale.getDefault()
            ).parse(fechaTexto)

        } catch (e: Exception) {

            null
        }
    }

    fun fechaEsAnteriorAHoy(fechaTexto: String): Boolean {
        return fechaYaPaso(fechaTexto)
    }

    fun obtenerTimestampHaceDias(
        dias: Int
    ): Long {

        val calendar = Calendar.getInstance()

        calendar.add(
            Calendar.DAY_OF_YEAR,
            -dias
        )

        return calendar.timeInMillis
    }

}
