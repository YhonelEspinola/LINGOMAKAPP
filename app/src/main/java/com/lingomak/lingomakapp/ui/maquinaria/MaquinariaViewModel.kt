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
    private val userRepo = com.lingomak.lingomakapp.data.repository.UserRepository(application)
    private val suministroRepo = com.lingomak.lingomakapp.data.repository.SuministroRepository(application)

    val categorias: LiveData<List<CategoriaModel>> = configRepository.obtenerCategoriasPorTipoObservable("MAQUINARIA", true)

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    private val _consumoPromedio = MutableLiveData<Double?>()
    val consumoPromedio: LiveData<Double?> get() = _consumoPromedio

    val listaMaquinarias: LiveData<List<MaquinariaModel>> = repository.obtenerMaquinariasObservable()
    val maquinariasActivas: LiveData<List<MaquinariaModel>> = repository.obtenerMaquinariasActivasObservable()
    val operariosActivos: LiveData<List<com.lingomak.lingomakapp.data.model.UserModel>> = userRepo.obtenerOperariosActivosObservable()

    fun obtenerMaquinariaPorUid(uid: String): LiveData<MaquinariaModel?> {
        return repository.obtenerMaquinariaPorUidObservable(uid)
    }

    fun cargarConsumoPromedio(uidMaquinaria: String) {
        viewModelScope.launch {
            val consumo = suministroRepo.calcularConsumoGlsHora(uidMaquinaria)
            _consumoPromedio.postValue(consumo)
        }
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
