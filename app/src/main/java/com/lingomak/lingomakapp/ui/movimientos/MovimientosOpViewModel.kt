package com.lingomak.lingomakapp.ui.movimientos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.data.repository.MaquinariaRepository
import com.lingomak.lingomakapp.data.repository.MovimientoRepository
import com.lingomak.lingomakapp.data.repository.RepuestoRepository
import kotlinx.coroutines.launch

class MovimientosOpViewModel(application: Application) : AndroidViewModel(application) {

    private val movimientoRepository = MovimientoRepository(application)
    private val repuestoRepository = RepuestoRepository(application)
    private val maquinariaRepository = MaquinariaRepository(application)
    private val userDao = AppDatabase.getInstance(application).userDao()

    val todosLosRepuestos: LiveData<List<RepuestoModel>> = repuestoRepository.obtenerRepuestosObservable()
    val todasLasMaquinas: LiveData<List<MaquinariaModel>> = maquinariaRepository.obtenerMaquinariasObservable()

    private val _registroExitoso = MutableLiveData<Boolean>()
    val registroExitoso: LiveData<Boolean> get() = _registroExitoso

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun registrarSalida(movimiento: MovimientoModel) {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                val uid = user?.uid ?: ""
                var nombre = user?.displayName ?: ""

                if (nombre.isBlank() && uid.isNotEmpty()) {
                    nombre = userDao.obtenerPorUid(uid)?.nombre ?: "Operario"
                } else if (nombre.isBlank()) {
                    nombre = "Usuario"
                }

                val movConNombre = movimiento.copy(
                    registradoPor = uid,
                    nombreRegistradoPor = nombre
                )
                movimientoRepository.registrarMovimiento(movConNombre)
                _registroExitoso.postValue(true)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Error al registrar la salida")
            }
        }
    }
}
