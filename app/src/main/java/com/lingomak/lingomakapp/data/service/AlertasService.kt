package com.lingomak.lingomakapp.data.service

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.RepuestoEntity
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.utils.DateUtils
import kotlinx.coroutines.tasks.await

class AlertasService(
    private val context: Context
) {

    private val database = FirebaseFirestore.getInstance()
    private val repuestoDao = AppDatabase.getInstance(context).repuestoDao()
    private val movimientoDao = AppDatabase.getInstance(context).movimientoDao()

    suspend fun obtenerAlertas(): List<AlertaModel> {

        val listaAlertas = mutableListOf<AlertaModel>()

        val mantenimientos = database.collection("mantenimientos")
            .get()
            .await()
            .toObjects(MantenimientoModel::class.java)

        cargarAlertasMantenimiento(mantenimientos, listaAlertas)

        val repuestos = repuestoDao.obtenerRepuestosActivos()

        cargarAlertasInventario(repuestos, listaAlertas)
        cargarAlertasMovimientos(repuestos, listaAlertas)

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
                mantenimiento.estado == "PENDIENTE" &&
                DateUtils.mantenimientoProximo(mantenimiento.fechaProgramada)
            ) {
                listaAlertas.add(
                    AlertaModel(
                        uid = mantenimiento.uid,
                        categoria = "MANTENIMIENTO",
                        icono = "⏳",
                        titulo = "Mantenimiento próximo",
                        mensaje = "La maquinaria ${mantenimiento.nombreMaquinaria} tiene un mantenimiento próximo a vencer.",
                        tipo = "PROXIMO",
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
            compareBy<AlertaModel> { alerta ->
                when (alerta.categoria) {
                    "MANTENIMIENTO" -> 1
                    "INVENTARIO" -> 2
                    "MOVIMIENTOS" -> 3
                    else -> 4
                }
            }.thenBy { alerta ->
                when (alerta.tipo) {
                    "VENCIDO" -> 1
                    "STOCK_AGOTADO" -> 2
                    "STOCK_CRITICO" -> 3
                    "PROXIMO" -> 4
                    "STOCK_BAJO" -> 5
                    "ALTO_CONSUMO" -> 6
                    "SIN_ROTACION" -> 7
                    "EN_PROCESO" -> 8
                    else -> 9
                }
            }
        )
    }
}