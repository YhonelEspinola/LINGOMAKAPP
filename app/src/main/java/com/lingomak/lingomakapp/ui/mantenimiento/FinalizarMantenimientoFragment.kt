package com.lingomak.lingomakapp.ui.mantenimiento

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.data.repository.ConsumoRepuesto
import com.lingomak.lingomakapp.data.repository.RepuestoRepository
import com.lingomak.lingomakapp.databinding.DialogSeleccionarInsumoBinding
import com.lingomak.lingomakapp.databinding.FragmentFinalizarMantenimientoBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FinalizarMantenimientoFragment : Fragment() {

    private var _binding: FragmentFinalizarMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MantenimientoViewModel by viewModels()
    private lateinit var repuestoRepository: RepuestoRepository

    private var uidMantenimiento = ""
    private var uidMaquinaria = ""
    private var estadoMantenimiento = ""
    private var horometroActualMaquina = 0

    private val insumosSeleccionados: MutableList<ConsumoRepuesto> = mutableListOf()
    private lateinit var adapterInsumos: InsumoMantenimientoAdapter

    private var listaRepuestosDisponibles: List<RepuestoModel> = emptyList()

    private val imagenesFinalizacionLocal: MutableList<String> = mutableListOf()
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
        _binding = FragmentFinalizarMantenimientoBinding.inflate(inflater, container, false)
        repuestoRepository = RepuestoRepository(requireContext())
        uidMantenimiento = arguments?.getString("uid") ?: ""

        cargarDatos()
        configurarRecyclerView()
        configurarEventos()
        observarViewModel()
        cargarRepuestos()

        return binding.root
    }

    private fun cargarDatos() {
        viewModel.obtenerMantenimientoPorUid(uidMantenimiento) { m ->
            uidMaquinaria = m.uidMaquinaria
            estadoMantenimiento = m.estado
            horometroActualMaquina = m.horometroProgramado
            
            binding.tvCodigoFinalizar.text = "CÓDIGO: ${m.codigoMantenimiento}"
            binding.tvMaquinariaFinalizar.text = "Maquinaria: ${m.nombreMaquinaria}"
            binding.tvDescripcionFinalizar.text = "Descripción: ${m.descripcion}"
            binding.etHorometroReal.setText(m.horometroProgramado.toString())
            binding.etCostoReal.setText(m.costoEstimado.toString())
        }
    }

    private fun configurarRecyclerView() {
        adapterInsumos = InsumoMantenimientoAdapter(insumosSeleccionados) { posicion ->
            insumosSeleccionados.removeAt(posicion)
            adapterInsumos.notifyItemRemoved(posicion)
        }
        binding.rvInsumos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInsumos.adapter = adapterInsumos

        adapterImagenes = EvidenciasAdapter(imagenesFinalizacionLocal) { posicion ->
            imagenesFinalizacionLocal.removeAt(posicion)
            adapterImagenes.notifyDataSetChanged()
            actualizarVisibilidadBotonAgregar()
        }
        binding.rvImagenesFinalizacion.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvImagenesFinalizacion.adapter = adapterImagenes
    }

    private fun configurarEventos() {
        binding.btnAgregarInsumo.setOnClickListener { mostrarDialogoAgregarInsumo() }
        binding.cardSubirEvidencia.setOnClickListener { mostrarSelectorImagen() }
        binding.btnFinalizarMantenimiento.setOnClickListener { validarFormulario() }
    }

    private fun mostrarSelectorImagen() {
        val opciones = arrayOf("Cámara", "Galería")
        AlertDialog.Builder(requireContext())
            .setTitle("Subir Evidencia")
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
        return File.createTempFile("FIN_${ts}_", ".jpg", dir).apply { currentPhotoPath = absolutePath }
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

    private fun agregarImagenALista(uri: Uri) {
        imagenesFinalizacionLocal.add(uri.toString())
        adapterImagenes.notifyDataSetChanged()
        actualizarVisibilidadBotonAgregar()
    }

    private fun actualizarVisibilidadBotonAgregar() {
        binding.cardSubirEvidencia.visibility = if (imagenesFinalizacionLocal.size >= 3) View.GONE else View.VISIBLE
    }

    private fun cargarRepuestos() {
        repuestoRepository.obtenerRepuestosObservable().observe(viewLifecycleOwner) { lista ->
            listaRepuestosDisponibles = lista.filter { it.estado == "ACTIVO" }
        }
    }

    private fun mostrarDialogoAgregarInsumo() {
        val dialogBinding = DialogSeleccionarInsumoBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Agregar", null) // Lo manejamos manual para no cerrar si hay error
            .setNegativeButton("Cancelar", null)
            .create()

        val adapter = RepuestoDropdownAdapter(requireContext(), listaRepuestosDisponibles)
        dialogBinding.actvRepuesto.setAdapter(adapter)

        var repuestoSeleccionado: RepuestoModel? = null
        dialogBinding.actvRepuesto.setOnItemClickListener { _, _, position, _ ->
            repuestoSeleccionado = adapter.getItem(position)
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val cantStr = dialogBinding.etCantidadInsumo.text.toString().trim()
                if (repuestoSeleccionado == null || cantStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Seleccione repuesto y cantidad", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val cant = cantStr.toInt()
                if (cant > (repuestoSeleccionado?.stockActual ?: 0)) {
                    Toast.makeText(requireContext(), "Stock insuficiente. Disponible: ${repuestoSeleccionado?.stockActual}", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                insumosSeleccionados.add(ConsumoRepuesto(repuestoSeleccionado!!.uid, repuestoSeleccionado!!.nombre, cant))
                adapterInsumos.notifyDataSetChanged()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun validarFormulario() {
        val horometroStr = binding.etHorometroReal.text.toString().trim()
        val costoStr = binding.etCostoReal.text.toString().trim()
        val obs = binding.etObservacionesFinales.text.toString().trim()

        if (horometroStr.isEmpty() || costoStr.isEmpty()) {
            Toast.makeText(requireContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val hReal = horometroStr.toInt()
        val cReal = costoStr.toDouble()

        if (imagenesFinalizacionLocal.isEmpty()) {
            Toast.makeText(requireContext(), "Debe subir al menos una evidencia", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCargando(true)
        viewModel.finalizarMantenimiento(
            uid = uidMantenimiento,
            uidMaquinaria = uidMaquinaria,
            fechaRealizada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            horometroReal = hReal,
            costoReal = cReal,
            observacionesFinales = obs,
            insumos = insumosSeleccionados,
            imagenesFinalizacionLocal = imagenesFinalizacionLocal
        ) {
            Toast.makeText(requireContext(), "Mantenimiento finalizado con éxito", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun observarViewModel() {
        viewModel.mensajeError.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                mostrarCargando(false)
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarCargando(show: Boolean) {
        binding.progressFinalizarMantenimiento.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnFinalizarMantenimiento.isEnabled = !show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
