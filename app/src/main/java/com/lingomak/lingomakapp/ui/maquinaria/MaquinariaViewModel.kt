package com.lingomak.lingomakapp.ui.maquinaria

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.repository.MaquinariaRepository
import android.net.Uri

class MaquinariaViewModel : ViewModel() {

    private val repository = MaquinariaRepository()

    private val _listarMaquinarias = MutableLiveData<List<MaquinariaModel>>()
    val listaMaquinarias: LiveData<List<MaquinariaModel>> get() = _listarMaquinarias

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String>
        get() = _mensajeError


    fun listarMaquinarias(){
        repository.listarMaquinarias(
            onSuccess = { lista ->
                _listarMaquinarias.postValue(lista)
            },
            onError = {error ->
                _mensajeError.postValue(error)
            }
        )
    }

    fun agregarMaquinaria(
        maquinaria: MaquinariaModel,
        onSuccess: () -> Unit
    ){
        repository.agregarMaquinaria(
            maquinaria = maquinaria,

            onSuccess = {
                onSuccess()
            },
            onError = { error ->
                _mensajeError.postValue(error)
            }
        )
    }

    fun actualizarMaquinaria(
        maquinaria: MaquinariaModel,
        onSuccess: () -> Unit
    ){
        repository.actualizarMaquinaria(
            maquinaria = maquinaria,
            onSuccess = {
                onSuccess()
            },
            onError = { error ->
                _mensajeError.postValue(error)
            }
        )
    }

    fun cambiarEstadoMaquinaria(
        uid: String,
        nuevoEstado: String,
        onSuccess: () -> Unit
    ){
        repository.cambiarEstadoMaquinaria(
            uid = uid,
            nuevoEstado = nuevoEstado,

            onSuccess = {
                onSuccess()
            },

            onError = { error ->
                _mensajeError.postValue(error)
            }
        )
    }

    fun subirImagenMaquinaria(
        imagenUri: Uri,
        uid: String,
        onSuccess: (String) -> Unit
    ){
        repository.subirImagenMaquinaria(
            imagenUri = imagenUri,
            uid = uid,
            onSuccess = {urlImagen ->
                onSuccess(urlImagen)
            },
            onError = {error ->
                _mensajeError.postValue(error)
            }
        )
    }

}