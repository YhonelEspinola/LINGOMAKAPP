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
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.data.repository.UserRepository
import com.lingomak.lingomakapp.databinding.FragmentEditarMantenimientoBinding
import com.lingomak.lingomakapp.ui.maquinaria.MaquinariaViewModel
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditarMantenimientoFragment : Fragment() {

    private var _binding: FragmentEditarMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MantenimientoViewModel by viewModels()
    private val maquinariaViewModel: MaquinariaViewModel by viewModels()
    private lateinit var userRepository: UserRepository

    private var uidMantenimiento = ""
    private var mantenimientoActual: MantenimientoModel? = null

    private var listaMaquinarias: List<MaquinariaModel> = emptyList()
    private var listaOperarios: List<UserModel> = emptyList()

    private var maquinariaSeleccionada: MaquinariaModel? = null
    private var operarioSeleccionado: UserModel? = null

    private val imagenesReporteLocal: MutableList<String> = mutableListOf()
    private val imagenesExistentesRemotas: MutableList<String> = mutableListOf()
    private lateinit var adapterImagenes: EvidenciasAdapter

    private var currentPhotoPath: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) abrirCamara()
            else Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { iniciarRecorte(it) }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                currentPhotoPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) iniciarRecorte(Uri.fromFile(file))
                }
            }
        }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.let { UCrop.getOutput(it) }?.let { agregarImagenALista(it) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditarMantenimientoBinding.inflate(inflater, container, false)
        userRepository = UserRepository(requireContext())
        uidMantenimiento = arguments?.getString("uid") ?: ""

        configurarRecyclerView()
        configurarSpinners()
        configurarEventos()
        observarViewModel()
        
        cargarOperarios()
        maquinariaViewModel.listarMaquinarias()

        if (uidMantenimiento.isNotEmpty()) {
            cargarDatos()
        }

        return binding.root
    }

    private fun configurarRecyclerView() {
        val todasLasImagenes = mutableListOf<String>()
        adapterImagenes = EvidenciasAdapter(todasLasImagenes) { posicion ->
            if (posicion < imagenesExistentesRemotas.size) {
                imagenesExistentesRemotas.removeAt(posicion)
            } else {
                imagenesReporteLocal.removeAt(posicion - imagenesExistentesRemotas.size)
            }
            actualizarListaAdapter()
        }
        binding.rvEvidenciasEditar.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvEvidenciasEditar.adapter = adapterImagenes
    }

    private fun actualizarListaAdapter() {
        val todas = imagenesExistentesRemotas + imagenesReporteLocal
        adapterImagenes.actualizarLista(todas)
        binding.btnAgregarEvidenciaEditar.visibility = if (todas.size >= 3) View.GONE else View.VISIBLE
    }

    private fun cargarDatos() {
        viewModel.obtenerMantenimientoPorUid(uidMantenimiento) { m ->
            mantenimientoActual = m
            pintarDatos(m)
        }
    }

    private fun pintarDatos(m: MantenimientoModel) {
        binding.tvCodigoMantenimientoEditar.text = "Código: ${m.codigoMantenimiento}"
        
        // Maquinaria
        val nombreMaqStr = "${m.nombreMaquinaria} (${m.codigoMaquinaria})"
        binding.actvMaquinariaEditar.setText(nombreMaqStr, false)
        
        // Responsable
        binding.actvResponsableEditar.setText(m.responsable, false)
        if (m.responsableUid == "TODOS") {
            operarioSeleccionado = UserModel(uid = "TODOS", nombre = "Todos los operarios")
        }

        binding.etDescripcionEditar.setText(m.descripcion)
        binding.etFechaProgramadaEditar.setText(m.fechaProgramada)
        binding.etHorometroProgramadoEditar.setText(m.horometroProgramado.toString())
        binding.etCostoEstimadoEditar.setText(m.costoEstimado.toString())
        binding.etObservacionesEditar.setText(m.observaciones)

        binding.spTipoMantenimientoEditar.setText(m.tipoMantenimiento, false)

        when (m.prioridad) {
            "ALTA" -> binding.rbPrioridadAltaEditar.isChecked = true
            "MEDIA" -> binding.rbPrioridadMediaEditar.isChecked = true
            "BAJA" -> binding.rbPrioridadBajaEditar.isChecked = true
        }

        imagenesExistentesRemotas.clear()
        imagenesExistentesRemotas.addAll(m.imagenesReporte)
        
        // No borramos lo que ya estaba pendiente de subir localmente
        imagenesReporteLocal.clear()
        imagenesReporteLocal.addAll(m.imagenesReporteLocal)

        actualizarListaAdapter()
    }

    private fun configurarSpinners() {
        val tipos = listOf("PREVENTIVO", "CORRECTIVO")
        binding.spTipoMantenimientoEditar.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tipos))
    }

    private fun configurarEventos() {
        binding.etHorometroProgramadoEditar.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))
        binding.etCostoEstimadoEditar.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))

        // Bloquear pegado de texto en campos de fecha
        binding.etFechaProgramadaEditar.isLongClickable = false
        binding.etFechaProgramadaEditar.keyListener = null

        binding.etFechaProgramadaEditar.setOnClickListener { mostrarDatePicker() }
        binding.btnAgregarEvidenciaEditar.setOnClickListener { mostrarSelectorImagen() }
        binding.btnGuardarCambiosMantenimiento.setOnClickListener { validarYGuardar() }

        binding.actvMaquinariaEditar.setOnItemClickListener { _, _, position, _ ->
            val seleccion = binding.actvMaquinariaEditar.adapter.getItem(position).toString()
            maquinariaSeleccionada = listaMaquinarias.find { "${it.nombre} (${it.codigoMaquinaria})" == seleccion }
            maquinariaSeleccionada?.let { 
                if (binding.etHorometroProgramadoEditar.text.isNullOrBlank()) {
                    binding.etHorometroProgramadoEditar.setText(it.horometroActual.toString())
                }
            }
        }

        binding.actvResponsableEditar.setOnItemClickListener { _, _, position, _ ->
            val seleccion = binding.actvResponsableEditar.adapter.getItem(position).toString()
            if (seleccion == "Todos los operarios") {
                operarioSeleccionado = UserModel(uid = "TODOS", nombre = "Todos los operarios")
            } else {
                operarioSeleccionado = listaOperarios.find { it.nombre == seleccion }
            }
        }
    }

    private fun observarViewModel() {
        viewModel.mensajeError.observe(viewLifecycleOwner) { error ->
            if (error.isNotBlank()) {
                mostrarCargando(false)
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        maquinariaViewModel.listaMaquinarias.observe(viewLifecycleOwner) { maquinarias ->
            listaMaquinarias = maquinarias
            val nombres = maquinarias.map { "${it.nombre} (${it.codigoMaquinaria})" }
            binding.actvMaquinariaEditar.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nombres))
            
            // Si ya tenemos el mantenimiento, intentar setear el objeto seleccionado
            mantenimientoActual?.let { m ->
                maquinariaSeleccionada = maquinarias.find { it.uid == m.uidMaquinaria }
            }
        }
    }

    private fun cargarOperarios() {
        userRepository.listarOperarios({ usuarios ->
            if (!isAdded || _binding == null) return@listarOperarios
            listaOperarios = usuarios.filter { it.estado == "ACTIVO" }
            val nombres = mutableListOf("Todos los operarios")
            nombres.addAll(listaOperarios.map { it.nombre })
            binding.actvResponsableEditar.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nombres))
            
            mantenimientoActual?.let { m ->
                if (m.responsableUid != "TODOS") {
                    operarioSeleccionado = listaOperarios.find { it.uid == m.responsableUid }
                }
            }
        }, { error ->
            if (isAdded) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        })
    }

    private fun mostrarDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Seleccionar fecha").build()
        picker.addOnPositiveButtonClickListener { selection ->
            val fecha = Date(selection)
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.etFechaProgramadaEditar.setText(formato.format(fecha))
        }
        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun validarYGuardar() {
        val m = mantenimientoActual ?: return
        
        val descripcion = binding.etDescripcionEditar.text.toString().trim()
        val horometroStr = binding.etHorometroProgramadoEditar.text.toString().trim()
        val maq = maquinariaSeleccionada ?: (if (binding.actvMaquinariaEditar.text.isNotEmpty()) m else null)
        val resp = operarioSeleccionado ?: (if (binding.actvResponsableEditar.text.isNotEmpty()) m else null)

        if (descripcion.isEmpty() || horometroStr.isEmpty() || maq == null || resp == null) {
            Toast.makeText(requireContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val horometroProgramado = horometroStr.toDoubleOrNull() ?: 0.0
        val maquinaria = maquinariaSeleccionada ?: listaMaquinarias.find { it.uid == m.uidMaquinaria }
        
        if (maquinaria != null && horometroProgramado < maquinaria.horometroActual) {
            binding.etHorometroProgramadoEditar.error = "El horómetro programado no puede ser menor al actual (${maquinaria.horometroActual})"
            return
        }

        mostrarCargando(true)

        val nuevoEstado = if (m.estado == "VENCIDO" || m.estado == "CANCELADO") "PENDIENTE" else m.estado

        val mActualizado = m.copy(
            uidMaquinaria = if (maquinariaSeleccionada != null) maquinariaSeleccionada!!.uid else m.uidMaquinaria,
            codigoMaquinaria = if (maquinariaSeleccionada != null) maquinariaSeleccionada!!.codigoMaquinaria else m.codigoMaquinaria,
            nombreMaquinaria = if (maquinariaSeleccionada != null) maquinariaSeleccionada!!.nombre else m.nombreMaquinaria,
            tipoMaquinaria = if (maquinariaSeleccionada != null) maquinariaSeleccionada!!.tipo else m.tipoMaquinaria,
            
            responsable = if (operarioSeleccionado != null) operarioSeleccionado!!.nombre else m.responsable,
            responsableUid = if (operarioSeleccionado != null) operarioSeleccionado!!.uid else m.responsableUid,

            tipoMantenimiento = binding.spTipoMantenimientoEditar.text.toString(),
            prioridad = if (binding.rbPrioridadAltaEditar.isChecked) "ALTA" else if (binding.rbPrioridadMediaEditar.isChecked) "MEDIA" else "BAJA",
            descripcion = descripcion,
            fechaProgramada = binding.etFechaProgramadaEditar.text.toString(),
            horometroProgramado = horometroStr.toDoubleOrNull() ?: m.horometroProgramado,
            costoEstimado = binding.etCostoEstimadoEditar.text.toString().toDoubleOrNull() ?: m.costoEstimado,
            observaciones = binding.etObservacionesEditar.text.toString().trim(),
            estado = nuevoEstado,
            imagenesReporte = imagenesExistentesRemotas.toList(),
            imagenesReporteLocal = imagenesReporteLocal.toList()
        )

        viewModel.actualizarMantenimiento(mActualizado) {
            mostrarCargando(false)
            Toast.makeText(requireContext(), "Mantenimiento actualizado", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressEditarMantenimiento.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnGuardarCambiosMantenimiento.isEnabled = !mostrar
    }

    private fun mostrarSelectorImagen() {
        val opciones = arrayOf("Cámara", "Galería")
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar evidencia")
            .setItems(opciones) { _, op ->
                if (op == 0) verificarPermisosCamara() else galleryLauncher.launch("image/*")
            }.show()
    }

    private fun verificarPermisosCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            abrirCamara()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val file = createImageFile()
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            cameraLauncher.launch(intent)
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Error al crear archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("EDIT_${ts}_", ".jpg", dir).apply { currentPhotoPath = absolutePath }
    }

    private fun iniciarRecorte(uri: Uri) {
        val dest = File(requireContext().cacheDir, "CROP_${System.currentTimeMillis()}.jpg")
        val options = UCrop.Options().apply {
            setToolbarColor(ContextCompat.getColor(requireContext(), R.color.primary))
            setCompressionQuality(85)
        }
        val intent = UCrop.of(uri, Uri.fromFile(dest))
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800)
            .withOptions(options)
            .getIntent(requireContext())
        cropLauncher.launch(intent)
    }

    private fun agregarImagenALista(uri: Uri) {
        val path = if (uri.scheme == "file") uri.path else uri.toString()
        if (!path.isNullOrBlank()) {
            imagenesReporteLocal.add(path)
            actualizarListaAdapter()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
