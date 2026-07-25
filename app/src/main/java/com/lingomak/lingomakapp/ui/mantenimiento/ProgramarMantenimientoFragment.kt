package com.lingomak.lingomakapp.ui.mantenimiento

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.data.repository.SolicitudMantenimientoRepository
import com.lingomak.lingomakapp.data.repository.UserRepository
import com.lingomak.lingomakapp.databinding.FragmentProgramarMantenimientoBinding
import com.lingomak.lingomakapp.ui.maquinaria.MaquinariaViewModel
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ProgramarMantenimientoFragment : Fragment() {

    private var _binding: FragmentProgramarMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val maquinariaViewModel: MaquinariaViewModel by viewModels()
    private val mantenimientoViewModel: MantenimientoViewModel by viewModels()

    private lateinit var userRepository: UserRepository
    private val solicitudRepository = SolicitudMantenimientoRepository()

    private var listaMaquinarias: List<MaquinariaModel> = emptyList()
    private var listaOperarios: List<UserModel> = emptyList()

    private var maquinariaSeleccionada: MaquinariaModel? = null
    private var operarioSeleccionado: UserModel? = null

    /*
     * Datos recibidos cuando el fragment se abre desde una
     * solicitud automática de mantenimiento.
     */
    private var origen: String = ""
    private var uidSolicitud: String = ""
    private var uidMaquinariaSolicitud: String = ""
    private var codigoMaquinariaSolicitud: String = ""
    private var nombreMaquinariaSolicitud: String = ""
    private var tipoMaquinariaSolicitud: String = ""
    private var motivoSolicitud: String = ""
    private var fechaSugerida: String = ""
    private var horometroSolicitud: Int = 0

    /*
     * Evita volver a cargar los datos si el observable de
     * maquinarias emite varias veces.
     */
    private var datosSolicitudAplicados = false

    private val imagenesReporteLocal: MutableList<String> = mutableListOf()
    private lateinit var adapterImagenes: EvidenciasAdapter

    private var currentPhotoPath: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                abrirCamara()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Se requiere permiso de cámara",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val galleryLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                iniciarRecorte(it)
            }
        }

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == android.app.Activity.RESULT_OK) {

                val rutaFoto = currentPhotoPath
                    ?: return@registerForActivityResult

                val archivo = File(rutaFoto)

                if (archivo.exists()) {
                    iniciarRecorte(Uri.fromFile(archivo))
                }
            }
        }

    private val cropLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == android.app.Activity.RESULT_OK) {

                val resultUri = result.data?.let {
                    UCrop.getOutput(it)
                }

                resultUri?.let {
                    agregarImagenALista(it)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        obtenerArgumentosSolicitud()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProgramarMantenimientoBinding.inflate(
            inflater,
            container,
            false
        )
        userRepository = UserRepository(requireContext())

        configurarRecyclerView()
        configurarSpinners()
        configurarEventos()
        observarMantenimientoViewModel()
        observarMaquinarias()

        cargarOperarios()
        maquinariaViewModel.listarMaquinarias()

        return binding.root
    }

    // ============================================================
    // SOLICITUD AUTOMÁTICA DE MANTENIMIENTO
    // ============================================================

    private fun obtenerArgumentosSolicitud() {

        origen = arguments
            ?.getString("origen")
            .orEmpty()

        uidSolicitud = arguments
            ?.getString("uidSolicitud")
            .orEmpty()

        uidMaquinariaSolicitud = arguments
            ?.getString("uidMaquinaria")
            .orEmpty()

        codigoMaquinariaSolicitud = arguments
            ?.getString("codigoMaquinaria")
            .orEmpty()

        nombreMaquinariaSolicitud = arguments
            ?.getString("nombreMaquinaria")
            .orEmpty()

        tipoMaquinariaSolicitud = arguments
            ?.getString("tipoMaquinaria")
            .orEmpty()

        motivoSolicitud = arguments
            ?.getString("motivoSolicitud")
            .orEmpty()

        fechaSugerida = arguments
            ?.getString("fechaSugerida")
            .orEmpty()

        horometroSolicitud = arguments
            ?.getInt("horometroProgramado", 0)
            ?: 0
    }

    private fun vieneDesdeSolicitud(): Boolean {
        return origen == "SOLICITUD_MANTENIMIENTO" &&
                uidSolicitud.isNotBlank()
    }

    private fun aplicarDatosDeSolicitud() {

        if (!vieneDesdeSolicitud()) {
            return
        }

        if (datosSolicitudAplicados) {
            return
        }

        val maquinariaEncontrada =
            listaMaquinarias.find {
                it.uid == uidMaquinariaSolicitud
            }

        if (maquinariaEncontrada == null) {

            Toast.makeText(
                requireContext(),
                "No se encontró la maquinaria relacionada con la solicitud",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        maquinariaSeleccionada = maquinariaEncontrada

        val nombreMostrado =
            "${maquinariaEncontrada.nombre} " +
                    "(${maquinariaEncontrada.codigoMaquinaria})"

        binding.actvMaquinaria.setText(
            nombreMostrado,
            false
        )

        /*
         * La maquinaria viene definida por la solicitud,
         * por eso el administrador no puede cambiarla.
         */
        binding.actvMaquinaria.isEnabled = false
        binding.actvMaquinaria.isFocusable = false
        binding.actvMaquinaria.isClickable = false

        /*
         * Toda solicitud generada por horómetro corresponde
         * a mantenimiento preventivo.
         */
        binding.spTipoMantenimiento.setSelection(0)
        binding.spTipoMantenimiento.isEnabled = false
        binding.spTipoMantenimiento.isClickable = false

        if (horometroSolicitud > 0) {
            binding.etHorometroProgramado.setText(
                horometroSolicitud.toString()
            )
        } else {
            binding.etHorometroProgramado.setText(
                maquinariaEncontrada.horometroActual.toString()
            )
        }

        binding.etHorometroProgramado.isEnabled = false

        if (motivoSolicitud.isNotBlank()) {
            binding.etDescripcion.setText(motivoSolicitud)
        }

        if (fechaSugerida.isNotBlank()) {
            binding.etFechaProgramada.setText(fechaSugerida)
        }

        datosSolicitudAplicados = true
    }

    // ============================================================
    // CONFIGURACIÓN DE INTERFAZ
    // ============================================================

    private fun configurarRecyclerView() {

        adapterImagenes = EvidenciasAdapter(
            imagenesReporteLocal
        ) { posicion ->

            if (posicion in imagenesReporteLocal.indices) {
                imagenesReporteLocal.removeAt(posicion)
                adapterImagenes.notifyDataSetChanged()
                actualizarVisibilidadBotonAgregar()
            }
        }

        binding.rvImagenesReporte.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )

        binding.rvImagenesReporte.adapter =
            adapterImagenes
    }

    private fun configurarSpinners() {

        val tipos = listOf(
            "PREVENTIVO",
            "CORRECTIVO"
        )

        binding.spTipoMantenimiento.adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                tipos
            )
    }

    private fun configurarEventos() {

        binding.etFechaProgramada.setOnClickListener {
            mostrarDatePicker()
        }

        binding.cardSubirImagenReferencia.setOnClickListener {
            mostrarSelectorImagen()
        }

        binding.btnGuardarMantenimiento.setOnClickListener {
            validarFormulario()
        }

        binding.actvMaquinaria.setOnItemClickListener {
                _, _, position, _ ->

            /*
             * Aunque el campo queda bloqueado cuando viene
             * desde una solicitud, se mantiene este evento
             * para la programación manual.
             */
            if (vieneDesdeSolicitud()) {
                return@setOnItemClickListener
            }

            val seleccion =
                binding.actvMaquinaria.adapter
                    .getItem(position)
                    .toString()

            maquinariaSeleccionada =
                listaMaquinarias.find {
                    "${it.nombre} (${it.codigoMaquinaria})" ==
                            seleccion
                }

            maquinariaSeleccionada?.let { maquinaria ->

                binding.etHorometroProgramado.setText(
                    maquinaria.horometroActual.toString()
                )
            }
        }

        binding.actvResponsable.setOnItemClickListener {
                _, _, position, _ ->

            val seleccion =
                binding.actvResponsable.adapter
                    .getItem(position)
                    .toString()

            if (seleccion == "Todos los operarios") {
                operarioSeleccionado = UserModel(uid = "TODOS", nombre = "Todos los operarios")
            } else {
                operarioSeleccionado =
                    listaOperarios.find {
                        it.nombre == seleccion
                    }
            }
        }
    }

    // ============================================================
    // CARGA DE MAQUINARIAS Y OPERARIOS
    // ============================================================

    private fun observarMaquinarias() {

        maquinariaViewModel.listaMaquinarias.observe(
            viewLifecycleOwner
        ) { maquinarias ->

            listaMaquinarias = maquinarias

            val nombresMaquinarias =
                maquinarias.map {
                    "${it.nombre} (${it.codigoMaquinaria})"
                }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                nombresMaquinarias
            )

            binding.actvMaquinaria.setAdapter(adapter)

            /*
             * Los datos se aplican después de recibir la lista,
             * porque recién aquí podemos obtener el objeto completo.
             */
            aplicarDatosDeSolicitud()
        }
    }

    private fun cargarOperarios() {

        userRepository.listarOperarios(
            { usuarios ->

                if (!isAdded || _binding == null) {
                    return@listarOperarios
                }

                listaOperarios =
                    usuarios.filter {
                        it.estado == "ACTIVO"
                    }

                val nombres = mutableListOf("Todos los operarios")
                nombres.addAll(listaOperarios.map { it.nombre })

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    nombres
                )

                binding.actvResponsable.setAdapter(adapter)
            },
            { error ->

                if (!isAdded) {
                    return@listarOperarios
                }

                Toast.makeText(
                    requireContext(),
                    error,
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // ============================================================
    // VALIDACIÓN Y REGISTRO
    // ============================================================

    private fun validarFormulario() {

        val descripcion =
            binding.etDescripcion.text
                .toString()
                .trim()

        val fechaProgramada =
            binding.etFechaProgramada.text
                .toString()
                .trim()

        val horometroTexto =
            binding.etHorometroProgramado.text
                .toString()
                .trim()

        val maquinaria = maquinariaSeleccionada
        val responsable = operarioSeleccionado

        if (maquinaria == null) {

            binding.actvMaquinaria.error =
                "Seleccione una maquinaria"

            return
        }

        if (responsable == null) {

            binding.actvResponsable.error =
                "Seleccione un responsable"

            return
        }

        if (descripcion.isBlank()) {

            binding.etDescripcion.error =
                "La descripción es obligatoria"

            binding.etDescripcion.requestFocus()

            return
        }

        if (fechaProgramada.isBlank()) {

            binding.etFechaProgramada.error =
                "Seleccione una fecha"

            return
        }

        val horometroProgramado =
            horometroTexto.toIntOrNull()

        if (horometroProgramado == null) {

            binding.etHorometroProgramado.error =
                "Ingrese un horómetro válido"

            return
        }

        val mantenimiento =
            MantenimientoModel(
                uid = UUID.randomUUID().toString(),
                codigoMantenimiento = generarCodigoMantenimiento(),

                uidMaquinaria = maquinaria.uid,
                codigoMaquinaria = maquinaria.codigoMaquinaria,
                nombreMaquinaria = maquinaria.nombre,
                tipoMaquinaria = maquinaria.tipo,

                tipoMantenimiento =
                    if (vieneDesdeSolicitud()) {
                        "PREVENTIVO"
                    } else {
                        binding.spTipoMantenimiento
                            .selectedItem
                            .toString()
                    },

                descripcion = descripcion,
                fechaProgramada = fechaProgramada,
                estado = "PENDIENTE",
                prioridad = obtenerPrioridadSeleccionada(),

                responsable = responsable.nombre,
                responsableUid = responsable.uid,

                horometroProgramado = horometroProgramado,
                horometroReal = 0,

                costoEstimado =
                    binding.etCostoEstimado.text
                        .toString()
                        .toDoubleOrNull()
                        ?: 0.0,

                costoReal = 0.0,

                observaciones =
                    binding.etObservaciones.text
                        .toString()
                        .trim(),

                fechaRegistro = obtenerFechaActual(),
                fechaActualizacion = obtenerFechaActual(),

                registradoPor =
                    FirebaseAuth.getInstance()
                        .currentUser
                        ?.uid
                        .orEmpty(),

                imagenesReporteLocal =
                    imagenesReporteLocal.toList()
            )

        guardarMantenimiento(mantenimiento)
    }

    private fun guardarMantenimiento(
        mantenimiento: MantenimientoModel
    ) {

        mostrarCargando(true)

        mantenimientoViewModel.agregarMantenimiento(
            mantenimiento
        ) {

            /*
             * Si fue una programación manual, el proceso termina
             * después de guardar el mantenimiento.
             */
            if (!vieneDesdeSolicitud()) {

                mostrarCargando(false)

                Toast.makeText(
                    requireContext(),
                    "Mantenimiento programado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()

                return@agregarMantenimiento
            }

            /*
             * Si se originó desde una solicitud automática,
             * se actualiza la solicitud después de crear el
             * mantenimiento.
             */
            marcarSolicitudComoConvertida(
                uidMantenimiento = mantenimiento.uid
            )
        }
    }

    private fun marcarSolicitudComoConvertida(
        uidMantenimiento: String
    ) {

        val uidAdministrador =
            FirebaseAuth.getInstance()
                .currentUser
                ?.uid
                .orEmpty()

        if (uidAdministrador.isBlank()) {

            mostrarCargando(false)

            Toast.makeText(
                requireContext(),
                "El mantenimiento fue creado, pero no se encontró la sesión del administrador",
                Toast.LENGTH_LONG
            ).show()

            parentFragmentManager.popBackStack()
            return
        }

        solicitudRepository.marcarComoConvertida(
            uidSolicitud = uidSolicitud,
            uidAdministrador = uidAdministrador,
            uidMantenimientoGenerado = uidMantenimiento,

            onSuccess = {

                if (!isAdded || _binding == null) {
                    return@marcarComoConvertida
                }

                mostrarCargando(false)

                Toast.makeText(
                    requireContext(),
                    "Mantenimiento programado y solicitud aprobada",
                    Toast.LENGTH_LONG
                ).show()

                /*
                 * Al cambiar la solicitud a
                 * CONVERTIDA_A_MANTENIMIENTO deja de cumplir
                 * el filtro PENDIENTE_APROBACION y desaparece
                 * automáticamente de las alertas.
                 */
                parentFragmentManager.popBackStack()
            },

            onError = { error ->

                if (!isAdded || _binding == null) {
                    return@marcarComoConvertida
                }

                mostrarCargando(false)

                Toast.makeText(
                    requireContext(),
                    "El mantenimiento fue creado, pero no se pudo " +
                            "actualizar la solicitud: $error",
                    Toast.LENGTH_LONG
                ).show()

                /*
                 * El mantenimiento ya fue guardado localmente,
                 * por eso evitamos volver a ejecutar el registro
                 * desde esta pantalla y crear un duplicado.
                 */
                parentFragmentManager.popBackStack()
            }
        )
    }

    // ============================================================
    // IMÁGENES
    // ============================================================

    private fun agregarImagenALista(uri: Uri) {

        if (imagenesReporteLocal.size >= 3) {

            Toast.makeText(
                requireContext(),
                "Solo puede agregar hasta 3 imágenes",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        /*
         * UCrop normalmente devuelve una URI de archivo.
         * Se guarda su ruta porque MantenimientoRepository
         * utiliza File(path) al sincronizar.
         */
        val rutaImagen =
            if (uri.scheme == "file") {
                uri.path
            } else {
                uri.toString()
            }

        if (rutaImagen.isNullOrBlank()) {
            return
        }

        imagenesReporteLocal.add(rutaImagen)
        adapterImagenes.notifyDataSetChanged()
        actualizarVisibilidadBotonAgregar()
    }

    private fun actualizarVisibilidadBotonAgregar() {

        binding.cardSubirImagenReferencia.visibility =
            if (imagenesReporteLocal.size >= 3) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun mostrarSelectorImagen() {

        val opciones = arrayOf(
            "Cámara",
            "Galería"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Subir imagen de referencia")
            .setItems(opciones) { _, opcion ->

                when (opcion) {
                    0 -> verificarPermisosYCamara()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun verificarPermisosYCamara() {

        val permisoConcedido =
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (permisoConcedido) {
            abrirCamara()
        } else {
            requestPermissionLauncher.launch(
                android.Manifest.permission.CAMERA
            )
        }
    }

    private fun abrirCamara() {

        val intent =
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        try {

            val archivoFoto =
                createImageFile()

            val photoUri =
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    archivoFoto
                )

            intent.putExtra(
                MediaStore.EXTRA_OUTPUT,
                photoUri
            )

            intent.addFlags(
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            cameraLauncher.launch(intent)

        } catch (exception: IOException) {

            Toast.makeText(
                requireContext(),
                "Error al crear el archivo de la cámara",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {

        val timestamp =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date())

        val directorio =
            requireContext().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES
            )

        return File.createTempFile(
            "REF_${timestamp}_",
            ".jpg",
            directorio
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun iniciarRecorte(uri: Uri) {

        val archivoDestino =
            File(
                requireContext().cacheDir,
                "CROP_${System.currentTimeMillis()}.jpg"
            )

        val destinationUri =
            Uri.fromFile(archivoDestino)

        val options =
            UCrop.Options().apply {

                setToolbarColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary
                    )
                )

                setCompressionQuality(85)
            }

        val intent =
            UCrop.of(uri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(800, 800)
                .withOptions(options)
                .getIntent(requireContext())

        cropLauncher.launch(intent)
    }
    // ============================================================
    // FECHA, PRIORIDAD Y CÓDIGO
    // ============================================================

    private fun mostrarDatePicker() {

        val picker =
            MaterialDatePicker.Builder
                .datePicker()
                .setTitleText("Seleccionar fecha")
                .build()

        picker.addOnPositiveButtonClickListener { selection ->

            val fecha = Date(selection)

            val formato =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            binding.etFechaProgramada.setText(
                formato.format(fecha)
            )
        }

        picker.show(
            parentFragmentManager,
            "DATE_PICKER"
        )
    }

    private fun obtenerPrioridadSeleccionada(): String {

        return when (
            binding.rgPrioridad.checkedRadioButtonId
        ) {

            R.id.rbPrioridadAlta -> "ALTA"
            R.id.rbPrioridadMedia -> "MEDIA"
            R.id.rbPrioridadBaja -> "BAJA"
            else -> "MEDIA"
        }
    }

    private fun generarCodigoMantenimiento(): String {

        val fecha =
            SimpleDateFormat(
                "yyyyMMdd",
                Locale.getDefault()
            ).format(Date())

        val numeroAleatorio =
            (1000..9999).random()

        return "MAN-$fecha-$numeroAleatorio"
    }

    private fun obtenerFechaActual(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())
    }

    // ============================================================
    // VIEWMODEL
    // ============================================================

    private fun observarMantenimientoViewModel() {

        mantenimientoViewModel.mensajeError.observe(
            viewLifecycleOwner
        ) { error ->

            if (error.isNotBlank()) {

                mostrarCargando(false)

                Toast.makeText(
                    requireContext(),
                    error,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mostrarCargando(mostrar: Boolean) {

        binding.progressGuardarMantenimiento.visibility =
            if (mostrar) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.btnGuardarMantenimiento.isEnabled =
            !mostrar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}