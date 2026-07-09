package com.lingomak.lingomakapp.data.repository

import android.content.Context
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

    fun listarAlertas(
        onSuccess: (List<AlertaModel>) -> Unit,
        onError: (String) -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val listaAlertas = alertasService.obtenerAlertas()

                withContext(Dispatchers.Main) {
                    onSuccess(listaAlertas)
                }

            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    onError(
                        exception.message ?: "Error al listar alertas"
                    )
                }
            }
        }
    }
}