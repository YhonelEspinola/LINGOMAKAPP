package com.lingomak.lingomakapp.ui.configuracion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lingomak.lingomakapp.data.model.CategoriaModel
import com.lingomak.lingomakapp.data.repository.ConfiguracionRepository
import kotlinx.coroutines.launch

class ConfiguracionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfiguracionRepository(application)

    val categoriasRepuesto: LiveData<List<CategoriaModel>> = repository.obtenerCategoriasPorTipoObservable("REPUESTO", false)
    val categoriasMaquinaria: LiveData<List<CategoriaModel>> = repository.obtenerCategoriasPorTipoObservable("MAQUINARIA", false)

    init {
        viewModelScope.launch {
            repository.descargarCategoriasDeFirestore()
        }
    }

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _exito = MutableLiveData<Boolean>()
    val exito: LiveData<Boolean> = _exito

    fun guardarCategoria(nombre: String, tipo: String) {
        if (nombre.isBlank()) {
            _error.value = "El nombre no puede estar vacío"
            return
        }
        
        val nueva = CategoriaModel(nombre = nombre, tipo = tipo)
        repository.guardarCategoria(nueva, {
            _exito.postValue(true)
        }, {
            _error.postValue(it)
        })
    }

    fun eliminarCategoria(uid: String) {
        repository.eliminarCategoria(uid, {
            // Room se actualizará solo
        }, {
            _error.postValue(it)
        })
    }
    
    fun resetExito() {
        _exito.value = false
    }
}
