package com.lingomak.lingomakapp.data.service

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.RepuestoEntity
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.utils.DateUtils
import kotlinx.coroutines.tasks.await
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel

class AlertasService(
    private val context: Context
) {

    private val database = FirebaseFirestore.getInstance()
    private val repuestoDao = AppDatabase.getInstance(context).repuestoDao()
    private val movimientoDao = AppDatabase.getInstance(context).movimientoDao()

    private val coleccionSolicitudesMantenimiento = "solicitudes_mantenimiento"

    suspend fun obtenerAlertas(userUid: String? = null, esOperario: Boolean = false): List<AlertaModel> {

        val listaAlertas = mutableListOf<AlertaModel>()

        // 1. Mantenimientos (Filtrar por operario si corresponde)
        var queryMantenimientos = database.collection("mantenimientos")
        
        val mantenimientos = if (esOperario && userUid != null) {
            queryMantenimientos.get().await().toObjects(MantenimientoModel::class.java)
                .filter { it.responsableUid == userUid || it.responsableUid == "TODOS" }
        } else {
            queryMantenimientos.get().await().toObjects(MantenimientoModel::class.java)
        }

        cargarAlertasMantenimiento(mantenimientos, listaAlertas)

        if (!esOperario) {
            val repuestos = repuestoDao.obtenerRepuestosActivos()
            cargarAlertasInventario(repuestos, listaAlertas)
            cargarAlertasMovimientos(repuestos, listaAlertas)

            val solicitudesPendientes = database.collection(coleccionSolicitudesMantenimiento)
                .whereEqualTo("estadoSolicitud", "PENDIENTE_APROBACION")
                .get().await().toObjects(SolicitudMantenimientoModel::class.java)

            cargarAlertasSolicitudes(solicitudesPendientes, listaAlertas)
        }

        return ordenarAlertas(listaAlertas)
    }

    private fun cargarAlertasMantenimiento(
        mantenimientos: List<MantenimientoModel>,
        listaAlertas: MutableList<AlertaModel>
    ) {
        mantenimientos.forEach { mantenimiento ->

            if (mantenimiento.estado == "VENCIDO") {
                listaAlertas.add(
                    AlertaModel(
                        uid = mantenimiento.uid,
                        categoria = "MANTENIMIENTO",
                        icono = "🚨",
                        titulo = "Mantenimiento vencido",
                        mensaje = "La maquinaria ${mantenimiento.nombreMaquinaria} tiene un mantenimiento vencido.",
                        tipo = "VENCIDO",
                        prioridad = mantenimiento.prioridad,
                        fecha = mantenimiento.fechaProgramada,
                        uidMantenimiento = mantenimiento.uid,
                        codigoMantenimiento = mantenimiento.codigoMantenimiento,
                        uidMaquinaria = mantenimiento.uidMaquinaria,
                        nombreMaquinaria = mantenimiento.nombreMaquinaria
                    )
                )
            }

            if (mantenimiento.estado == "EN_PROCESO") {
                listaAlertas.add(
                    AlertaModel(
                        uid = mantenimiento.uid,
                        categoria = "MANTENIMIENTO",
                        icono = "🛠",
                        titulo = "Mantenimiento en proceso",
                        mensaje = "La maquinaria ${mantenimiento.nombreMaquinaria} se encuentra en mantenimiento.",
                        tipo = "EN_PROCESO",
                        prioridad = "MEDIA",
                        fecha = mantenimiento.fechaProgramada,
                        uidMantenimiento = mantenimiento.uid,
                        codigoMantenimiento = mantenimiento.codigoMantenimiento,
                        uidMaquinaria = mantenimiento.uidMaquinaria,
                        nombreMaquinaria = mantenimiento.nombreMaquinaria
                    )
                )
            }

            if (
                mantenimiento.estado == "PENDIENTE"
            ) {
                val esProximo = DateUtils.mantenimientoProximo(mantenimiento.fechaProgramada)
                val titulo = if (esProximo) "Mantenimiento próximo" else "Mantenimiento asignado"
                val icono = if (esProximo) "⏳" else "📋"
                val tipo = if (esProximo) "PROXIMO" else "MANTENIMIENTO_PENDIENTE"

                listaAlertas.add(
                    AlertaModel(
                        uid = mantenimiento.uid,
                        categoria = "MANTENIMIENTO",
                        icono = icono,
                        titulo = titulo,
                        mensaje = "Tiene asignado el mantenimiento de la maquinaria ${mantenimiento.nombreMaquinaria}.",
                        tipo = tipo,
                        prioridad = mantenimiento.prioridad,
                        fecha = mantenimiento.fechaProgramada,
                        uidMantenimiento = mantenimiento.uid,
                        codigoMantenimiento = mantenimiento.codigoMantenimiento,
                        uidMaquinaria = mantenimiento.uidMaquinaria,
                        nombreMaquinaria = mantenimiento.nombreMaquinaria
                    )
                )
            }
        }
    }

    private fun cargarAlertasInventario(
        repuestos: List<RepuestoEntity>,
        listaAlertas: MutableList<AlertaModel>
    ) {
        repuestos.forEach { repuesto ->

            when {
                repuesto.stockActual == 0 -> {
                    listaAlertas.add(
                        AlertaModel(
                            uid = repuesto.uid,
                            categoria = "INVENTARIO",
                            icono = "📦",
                            titulo = "Repuesto agotado",
                            mensaje = "El repuesto ${repuesto.nombre} se encuentra agotado.",
                            tipo = "STOCK_AGOTADO",
                            prioridad = "ALTA",
                            uidRepuesto = repuesto.uid,
                            codigoRepuesto = repuesto.codigoInterno,
                            nombreRepuesto = repuesto.nombre,
                            stockActual = repuesto.stockActual,
                            stockMinimo = repuesto.stockMinimo
                        )
                    )
                }

                repuesto.stockActual <= repuesto.stockMinimo -> {
                    listaAlertas.add(
                        AlertaModel(
                            uid = repuesto.uid,
                            categoria = "INVENTARIO",
                            icono = "📦",
                            titulo = "Stock crítico",
                            mensaje = "El repuesto ${repuesto.nombre} alcanzó el stock mínimo.",
                            tipo = "STOCK_CRITICO",
                            prioridad = "ALTA",
                            uidRepuesto = repuesto.uid,
                            codigoRepuesto = repuesto.codigoInterno,
                            nombreRepuesto = repuesto.nombre,
                            stockActual = repuesto.stockActual,
                            stockMinimo = repuesto.stockMinimo
                        )
                    )
                }

                repuesto.stockActual <= repuesto.stockMinimo + 3 -> {
                    listaAlertas.add(
                        AlertaModel(
                            uid = repuesto.uid,
                            categoria = "INVENTARIO",
                            icono = "📦",
                            titulo = "Stock bajo",
                            mensaje = "El repuesto ${repuesto.nombre} está próximo a llegar al stock mínimo.",
                            tipo = "STOCK_BAJO",
                            prioridad = "MEDIA",
                            uidRepuesto = repuesto.uid,
                            codigoRepuesto = repuesto.codigoInterno,
                            nombreRepuesto = repuesto.nombre,
                            stockActual = repuesto.stockActual,
                            stockMinimo = repuesto.stockMinimo
                        )
                    )
                }
            }
        }
    }

    private suspend fun cargarAlertasMovimientos(
        repuestos: List<RepuestoEntity>,
        listaAlertas: MutableList<AlertaModel>
    ) {
        val salidasUltimos7Dias =
            movimientoDao.obtenerSalidasDesde(DateUtils.obtenerTimestampHaceDias(7))

        val consumoPorRepuesto =
            salidasUltimos7Dias
                .groupBy { it.repuestoUid }
                .mapValues { entry -> entry.value.sumOf { it.cantidad } }

        val movimientosUltimos90Dias =
            movimientoDao.obtenerMovimientosDesde(DateUtils.obtenerTimestampHaceDias(90))

        val repuestosConMovimiento =
            movimientosUltimos90Dias.map { it.repuestoUid }.toSet()

        repuestos.forEach { repuesto ->

            val consumoSemanal = consumoPorRepuesto[repuesto.uid] ?: 0
            val limiteAltoConsumo = repuesto.stockMaximo * 0.40

            if (repuesto.stockMaximo > 0 && consumoSemanal >= limiteAltoConsumo) {
                listaAlertas.add(
                    AlertaModel(
                        uid = "${repuesto.uid}_ALTO_CONSUMO",
                        categoria = "MOVIMIENTOS",
                        icono = "📈",
                        titulo = "Alto consumo de repuesto",
                        mensaje = "El repuesto ${repuesto.nombre} tuvo $consumoSemanal salidas en los últimos 7 días.",
                        tipo = "ALTO_CONSUMO",
                        prioridad = "MEDIA",
                        uidRepuesto = repuesto.uid,
                        codigoRepuesto = repuesto.codigoInterno,
                        nombreRepuesto = repuesto.nombre,
                        stockActual = repuesto.stockActual,
                        stockMinimo = repuesto.stockMinimo
                    )
                )
            }

            if (!repuestosConMovimiento.contains(repuesto.uid)) {
                listaAlertas.add(
                    AlertaModel(
                        uid = "${repuesto.uid}_SIN_ROTACION",
                        categoria = "MOVIMIENTOS",
                        icono = "📦",
                        titulo = "Repuesto sin rotación",
                        mensaje = "El repuesto ${repuesto.nombre} no registra movimientos en los últimos 90 días.",
                        tipo = "SIN_ROTACION",
                        prioridad = "MEDIA",
                        uidRepuesto = repuesto.uid,
                        codigoRepuesto = repuesto.codigoInterno,
                        nombreRepuesto = repuesto.nombre,
                        stockActual = repuesto.stockActual,
                        stockMinimo = repuesto.stockMinimo
                    )
                )
            }
        }
    }

    private fun ordenarAlertas(
        listaAlertas: List<AlertaModel>
    ): List<AlertaModel> {
        return listaAlertas.sortedWith(
            compareByDescending<AlertaModel> { it.fecha }
                .thenBy { alerta ->
                    when (alerta.categoria) {
                        "MANTENIMIENTO" -> 1
                        "INVENTARIO" -> 2
                        "MOVIMIENTOS" -> 3
                        else -> 4
                    }
                }
        )
    }

    private fun cargarAlertasSolicitudes(
        solicitudes: List<SolicitudMantenimientoModel>,
        listaAlertas: MutableList<AlertaModel>
    ) {
        solicitudes.forEach { solicitud ->
            val prioridad = if (solicitud.horasRestantes <= 10) "ALTA" else "MEDIA"
            val mensaje = when {
                solicitud.horasRestantes < 0 -> "La maquinaria ${solicitud.nombreMaquinaria} superó el intervalo de mantenimiento por ${kotlin.math.abs(solicitud.horasRestantes)} horas."
                solicitud.horasRestantes == 0.0 -> "La maquinaria ${solicitud.nombreMaquinaria} alcanzó el límite de mantenimiento."
                else -> "La maquinaria ${solicitud.nombreMaquinaria} tiene una solicitud pendiente. Faltan ${solicitud.horasRestantes} horas."
            }

            listaAlertas.add(
                AlertaModel(
                    uid = "${solicitud.uid}_SOLICITUD_MANTENIMIENTO",
                    categoria = "MANTENIMIENTO",
                    icono = "📋",
                    titulo = "Solicitud de mantenimiento pendiente",
                    mensaje = mensaje,
                    tipo = "SOLICITUD_MANTENIMIENTO",
                    prioridad = prioridad,
                    fecha = solicitud.fechaRegistro,
                    uidSolicitudMantenimiento = solicitud.uid,
                    uidMaquinaria = solicitud.uidMaquinaria,
                    nombreMaquinaria = solicitud.nombreMaquinaria
                )
            )
        }
    }
}
