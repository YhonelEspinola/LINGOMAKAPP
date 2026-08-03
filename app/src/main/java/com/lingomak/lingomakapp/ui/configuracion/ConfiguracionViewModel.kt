package com.lingomak.lingomakapp.ui.configuracion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MediatorLiveData
import com.lingomak.lingomakapp.data.model.CategoriaModel
import com.lingomak.lingomakapp.data.repository.ConfiguracionRepository
import kotlinx.coroutines.launch

class ConfiguracionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfiguracionRepository(application)

    private val _categoriasRepuestoCrudo = repository.obtenerCategoriasPorTipoObservable("REPUESTO", false)
    private val _categoriasMaquinariaCrudo = repository.obtenerCategoriasPorTipoObservable("MAQUINARIA", false)
    private val _categoriasCombustibleCrudo = repository.obtenerCategoriasPorTipoObservable("COMBUSTIBLE", false)

    private var queryRepuesto = ""
    private var queryMaquinaria = ""
    private var queryCombustible = ""

    val categoriasRepuesto = MediatorLiveData<List<CategoriaModel>>()
    val categoriasMaquinaria = MediatorLiveData<List<CategoriaModel>>()
    val categoriasCombustible = MediatorLiveData<List<CategoriaModel>>()

    init {
        categoriasRepuesto.addSource(_categoriasRepuestoCrudo) { filterRepuestos() }
        categoriasMaquinaria.addSource(_categoriasMaquinariaCrudo) { filterMaquinaria() }
        categoriasCombustible.addSource(_categoriasCombustibleCrudo) { filterCombustible() }
        
        viewModelScope.launch {
            repository.descargarCategoriasDeFirestore()
        }
    }

    fun buscarRepuesto(q: String) {
        queryRepuesto = q
        filterRepuestos()
    }

    fun buscarMaquinaria(q: String) {
        queryMaquinaria = q
        filterMaquinaria()
    }

    fun buscarCombustible(q: String) {
        queryCombustible = q
        filterCombustible()
    }

    private fun filterRepuestos() {
        val list = _categoriasRepuestoCrudo.value ?: emptyList()
        categoriasRepuesto.value = if (queryRepuesto.isBlank()) list 
                                   else list.filter { it.nombre.contains(queryRepuesto, true) }
    }

    private fun filterMaquinaria() {
        val list = _categoriasMaquinariaCrudo.value ?: emptyList()
        categoriasMaquinaria.value = if (queryMaquinaria.isBlank()) list 
                                     else list.filter { it.nombre.contains(queryMaquinaria, true) }
    }

    private fun filterCombustible() {
        val list = _categoriasCombustibleCrudo.value ?: emptyList()
        categoriasCombustible.value = if (queryCombustible.isBlank()) list 
                                      else list.filter { it.nombre.contains(queryCombustible, true) }
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
