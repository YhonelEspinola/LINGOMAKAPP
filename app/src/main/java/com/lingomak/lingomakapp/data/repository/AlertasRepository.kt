package com.lingomak.lingomakapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.utils.DateUtils

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

                        if (mantenimiento.estado == "VENCIDO") {

                            listaAlertas.add(
                                AlertaModel(
                                    uid = mantenimiento.uid,
                                    titulo = "Mantenimiento vencido",
                                    mensaje =
                                        "La maquinaria ${mantenimiento.nombreMaquinaria} tiene un mantenimiento vencido.",
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

                onSuccess(
                    listaAlertas.sortedBy { alerta ->
                        when (alerta.tipo) {
                            "VENCIDO" -> 1
                            "PROXIMO" -> 2
                            "EN_PROCESO" -> 3
                            else -> 4
                        }
                    }
                )
            }
    }


}