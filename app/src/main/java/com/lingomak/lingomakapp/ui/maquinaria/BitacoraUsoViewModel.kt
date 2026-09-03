package com.lingomak.lingomakapp.ui.maquinaria

import android.app.Application
import androidx.lifecycle.*
import com.lingomak.lingomakapp.data.model.BitacoraUsoModel
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.RegistroUsoMaquinariaModel
import com.lingomak.lingomakapp.data.repository.RegistroUsoMaquinariaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BitacoraUsoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RegistroUsoMaquinariaRepository(application)

    private val _listaBitacora = MediatorLiveData<List<BitacoraUsoModel>>()
    val listaBitacora: LiveData<List<BitacoraUsoModel>> get() = _listaBitacora

    private val _cargando = MutableLiveData<Boolean>()
    val cargando: LiveData<Boolean> get() = _cargando

    private val _registroSeleccionado = MutableLiveData<BitacoraUsoModel?>()
    val registroSeleccionado: LiveData<BitacoraUsoModel?> get() = _registroSeleccionado

    private var sourceRegistros: LiveData<List<RegistroUsoMaquinariaModel>>? = null
    private var sourceMaquinarias: LiveData<List<MaquinariaModel>>? = null

    fun cargarBitacora() {
        // Remover fuentes previas si existen
        sourceRegistros?.let { _listaBitacora.removeSource(it) }
        sourceMaquinarias?.let { _listaBitacora.removeSource(it) }

        val registrosLive = repository.obtenerTodosObservable()
        val maquinariasLive = repository.obtenerMaquinariasActivasObservable()

        sourceRegistros = registrosLive
        sourceMaquinarias = maquinariasLive

        _listaBitacora.addSource(registrosLive) { registros ->
            combinarDatos(registros, sourceMaquinarias?.value ?: emptyList())
        }
        _listaBitacora.addSource(maquinariasLive) { maquinarias ->
            combinarDatos(sourceRegistros?.value ?: emptyList(), maquinarias)
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.descargarCambiosDeFirestore()
        }
    }

    private fun combinarDatos(
        registros: List<RegistroUsoMaquinariaModel>,
        maquinarias: List<MaquinariaModel>
    ) {
        val mapMaq = maquinarias.associateBy { it.uid }
        val bitacora = registros.map { registro ->
            BitacoraUsoModel(
                registroUso = registro,
                maquinaria = mapMaq[registro.uidMaquinaria]
            )
        }
        _listaBitacora.postValue(bitacora)
    }

    fun cargarRegistroPorUid(uid: String) {
        _cargando.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val registros = repository.obtenerTodosLosRegistros()
                val registro = registros.find { it.uid == uid }
                
                if (registro != null) {
                    val maquinaria = repository.obtenerTodasLasMaquinarias().find { it.uid == registro.uidMaquinaria }
                    _registroSeleccionado.postValue(BitacoraUsoModel(registro, maquinaria))
                } else {
                    _registroSeleccionado.postValue(null)
                }
            } catch (e: Exception) {
                _registroSeleccionado.postValue(null)
            } finally {
                _cargando.postValue(false)
            }
        }
    }
}
