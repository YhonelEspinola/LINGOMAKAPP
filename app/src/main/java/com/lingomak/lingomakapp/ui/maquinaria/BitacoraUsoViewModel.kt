package com.lingomak.lingomakapp.ui.maquinaria

import android.app.Application
import androidx.lifecycle.*
import com.lingomak.lingomakapp.data.model.BitacoraUsoModel
import com.lingomak.lingomakapp.data.repository.RegistroUsoMaquinariaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BitacoraUsoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RegistroUsoMaquinariaRepository(application)

    private val _listaBitacora = MutableLiveData<List<BitacoraUsoModel>>()
    val listaBitacora: LiveData<List<BitacoraUsoModel>> get() = _listaBitacora

    private val _cargando = MutableLiveData<Boolean>()
    val cargando: LiveData<Boolean> get() = _cargando

    private val _registroSeleccionado = MutableLiveData<BitacoraUsoModel?>()
    val registroSeleccionado: LiveData<BitacoraUsoModel?> get() = _registroSeleccionado

    fun cargarBitacora() {
        _cargando.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Sincronizar con Firestore antes de cargar localmente
                repository.descargarCambiosDeFirestore()

                val registros = repository.obtenerTodosLosRegistros()
                val maquinarias = repository.obtenerTodasLasMaquinarias().associateBy { it.uid }
                val suministros = repository.obtenerTodosLosSuministros().associateBy { it.uidRegistroUso }

                val bitacora = registros.map { registro ->
                    BitacoraUsoModel(
                        registroUso = registro,
                        maquinaria = maquinarias[registro.uidMaquinaria],
                        suministro = suministros[registro.uid]
                    )
                }

                _listaBitacora.postValue(bitacora)
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _cargando.postValue(false)
            }
        }
    }

    fun cargarRegistroPorUid(uid: String) {
        _cargando.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val registros = repository.obtenerTodosLosRegistros()
                val registro = registros.find { it.uid == uid }
                
                if (registro != null) {
                    val maquinaria = repository.obtenerTodasLasMaquinarias().find { it.uid == registro.uidMaquinaria }
                    val suministro = repository.obtenerSuministroAsociado(registro.uid)
                    
                    _registroSeleccionado.postValue(BitacoraUsoModel(registro, maquinaria, suministro))
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
