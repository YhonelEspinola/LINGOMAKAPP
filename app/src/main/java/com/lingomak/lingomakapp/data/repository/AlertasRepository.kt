package com.lingomak.lingomakapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.aEntity
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.service.AlertasService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlertasRepository(
    private val context: Context
) {

    private val alertasService = AlertasService(context)
    private val database = AppDatabase.getInstance(context)
    private val alertaDao = database.alertaDao()

    // ===================================================================
    // LECTURA LOCAL (Room)
    // ===================================================================

    fun obtenerAlertasObservable(): LiveData<List<AlertaModel>> {
        return alertaDao.obtenerTodasObservable().map { entities ->
            entities.map { it.aModel() }
        }
    }

    fun obtenerTotalAlertasObservable(): LiveData<Int> {
        return alertaDao.obtenerTotalAlertasObservable()
    }

    // ===================================================================
    // SINCRONIZACIÓN
    // ===================================================================

    fun listarAlertas(
        userUid: String? = null,
        esOperario: Boolean = false,
        onSuccess: (List<AlertaModel>) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Calcular alertas frescas (Firestore + Room)
                val listaAlertas = alertasService.obtenerAlertas(userUid, esOperario)

                // 2. Volcar a Room (Limpiar e insertar)
                alertaDao.limpiarAlertas()
                alertaDao.insertarLista(listaAlertas.map { it.aEntity() })

                withContext(Dispatchers.Main) {
                    onSuccess(listaAlertas)
                }

            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    onError(exception.message ?: "Error al listar alertas")
                }
            }
        }
    }
}
