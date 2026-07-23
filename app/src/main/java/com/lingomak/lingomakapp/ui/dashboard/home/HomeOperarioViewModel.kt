package com.lingomak.lingomakapp.ui.dashboard.home

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.repository.AlertasRepository

class HomeOperarioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val alertasRepository = AlertasRepository(application)

    private val _usuarioUid = MutableLiveData<String?>()
    
    init {
        _usuarioUid.value = FirebaseAuth.getInstance().currentUser?.uid
    }

    // 1. Máquina en uso
    val ultimoUso = _usuarioUid.switchMap { uid ->
        if (uid != null) database.registroUsoMaquinariaDao().obtenerUltimoUsoObservable(uid)
        else MutableLiveData(null)
    }

    val maquinariaEnUso = ultimoUso.switchMap { uso ->
        if (uso != null) database.maquinariaDao().obtenerPorUidObservable(uso.uidMaquinaria)
        else MutableLiveData(null)
    }

    // 2. Alertas pendientes
    private val _listaAlertas = MutableLiveData<List<AlertaModel>>(emptyList())
    val totalAlertas: LiveData<Int> = _listaAlertas.map { it.size }
    val alertasCriticas: LiveData<Int> = _listaAlertas.map { lista ->
        lista.count { it.prioridad == "ALTA" || it.tipo == "VENCIDO" }
    }

    fun cargarAlertas() {
        val uid = _usuarioUid.value
        if (uid != null) {
            alertasRepository.listarAlertas(
                userUid = uid,
                esOperario = true,
                onSuccess = { _listaAlertas.postValue(it) },
                onError = { }
            )
        }
    }

    // 3. Sincronización
    val pendientesSync = MediatorLiveData<Int>().apply {
        val repuestoCount = database.repuestoDao().obtenerPendientesDeSincronizarCount()
        val maquinariaCount = database.maquinariaDao().obtenerPendientesDeSincronizarCount()
        val movimientoCount = database.movimientoDao().obtenerPendientesDeSincronizarCount()
        val mantenimientoCount = database.mantenimientoDao().obtenerPendientesDeSincronizarCount()
        val registroUsoCount = database.registroUsoMaquinariaDao().obtenerPendientesDeSincronizarCount()
        val solicitudCount = database.solicitudMantenimientoDao().obtenerPendientesDeSincronizarCount()

        fun update() {
            value = (repuestoCount.value ?: 0) +
                    (maquinariaCount.value ?: 0) +
                    (movimientoCount.value ?: 0) +
                    (mantenimientoCount.value ?: 0) +
                    (registroUsoCount.value ?: 0) +
                    (solicitudCount.value ?: 0)
        }

        addSource(repuestoCount) { update() }
        addSource(maquinariaCount) { update() }
        addSource(movimientoCount) { update() }
        addSource(mantenimientoCount) { update() }
        addSource(registroUsoCount) { update() }
        addSource(solicitudCount) { update() }
    }
}
