package com.lingomak.lingomakapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.local.entity.MaquinariaEntity
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import kotlinx.coroutines.tasks.await

class MaquinariaRepositoryOp(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val maquinariasCollection = db.collection("maquinarias")
    private val maquinariaDao = AppDatabase.getInstance(context).maquinariaDao()

    fun obtenerMaquinariasObservable(): LiveData<List<MaquinariaModel>> {
        return maquinariaDao.obtenerTodasObservable().map { entities ->
            entities.map { it.aModel() }
        }
    }

    suspend fun descargarMaquinariasDeFirestore() {
        try {
            val snapshot = maquinariasCollection.get().await()
            val remotos = snapshot.toObjects(MaquinariaModel::class.java)
            val uidsRemotos = remotos.map { it.uid }

            if (uidsRemotos.isEmpty()) {
                maquinariaDao.eliminarTodasSincronizadas()
            } else {
                val entities = remotos.map { it.aEntity(estadoSync = "SINCRONIZADO") }
                maquinariaDao.insertarLista(entities)
                maquinariaDao.eliminarSincronizadosNoPresentes(uidsRemotos)
            }
        } catch (e: Exception) {
            // Manejar error o ignorar en offline
        }
    }

    private fun MaquinariaModel.aEntity(estadoSync: String): MaquinariaEntity {
        return MaquinariaEntity(
            uid = uid,
            codigoMaquinaria = codigoMaquinaria,
            nombre = nombre,
            tipo = tipo,
            marca = marca,
            modelo = modelo,
            placaSerie = placaSerie,
            anio = anio,
            estado = estado,
            horometroActual = horometroActual,
            horometroUltimoMantenimiento = horometroUltimoMantenimiento,
            ubicacionActual = ubicacionActual,
            imagenUrl = imagenUrl,
            observaciones = observaciones,
            fechaRegistro = fechaRegistro,
            fechaActualizacion = fechaActualizacion,
            registradoPor = registradoPor,
            estadoSync = estadoSync
        )
    }

    private fun MaquinariaEntity.aModel(): MaquinariaModel {
        return MaquinariaModel(
            uid = uid,
            codigoMaquinaria = codigoMaquinaria,
            nombre = nombre,
            tipo = tipo,
            marca = marca,
            modelo = modelo,
            placaSerie = placaSerie,
            anio = anio,
            estado = estado,
            horometroActual = horometroActual,
            horometroUltimoMantenimiento = horometroUltimoMantenimiento,
            ubicacionActual = ubicacionActual,
            imagenUrl = imagenUrl,
            observaciones = observaciones,
            fechaRegistro = fechaRegistro,
            fechaActualizacion = fechaActualizacion,
            registradoPor = registradoPor
        )
    }
}