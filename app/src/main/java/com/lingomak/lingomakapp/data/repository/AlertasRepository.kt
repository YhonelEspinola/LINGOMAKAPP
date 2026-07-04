package com.lingomak.lingomakapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlertasRepository {

    private val database = FirebaseFirestore.getInstance()
    private val coleccionMantenimientos = "mantenimientos"

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

                val listaAlertas = mutableListOf<AlertaModel>()

                snapshots.documents.forEach { document ->

                    val mantenimiento =
                        document.toObject(MantenimientoModel::class.java)

                    if (mantenimiento != null) {

                        if (
                            mantenimiento.estado == "PENDIENTE" &&
                            fechaYaPaso(mantenimiento.fechaProgramada)
                        ) {
                            listaAlertas.add(
                                AlertaModel(
                                    uid = mantenimiento.uid,
                                    titulo = "Mantenimiento vencido",
                                    mensaje = "La maquinaria ${mantenimiento.nombreMaquinaria} tiene un mantenimiento vencido.",
                                    tipo = "VENCIDO",
                                    prioridad = "ALTA",
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
                            mantenimientoProximo(mantenimiento.fechaProgramada)
                        ) {
                            listaAlertas.add(
                                AlertaModel(
                                    uid = mantenimiento.uid,
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

                onSuccess(listaAlertas)
            }
    }

    private fun fechaYaPaso(fechaTexto: String): Boolean {
        return try {
            val formato = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val fechaMantenimiento = formato.parse(fechaTexto)

            val hoy = Calendar.getInstance().time

            fechaMantenimiento != null && fechaMantenimiento.before(hoy)
        } catch (e: Exception) {
            false
        }
    }

    private fun mantenimientoProximo(fechaTexto: String): Boolean {
        return try {
            val formato = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val fechaMantenimiento = formato.parse(fechaTexto) ?: return false

            val hoy = Calendar.getInstance()

            val limite = Calendar.getInstance()
            limite.add(Calendar.DAY_OF_YEAR, 3)

            fechaMantenimiento.after(hoy.time) &&
                    fechaMantenimiento.before(limite.time)

        } catch (e: Exception) {
            false
        }
    }
}