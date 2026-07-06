package com.lingomak.lingomakapp.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.RepuestoEntity
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlertasRepository(
    private val context: Context
) {

    private val database = FirebaseFirestore.getInstance()
    private val coleccionMantenimientos = "mantenimientos"

    private val repuestoDao =
        AppDatabase.getInstance(context).repuestoDao()

    private val movimientoDao =
        AppDatabase.getInstance(context).movimientoDao()

    fun listarAlertas(
        onSuccess: (List<AlertaModel>) -> Unit,
        onError: (String) -> Unit
    ) {
        database.collection(coleccionMantenimientos)
            .addSnapshotListener { snapshots, error ->

                if (error != null) {
                    onError(error.message ?: "Error al listar alertas")
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    onSuccess(emptyList())
                    return@addSnapshotListener
                }

                CoroutineScope(Dispatchers.IO).launch {

                    val listaAlertas = mutableListOf<AlertaModel>()

                    // 1. Alertas de mantenimiento desde Firestore.
                    cargarAlertasMantenimiento(
                        snapshots = snapshots,
                        listaAlertas = listaAlertas
                    )

                    // 2. Obtenemos repuestos activos desde Room.
                    val repuestos =
                        repuestoDao.obtenerRepuestosActivos()

                    // 3. Alertas de inventario.
                    cargarAlertasInventario(
                        repuestos = repuestos,
                        listaAlertas = listaAlertas
                    )

                    // 4. Alertas de movimientos.
                    cargarAlertasMovimientos(
                        repuestos = repuestos,
                        listaAlertas = listaAlertas
                    )

                    // 5. Ordenamos la lista final.
                    val listaOrdenada =
                        ordenarAlertas(listaAlertas)

                    withContext(Dispatchers.Main) {
                        onSuccess(listaOrdenada)
                    }
                }
            }
    }

    private fun cargarAlertasMantenimiento(
        snapshots: QuerySnapshot,
        listaAlertas: MutableList<AlertaModel>
    ) {
        snapshots.documents.forEach { document ->

            val mantenimiento =
                document.toObject(MantenimientoModel::class.java)

            if (mantenimiento != null) {

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
                            estadoRelacionado = mantenimiento.estado,
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
                            estadoRelacionado = mantenimiento.estado,
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
                            estadoRelacionado = mantenimiento.estado,
                            uidMantenimiento = mantenimiento.uid,
                            codigoMantenimiento = mantenimiento.codigoMantenimiento,
                            uidMaquinaria = mantenimiento.uidMaquinaria,
                            nombreMaquinaria = mantenimiento.nombreMaquinaria
                        )
                    )
                }
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
            movimientoDao.obtenerSalidasDesde(
                DateUtils.obtenerTimestampHaceDias(7)
            )

        val consumoPorRepuesto =
            salidasUltimos7Dias
                .groupBy { it.repuestoUid }
                .mapValues { entry ->
                    entry.value.sumOf { it.cantidad }
                }

        val movimientosUltimos90Dias =
            movimientoDao.obtenerMovimientosDesde(
                DateUtils.obtenerTimestampHaceDias(90)
            )

        val repuestosConMovimiento =
            movimientosUltimos90Dias
                .map { it.repuestoUid }
                .toSet()

        repuestos.forEach { repuesto ->

            val consumoSemanal =
                consumoPorRepuesto[repuesto.uid] ?: 0

            val limiteAltoConsumo =
                repuesto.stockMaximo * 0.40

            if (
                repuesto.stockMaximo > 0 &&
                consumoSemanal >= limiteAltoConsumo
            ) {
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