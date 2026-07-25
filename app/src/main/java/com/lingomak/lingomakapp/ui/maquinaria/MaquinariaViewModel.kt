package com.lingomak.lingomakapp.ui.maquinaria

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.repository.MaquinariaRepository
import com.lingomak.lingomakapp.data.repository.ConfiguracionRepository
import com.lingomak.lingomakapp.data.model.CategoriaModel
import android.net.Uri
import kotlinx.coroutines.launch

class MaquinariaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MaquinariaRepository(application)
    private val configRepository = ConfiguracionRepository(application)

    val categorias: LiveData<List<CategoriaModel>> = configRepository.obtenerCategoriasPorTipoObservable("MAQUINARIA", true)

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    val listaMaquinarias: LiveData<List<MaquinariaModel>> = repository.obtenerMaquinariasObservable()

    fun obtenerMaquinariaPorUid(uid: String): LiveData<MaquinariaModel?> {
        return repository.obtenerMaquinariaPorUidObservable(uid)
    }

    fun listarMaquinarias(){
        viewModelScope.launch {
            repository.descargarMaquinariasDeFirestore()
        }
    }

    fun agregarMaquinaria(
        maquinaria: MaquinariaModel,
        onSuccess: () -> Unit
    ){
        viewModelScope.launch {
            repository.agregarMaquinaria(
                maquinaria = maquinaria,
                onSuccess = onSuccess,
                onError = { _mensajeError.postValue(it) }
            )
        }
    }

    fun actualizarMaquinaria(
        maquinaria: MaquinariaModel,
        onSuccess: () -> Unit
    ){
        viewModelScope.launch {
            repository.actualizarMaquinaria(
                maquinaria = maquinaria,
                onSuccess = onSuccess,
                onError = { _mensajeError.postValue(it) }
            )
        }
    }

    fun cambiarEstadoMaquinaria(
        uid: String,
        nuevoEstado: String,
        onSuccess: () -> Unit
    ){
        viewModelScope.launch {
            repository.cambiarEstadoMaquinaria(
                uid = uid,
                nuevoEstado = nuevoEstado,
                onSuccess = onSuccess,
                onError = { _mensajeError.postValue(it) }
            )
        }
    }

    fun subirImagenMaquinaria(
        imagenUri: Uri,
        uid: String,
        onSuccess: (String) -> Unit
    ){
        repository.subirImagenMaquinaria(
            imagenUri = imagenUri,
            uid = uid,
            onSuccess = onSuccess,
            onError = { _mensajeError.postValue(it) }
        )
    }
}
