package com.lingomak.lingomakapp.ui.usuarios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.data.repository.UserRepository
import kotlinx.coroutines.launch

class UsuariosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository(application)

    // La UI observa esta lista filtrada
    private val _usuariosFiltrados = MediatorLiveData<List<UserModel>>()
    val usuarios: LiveData<List<UserModel>> = _usuariosFiltrados

    private var allUsuarios: List<UserModel> = emptyList()
    private var filtroRol: String? = null
    private var filtroEstado: String? = null
    private var searchQuery: String = ""

    init {
        _usuariosFiltrados.addSource(repository.obtenerUsuariosObservable()) { lista: List<UserModel> ->
            allUsuarios = lista
            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {
        var lista = allUsuarios

        if (!filtroRol.isNullOrEmpty() && filtroRol != "TODOS") {
            lista = lista.filter { it.rol == filtroRol }
        }

        if (!filtroEstado.isNullOrEmpty() && filtroEstado != "TODOS") {
            lista = lista.filter { it.estado == filtroEstado }
        }

        if (searchQuery.isNotEmpty()) {
            lista = lista.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                it.correo.contains(searchQuery, ignoreCase = true)
            }
        }

        _usuariosFiltrados.value = lista
    }

    fun filtrarPorRol(rol: String?) {
        filtroRol = rol
        aplicarFiltros()
    }

    fun filtrarPorEstado(estado: String?) {
        filtroEstado = estado
        aplicarFiltros()
    }

    fun buscarUsuario(query: String) {
        searchQuery = query
        aplicarFiltros()
    }

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun listarUsuarios() {
        _loading.value = true
        viewModelScope.launch {
            repository.descargarUsuariosDeFirestore()
            _loading.postValue(false)
        }
    }

    fun cambiarEstadoUsuario(uid: String, nuevoEstado: String) {
        _loading.value = true
        repository.cambiarEstadoUsuario(
            uid = uid,
            nuevoEstado = nuevoEstado,
            onSuccess = {
                _loading.value = false
                // No es necesario llamar a listarUsuarios(), Room se actualizará vía SnapshotListener 
                // o mediante la próxima descarga manual.
            },
            onError = { mensaje ->
                _loading.value = false
                _error.value = mensaje
            }
        )
    }
}
