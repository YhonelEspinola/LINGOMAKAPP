package com.lingomak.lingomakapp.ui.repuestos

import android.app.Application
import androidx.lifecycle.*
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.data.repository.RepuestoRepository
import kotlinx.coroutines.launch

class InventarioOpViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RepuestoRepository(application)
    private val maqRepository = com.lingomak.lingomakapp.data.repository.MaquinariaRepositoryOp(application)

    private val _repuestosOriginales = repository.obtenerRepuestosObservable()
    
    private val _textoBusqueda = MutableLiveData("")
    private val _categoriaSeleccionada = MutableLiveData<String?>(null)
    private val _criticidadSeleccionada = MutableLiveData(NivelCriticidad.TODOS)
    private val _estadoSeleccionado = MutableLiveData<String?>(null)

    val repuestos: LiveData<List<RepuestoModel>> = MediatorLiveData<List<RepuestoModel>>().apply {
        fun filtrar() {
            val lista = _repuestosOriginales.value ?: return
            val query = _textoBusqueda.value?.lowercase() ?: ""
            val cat = _categoriaSeleccionada.value
            val crit = _criticidadSeleccionada.value
            val est = _estadoSeleccionado.value

            value = lista.filter { rep ->
                val coincideTexto = rep.nombre.lowercase().contains(query) || rep.codigoInterno.lowercase().contains(query)
                val coincideCat = cat == null || rep.categoria == cat
                val coincideEst = est == null || rep.estado == est
                
                val coincideCrit = when(crit) {
                    NivelCriticidad.EN_STOCK -> rep.stockActual > rep.stockMinimo
                    NivelCriticidad.BAJO_STOCK -> rep.stockActual > 0 && rep.stockActual <= rep.stockMinimo
                    NivelCriticidad.SIN_STOCK -> rep.stockActual == 0
                    else -> true
                }

                coincideTexto && coincideCat && coincideEst && coincideCrit
            }
        }

        addSource(_repuestosOriginales) { filtrar() }
        addSource(_textoBusqueda) { filtrar() }
        addSource(_categoriaSeleccionada) { filtrar() }
        addSource(_criticidadSeleccionada) { filtrar() }
        addSource(_estadoSeleccionado) { filtrar() }
    }

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private var ultimoNombreCargado: String? = null
    private var estaCargandoMas = false

    fun listarRepuestos() {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.descargarCambiosPaginados(batchSize = 50)
                maqRepository.descargarMaquinariasDeFirestore()
            } catch (e: Exception) {
                _error.value = "Error al actualizar desde la nube"
            } finally {
                _loading.value = false
            }
        }
    }

    fun cargarSiguienteLote() {
        if (estaCargandoMas) return
        viewModelScope.launch {
            estaCargandoMas = true
            val listaActual = repuestos.value ?: emptyList()
            if (listaActual.isNotEmpty()) {
                ultimoNombreCargado = listaActual.last().nombre
                repository.descargarCambiosPaginados(ultimoNombreCargado, 50)
            }
            estaCargandoMas = false
        }
    }

    fun buscarRepuesto(query: String) {
        _textoBusqueda.value = query
    }

    fun filtrarPorCategoria(categoria: String?) {
        _categoriaSeleccionada.value = categoria
    }

    fun filtrarPorCriticidad(nivel: NivelCriticidad) {
        _criticidadSeleccionada.value = nivel
    }

    fun filtrarPorEstado(estado: String?) {
        _estadoSeleccionado.value = estado
    }

    enum class NivelCriticidad { TODOS, EN_STOCK, BAJO_STOCK, SIN_STOCK }
}