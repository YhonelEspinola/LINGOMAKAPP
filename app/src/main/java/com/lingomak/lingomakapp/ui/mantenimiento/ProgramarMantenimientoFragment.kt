package com.lingomak.lingomakapp.ui.mantenimiento

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
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
import com.lingomak.lingomakapp.databinding.FragmentProgramarMantenimientoBinding
import com.lingomak.lingomakapp.ui.maquinaria.MaquinariaViewModel
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProgramarMantenimientoFragment : Fragment() {

    private var _binding: FragmentProgramarMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val maquinariaViewModel: MaquinariaViewModel by viewModels()
    private val mantenimientoViewModel: MantenimientoViewModel by viewModels()
    private val userRepository = UserRepository()

    private var listaMaquinarias: List<MaquinariaModel> = emptyList()
    private var listaOperarios: List<UserModel> = emptyList()

    private var maquinariaSeleccionada: MaquinariaModel? = null
    private var operarioSeleccionado: UserModel? = null

    private val imagenesReporteLocal: MutableList<String> = mutableListOf()
    private lateinit var adapterImagenes: EvidenciasAdapter
    private var currentPhotoPath: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) abrirCamara()
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { iniciarRecorte(it) }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val file = File(currentPhotoPath ?: return@registerForActivityResult)
                iniciarRecorte(Uri.fromFile(file))
            }
        }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val resultUri = result.data?.let { UCrop.getOutput(it) }
                resultUri?.let { agregarImagenALista(it) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgramarMantenimientoBinding.inflate(inflater, container, false)

        configurarRecyclerView()
        configurarSpinners()
        configurarEventos()
        observarMantenimientoViewModel()
        cargarMaquinaria()
        cargarOperarios()
        maquinariaViewModel.listarMaquinarias()

        return binding.root
    }

    private fun configurarRecyclerView() {
        adapterImagenes = EvidenciasAdapter(imagenesReporteLocal) { posicion ->
            imagenesReporteLocal.removeAt(posicion)
            adapterImagenes.notifyDataSetChanged()
            actualizarVisibilidadBotonAgregar()
        }
        binding.rvImagenesReporte.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvImagenesReporte.adapter = adapterImagenes
    }

    private fun agregarImagenALista(uri: Uri) {
        imagenesReporteLocal.add(uri.toString())
        adapterImagenes.notifyDataSetChanged()
        actualizarVisibilidadBotonAgregar()
    }

    private fun actualizarVisibilidadBotonAgregar() {
        binding.cardSubirImagenReferencia.visibility =
            if (imagenesReporteLocal.size >= 3) View.GONE else View.VISIBLE
    }

    private fun configurarSpinners() {
        val tipos = listOf("PREVENTIVO", "CORRECTIVO", "PREDICTIVO")
        binding.spTipoMantenimiento.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            tipos
        )
    }

    private fun cargarOperarios() {
        userRepository.listarOperarios({ usuarios ->
            listaOperarios = usuarios.filter { it.estado == "ACTIVO" }
            val nombres = listaOperarios.map { it.nombre }
            
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nombres)
            binding.actvResponsable.setAdapter(adapter)
        }, { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        })
    }

    private fun configurarEventos() {
        binding.etFechaProgramada.setOnClickListener { mostrarDatePicker() }
        binding.cardSubirImagenReferencia.setOnClickListener { mostrarSelectorImagen() }
        binding.btnGuardarMantenimiento.setOnClickListener { validarFormulario() }

        binding.actvMaquinaria.setOnItemClickListener { _, _, position, _ ->
            val selection = binding.actvMaquinaria.adapter.getItem(position) as String
            maquinariaSeleccionada = listaMaquinarias.find { "${it.nombre} (${it.codigoMaquinaria})" == selection }
            binding.etHorometroProgramado.setText(maquinariaSeleccionada?.horometroActual.toString())
        }

        binding.actvResponsable.setOnItemClickListener { _, _, position, _ ->
            val selection = binding.actvResponsable.adapter.getItem(position) as String
            operarioSeleccionado = listaOperarios.find { it.nombre == selection }
        }
    }

    private fun mostrarSelectorImagen() {
        val opciones = arrayOf("Cámara", "Galería")
        AlertDialog.Builder(requireContext())
            .setTitle("Subir Imagen de Referencia")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> verificarPermisosYCamara()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun verificarPermisosYCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            abrirCamara()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val photoFile = createImageFile()
            val photoURI = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            cameraLauncher.launch(intent)
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Error al abrir cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("REF_${ts}_", ".jpg", dir).apply { currentPhotoPath = absolutePath }
    }

    private fun iniciarRecorte(uri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "CROP_${System.currentTimeMillis()}.jpg"))
        val options = UCrop.Options()
        options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.primary))
        
        val intent = UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800)
            .withOptions(options)
            .getIntent(requireContext())
            
        cropLauncher.launch(intent)
    }

    private fun mostrarDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Seleccionar fecha")
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            val date = Date(selection)
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.etFechaProgramada.setText(format.format(date))
        }
        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun obtenerPrioridadSeleccionada(): String {
        return when (binding.rgPrioridad.checkedRadioButtonId) {
            R.id.rbPrioridadAlta -> "ALTA"
            R.id.rbPrioridadMedia -> "MEDIA"
            R.id.rbPrioridadBaja -> "BAJA"
            else -> "MEDIA"
        }
    }

    private fun validarFormulario() {
        val desc = binding.etDescripcion.text.toString().trim()
        val fecha = binding.etFechaProgramada.text.toString().trim()
        val horometroStr = binding.etHorometroProgramado.text.toString().trim()
        
        if (maquinariaSeleccionada == null || operarioSeleccionado == null || desc.isEmpty() || fecha.isEmpty() || horometroStr.isEmpty()) {
            Toast.makeText(requireContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val m = MantenimientoModel(
            uid = UUID.randomUUID().toString(),
            codigoMantenimiento = generarCodigoMantenimiento(),
            uidMaquinaria = maquinariaSeleccionada!!.uid,
            codigoMaquinaria = maquinariaSeleccionada!!.codigoMaquinaria,
            nombreMaquinaria = maquinariaSeleccionada!!.nombre,
            tipoMaquinaria = maquinariaSeleccionada!!.tipo,
            tipoMantenimiento = binding.spTipoMantenimiento.selectedItem.toString(),
            descripcion = desc,
            fechaProgramada = fecha,
            estado = "PENDIENTE",
            prioridad = obtenerPrioridadSeleccionada(),
            responsable = operarioSeleccionado!!.nombre,
            responsableUid = operarioSeleccionado!!.uid,
            horometroProgramado = horometroStr.toIntOrNull() ?: 0,
            costoEstimado = binding.etCostoEstimado.text.toString().toDoubleOrNull() ?: 0.0,
            horometroReal = 0,
            costoReal = 0.0,
            observaciones = binding.etObservaciones.text.toString().trim(),
            fechaRegistro = obtenerFechaActual(),
            imagenesReporteLocal = imagenesReporteLocal
        )

        mostrarCargando(true)
        mantenimientoViewModel.agregarMantenimiento(m) {
            Toast.makeText(requireContext(), "Mantenimiento programado", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun generarCodigoMantenimiento(): String {
        val ts = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "MAN-$ts-$random"
    }

    private fun obtenerFechaActual(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun observarMantenimientoViewModel() {
        mantenimientoViewModel.mensajeError.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                mostrarCargando(false)
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarCargando(show: Boolean) {
        binding.progressGuardarMantenimiento.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnGuardarMantenimiento.isEnabled = !show
    }

    private fun cargarMaquinaria() {
        maquinariaViewModel.listaMaquinarias.observe(viewLifecycleOwner) { maquinarias ->
            listaMaquinarias = maquinarias
            val displayList = maquinarias.map { "${it.nombre} (${it.codigoMaquinaria})" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayList)
            binding.actvMaquinaria.setAdapter(adapter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
