package com.lingomak.lingomakapp.ui.repuestos

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.data.local.AppDatabase
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.data.repository.RepuestoRepository
import com.lingomak.lingomakapp.data.repository.ConfiguracionRepository
import com.lingomak.lingomakapp.data.model.CategoriaModel
import com.lingomak.lingomakapp.utils.CodigoInternoGenerator
import com.lingomak.lingomakapp.data.worker.SincronizacionRepuestosWorker
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel del módulo de Repuestos, offline-first.
 *
 * IMPORTANTE: ahora extiende AndroidViewModel (no ViewModel) porque
 * RepuestoRepository necesita un Context de aplicación para acceder
 * a Room y para encolar trabajos de WorkManager. Esto NO cambia cómo
 * se usa desde los Fragments — `by viewModels()` lo sigue resolviendo
 * automáticamente, Android sabe inyectar el Application.
 *
 * La lista observable (repuestos) y el detalle (repuestoSeleccionado)
 * ahora vienen DIRECTO de Room vía LiveData reactivo: cualquier cambio
 * local o sincronizado desde Firestore se refleja solo, sin necesidad
 * de volver a llamar listarRepuestos() manualmente.
 */
class InventarioViewModel(application: Application) : AndroidViewModel(application) {

    enum class NivelCriticidad {
        TODOS, EN_STOCK, BAJO_STOCK, SIN_STOCK
    }

    private val repository = RepuestoRepository(application)
    private val configRepository = ConfiguracionRepository(application)

    val categorias: LiveData<List<CategoriaModel>> = configRepository.obtenerCategoriasPorTipoObservable("REPUESTO", true)

    // ----- LISTADO -----

    // Fuente cruda desde Room (sin filtrar).
    private val repuestosCrudo: LiveData<List<RepuestoModel>> = repository.obtenerRepuestosObservable()

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Estado actual de los filtros, para poder combinarlos.
    private var textoBusqueda: String = ""
    private var categoriaSeleccionada: String? = null
    private var estadoSeleccionado: String? = null
    private var criticidadSeleccionada: NivelCriticidad = NivelCriticidad.TODOS

    /**
     * Lista ya filtrada que observa el Fragment. Es un MediatorLiveData
     * que se recalcula cada vez que Room emite una nueva lista cruda
     * (escritura local o sincronización remota) Y cada vez que cambia
     * algún filtro en memoria (ver forzarRecalculoFiltros).
     */
    private val _repuestosFiltrados = MediatorLiveData<List<RepuestoModel>>()
    val repuestos: LiveData<List<RepuestoModel>> = _repuestosFiltrados

    init {
        _repuestosFiltrados.addSource(repuestosCrudo) { listaCruda ->
            _repuestosFiltrados.value = aplicarFiltros(listaCruda)
        }
    }

    // ----- DETALLE / EDICIÓN -----

    private var uidSeleccionadoActual: String? = null
    private var liveDataDetalleActual: LiveData<RepuestoModel?>? = null
    private val observerDetalle = androidx.lifecycle.Observer<RepuestoModel?> { repuesto ->
        _repuestoSeleccionado.value = repuesto
    }

    private val _repuestoSeleccionado = MutableLiveData<RepuestoModel?>()
    val repuestoSeleccionado: LiveData<RepuestoModel?> = _repuestoSeleccionado

    private val _guardadoExitoso = MutableLiveData(false)
    val guardadoExitoso: LiveData<Boolean> = _guardadoExitoso

    // UID del repuesto recién creado, necesario para generar el QR.
    private val _nuevoRepuestoId = MutableLiveData<String?>()
    val nuevoRepuestoId: LiveData<String?> = _nuevoRepuestoId

    // Código interno autogenerado, expuesto al Fragment para mostrarlo en pantalla
    // antes de que el usuario presione Guardar.
    private val _codigoGenerado = MutableLiveData<String>()
    val codigoGenerado: LiveData<String> = _codigoGenerado

    private val _actualizacionExitosa = MutableLiveData(false)
    val actualizacionExitosa: LiveData<Boolean> = _actualizacionExitosa

    /**
     * Suscribe el detalle de un repuesto puntual a Room. A diferencia
     * de la versión anterior (que buscaba en una lista en memoria),
     * ahora observa directo la fila de Room: si se actualiza localmente
     * o llega un cambio sincronizado, el detalle se refresca solo.
     *
     * Se desuscribe explícitamente el observer anterior antes de
     * suscribir uno nuevo, para no acumular observers ni dejar fugas
     * de memoria si se llama varias veces con distintos uid.
     */
    fun observarRepuestoPorUid(uid: String) {
        uidSeleccionadoActual = uid

        liveDataDetalleActual?.removeObserver(observerDetalle)

        val nuevoLiveData = repository.obtenerRepuestoPorUidObservable(uid)
        nuevoLiveData.observeForever(observerDetalle)
        liveDataDetalleActual = nuevoLiveData
    }

    /**
     * Compatibilidad con el nombre anterior, usado por Detalle/Editar.
     * Ahora simplemente delega a observarRepuestoPorUid, ya que con
     * Room la lectura es reactiva y no requiere un fetch puntual.
     */
    fun obtenerRepuestoPorUid(uid: String) {
        observarRepuestoPorUid(uid)
    }

    private var ultimoNombreCargado: String? = null
    private var estaCargandoMas = false

    fun listarRepuestos() {
        viewModelScope.launch {
            _loading.value = true
            repository.descargarCambiosPaginados(batchSize = 50)
            _loading.value = false
        }
    }

    fun cargarSiguienteLote() {
        if (estaCargandoMas) return
        
        viewModelScope.launch {
            estaCargandoMas = true
            val listaActual = _repuestosFiltrados.value ?: emptyList()
            if (listaActual.isNotEmpty()) {
                ultimoNombreCargado = listaActual.last().nombre
                repository.descargarCambiosPaginados(ultimoNombreCargado, 50)
            }
            estaCargandoMas = false
        }
    }

    fun buscarRepuesto(texto: String) {
        textoBusqueda = texto
        forzarRecalculoFiltros()
    }

    fun filtrarPorCategoria(categoria: String?) {
        categoriaSeleccionada = categoria
        forzarRecalculoFiltros()
    }

    fun filtrarPorEstado(estado: String?) {
        estadoSeleccionado = estado
        forzarRecalculoFiltros()
    }

    fun filtrarPorCriticidad(nivel: NivelCriticidad) {
        criticidadSeleccionada = nivel
        forzarRecalculoFiltros()
    }

    /**
     * Como _repuestosFiltrados depende de un MediatorLiveData sobre
     * Room, cambiar un filtro (que vive fuera de Room) no dispara una
     * nueva emisión por sí solo. Forzamos el recálculo reusando el
     * último valor crudo emitido por Room.
     */
    private fun forzarRecalculoFiltros() {
        repuestosCrudo.value?.let { listaCruda ->
            _repuestosFiltrados.value = aplicarFiltros(listaCruda)
        }
    }

    private fun aplicarFiltros(listaOriginal: List<RepuestoModel>): List<RepuestoModel> {
        var listaFiltrada = listaOriginal

        if (textoBusqueda.isNotBlank()) {
            listaFiltrada = listaFiltrada.filter { repuesto ->
                repuesto.nombre.contains(textoBusqueda, ignoreCase = true) ||
                    repuesto.codigoInterno.contains(textoBusqueda, ignoreCase = true)
            }
        }

        categoriaSeleccionada?.let { categoria ->
            listaFiltrada = listaFiltrada.filter { repuesto ->
                repuesto.categoria.equals(categoria, ignoreCase = true)
            }
        }

        estadoSeleccionado?.let { estado ->
            listaFiltrada = listaFiltrada.filter { repuesto ->
                repuesto.estado.equals(estado, ignoreCase = true)
            }
        }

        listaFiltrada = when (criticidadSeleccionada) {
            NivelCriticidad.SIN_STOCK -> listaFiltrada.filter { it.stockActual == 0 }
            NivelCriticidad.BAJO_STOCK -> listaFiltrada.filter { it.stockActual in 1..it.stockMinimo }
            NivelCriticidad.EN_STOCK -> listaFiltrada.filter { it.stockActual > it.stockMinimo }
            NivelCriticidad.TODOS -> listaFiltrada
        }

        return listaFiltrada
    }

    /**
     * Activa o inactiva un repuesto (baja lógica). Escribe en Room de
     * inmediato (funciona offline) y encola la sincronización.
     */
    fun cambiarEstadoRepuesto(uid: String, nuevoEstado: String) {
        viewModelScope.launch {
            _loading.value = true

            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            repository.cambiarEstadoRepuesto(
                uid = uid,
                nuevoEstado = nuevoEstado,
                actualizadoPor = currentUserUid,
                onSuccess = {
                    _loading.value = false
                },
                onFailure = { exception ->
                    _loading.value = false
                    _error.value = exception.message ?: "Error al cambiar el estado del repuesto"
                }
            )
        }
    }

    /**
     * Previsualiza el código interno para la categoría seleccionada
     * SIN incrementar el contador en la base de datos.
     * Se llama cuando el usuario selecciona una categoría en el Spinner.
     */
    fun generarCodigoInterno(categoria: String) {
        viewModelScope.launch {
            val db = AppDatabase.getInstance(getApplication())
            val contadorDao = db.contadorDao()
            val repuestoDao = db.repuestoDao()
            val codigo = CodigoInternoGenerator.previsualizar(categoria, contadorDao, repuestoDao)
            _codigoGenerado.value = codigo
        }
    }

    /**
     * Registra un nuevo repuesto e incrementa el contador de la categoría.
     */
    fun registrarRepuesto(
        nombre: String,
        categoria: String,
        marca: String,
        descripcion: String,
        stockActual: Int,
        stockMinimo: Int,
        stockMaximo: Int,
        ubicacionAlmacen: String,
        proveedorNombre: String = "",
        proveedorContacto: String = "",
        imagenLocalPath: String? = null,
        qrLocalPath: String? = null
    ) {
        viewModelScope.launch {
            _loading.value = true

            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val nuevoUid = java.util.UUID.randomUUID().toString()

            val db = AppDatabase.getInstance(getApplication())
            val contadorDao = db.contadorDao()
            val repuestoDao = db.repuestoDao()
            
            val codigoInterno = CodigoInternoGenerator.generar(categoria, contadorDao, repuestoDao)

            val nuevoRepuesto = RepuestoModel(
                uid = nuevoUid,
                codigoInterno = codigoInterno,
                nombre = nombre,
                categoria = categoria,
                marca = marca,
                descripcion = descripcion,
                stockActual = stockActual,
                stockMinimo = stockMinimo,
                stockMaximo = stockMaximo,
                ubicacionAlmacen = ubicacionAlmacen,
                proveedorNombre = proveedorNombre,
                proveedorContacto = proveedorContacto,
                estado = "ACTIVO",
                fechaRegistro = Date(),
                registradoPor = currentUserUid,
                actualizadoPor = currentUserUid
            )

            repository.guardarRepuesto(
                repuesto = nuevoRepuesto,
                esNuevo = true,
                imagenLocalPath = imagenLocalPath,
                qrLocalPath = qrLocalPath,
                onSuccess = {
                    _loading.value = false
                    _nuevoRepuestoId.value = nuevoUid
                    _guardadoExitoso.value = true
                },
                onFailure = { exception ->
                    _loading.value = false
                    _error.value = exception.message ?: "Error al registrar el repuesto"
                }
            )
        }
    }

    /**
     * Actualiza un repuesto existente, fijando fechaActualizacion y
     * actualizadoPor. Escribe en Room de inmediato y encola la
     * sincronización.
     */
    fun actualizarRepuesto(repuesto: RepuestoModel, imagenLocalPath: String? = null, qrLocalPath: String? = null) {
        viewModelScope.launch {
            _loading.value = true

            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            val repuestoActualizado = repuesto.copy(
                fechaActualizacion = Date(),
                actualizadoPor = currentUserUid
            )

            repository.guardarRepuesto(
                repuesto = repuestoActualizado,
                esNuevo = false,
                imagenLocalPath = imagenLocalPath,
                qrLocalPath = qrLocalPath,
                onSuccess = {
                    _loading.value = false
                    _actualizacionExitosa.value = true
                },
                onFailure = { exception ->
                    _loading.value = false
                    _error.value = exception.message ?: "Error al actualizar el repuesto"
                }
            )
        }
    }

    /**
     * Limpia los estados de éxito para evitar que al volver a una pantalla
     * se disparen los observadores de guardado/actualización previo.
     */
    fun limpiarEstados() {
        _guardadoExitoso.value = false
        _actualizacionExitosa.value = false
        _nuevoRepuestoId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        liveDataDetalleActual?.removeObserver(observerDetalle)
    }
}
