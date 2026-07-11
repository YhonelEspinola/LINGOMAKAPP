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
            // Firestore no soporta OR de forma tan directa en versiones antiguas, pero podemos simularlo
            // o simplemente traer y filtrar localmente si la lista no es inmensa.
            queryMantenimientos.get().await().toObjects(MantenimientoModel::class.java)
                .filter { it.responsableUid == userUid || it.responsableUid == "TODOS" }
        } else {
            queryMantenimientos.get().await().toObjects(MantenimientoModel::class.java)
        }

        cargarAlertasMantenimiento(mantenimientos, listaAlertas)

        // Si es operario, solo mostramos las alertas de mantenimiento asignadas.
        // Las alertas de inventario y movimientos suelen ser de nivel administrativo.
        if (!esOperario) {
            val repuestos = repuestoDao.obtenerRepuestosActivos()
            cargarAlertasInventario(repuestos, listaAlertas)
            cargarAlertasMovimientos(repuestos, listaAlertas)

            val solicitudesPendientes = database.collection(coleccionSolicitudesMantenimiento)
                .whereEqualTo("estadoSolicitud", "PENDIENTE_APROBACION")
                .get().await().toObjects(SolicitudMantenimientoModel::class.java)

            cargarAlertasSolicitudes(solicitudesPendientes, listaAlertas)
            
            // Cargar notificaciones de actividad de operarios (nueva categoría)
            cargarNotificacionesActividad(listaAlertas)
        }

        return ordenarAlertas(listaAlertas)
    }

    private suspend fun cargarNotificacionesActividad(listaAlertas: MutableList<AlertaModel>) {
        try {
            // Calculamos la fecha de hace 7 días
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
            val haceSieteDias = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(calendar.time)

            val snapshot = database.collection("alertas")
                .whereEqualTo("tipo", "ACTIVIDAD_OPERARIO")
                .whereGreaterThan("fecha", haceSieteDias) // Solo de los últimos 7 días
                .limit(50)
                .get()
                .await()
            
            val actividades = snapshot.toObjects(AlertaModel::class.java)
            listaAlertas.addAll(actividades)
        } catch (e: Exception) {
            // Manejar error de consulta si los índices no están listos
        }
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
                    "ACTIVIDAD" -> 1
                    "MANTENIMIENTO" -> 2
                    "INVENTARIO" -> 3
                    "MOVIMIENTOS" -> 4
                    else -> 5
                }
            }.thenBy { alerta ->
                when (alerta.tipo) {
                    "SOLICITUD_MANTENIMIENTO" -> 1
                    "ACTIVIDAD_OPERARIO" -> 2
                    "VENCIDO" -> 3
                    "STOCK_AGOTADO" -> 4
                    "STOCK_CRITICO" -> 5
                    "PROXIMO" -> 6
                    "STOCK_BAJO" -> 7
                    "ALTO_CONSUMO" -> 8
                    "SIN_ROTACION" -> 9
                    "EN_PROCESO" -> 10
                    else -> 11
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
                solicitud.horasRestantes == 0 -> "La maquinaria ${solicitud.nombreMaquinaria} alcanzó el límite de mantenimiento."
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