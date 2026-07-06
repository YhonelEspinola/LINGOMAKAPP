package com.lingomak.lingomakapp.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


object DateUtils {


    fun obtenerFechaActual(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())
    }


    fun fechaYaPaso(
        fechaTexto: String
    ): Boolean {

        return try {

            val formato =
                SimpleDateFormat(
                    "d/M/yyyy",
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
                    "d/M/yyyy",
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

            val limite =
                Calendar.getInstance()

            limite.add(
                Calendar.DAY_OF_YEAR,
                3
            )

            fechaProgramada.after(hoy.time)
                    &&
                    fechaProgramada.before(limite.time)

        } catch (e: Exception) {

            false
        }
    }

    fun convertirFecha(
        fechaTexto: String
    ): Date? {

        return try {

            SimpleDateFormat(
                "d/M/yyyy",
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