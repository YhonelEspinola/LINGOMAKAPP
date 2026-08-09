package com.lingomak.lingomakapp.ui.repuestos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.lingomak.lingomakapp.databinding.FragmentAgregarRepuestoBinding
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgregarRepuestoFragment : Fragment() {
    private var _binding: FragmentAgregarRepuestoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventarioViewModel by activityViewModels()

    private var imagenLocalPath: String? = null
    private var qrLocalPath: String? = null
    private var currentPhotoPath: String? = null

    private var listaCategorias: List<String> = emptyList()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            abrirCamara()
        } else {
            Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                iniciarRecorte(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = File(currentPhotoPath ?: "")
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
                iniciarRecorte(uri)
            }
        }
    }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                procesarImagenFinal(uri)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(requireContext(), "Error al recortar: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgregarRepuestoBinding.inflate(inflater, container, false)
        
        binding.etCodigoInterno.isEnabled = false // No permitir edición manual

        configurarObservadores()
        configurarEventos()
        return binding.root
    }

    private fun iniciarRecorte(uri: Uri) {
        val destinationFileName = "IMG_${System.currentTimeMillis()}.jpg"
        val destinationFile = File(requireContext().cacheDir, destinationFileName)
        val destinationUri = Uri.fromFile(destinationFile)

        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(80)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(false)
            setAspectRatioOptions(0, com.yalantis.ucrop.model.AspectRatio("1:1", 1f, 1f))
        }

        val uCrop = UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800) // Reescalado estándar industrial
            .withOptions(options)

        cropLauncher.launch(uCrop.getIntent(requireContext()))
    }

    private fun procesarImagenFinal(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            binding.ivFotoRepuesto.setImageBitmap(bitmap)
            binding.ivFotoRepuesto.visibility = View.VISIBLE
            binding.layoutPlaceholder.visibility = View.GONE
            
            // Guardar imagen en almacenamiento interno persistente
            val fileName = "REP_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            imagenLocalPath = file.absolutePath
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al procesar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private var categoriaSeleccionada: String? = null

    private fun configurarSpinnerCategorias(categorias: List<String>) {
        listaCategorias = categorias
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listaCategorias)
        binding.spinnerCategoria.setAdapter(adapter)
        binding.spinnerCategoria.setOnItemClickListener { parent, _, position, _ ->
            val seleccion = parent.getItemAtPosition(position).toString()
            categoriaSeleccionada = seleccion
            viewModel.generarCodigoInterno(seleccion)
        }
    }

    private fun configurarObservadores() {
        viewModel.categorias.observe(viewLifecycleOwner) { lista ->
            configurarSpinnerCategorias(lista.map { it.nombre })
        }
        viewModel.codigoGenerado.observe(viewLifecycleOwner) { codigo ->
            binding.etCodigoInterno.setText(codigo)
        }
        viewModel.guardadoExitoso.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(requireContext(), "Repuesto guardado con éxito", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                viewModel.limpiarEstados()
            }
        }
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
        viewModel.loading.observe(viewLifecycleOwner) { estaCargando ->
            binding.btnGuardarRepuesto.isEnabled = !estaCargando
        }
    }

    private fun configurarEventos() {
        binding.cardSubirImagen.setOnClickListener {
            mostrarDialogoSeleccionImagen()
        }

        binding.btnGuardarRepuesto.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val categoria = categoriaSeleccionada
            val codigoInterno = binding.etCodigoInterno.text.toString().trim()
            
            if (nombre.isEmpty() || categoria == null) {
                Toast.makeText(requireContext(), "Por favor complete los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (codigoInterno.isEmpty()) {
                Toast.makeText(requireContext(), "Generando código...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val stockActual = binding.etStockInicial.text.toString().toIntOrNull() ?: 0
            val stockMinimo = binding.etStockMinimo.text.toString().toIntOrNull() ?: 0
            val stockMaximo = binding.etStockMaximo.text.toString().toIntOrNull() ?: 0

            if (stockActual < 0 || stockMinimo < 0 || stockMaximo < 0) {
                Toast.makeText(requireContext(), "Las cantidades de stock no pueden ser negativas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (stockMinimo > stockMaximo) {
                Toast.makeText(requireContext(), "El stock mínimo no puede ser mayor al máximo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (stockActual > stockMaximo) {
                Toast.makeText(requireContext(), "El stock inicial no puede ser mayor al máximo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generar QR antes de registrar
            generarYGuardarQR(codigoInterno)

            viewModel.registrarRepuesto(
                nombre = nombre,
                categoria = categoria,
                marca = binding.etMarca.text.toString().trim(),
                descripcion = binding.etDescripcion.text.toString().trim(),
                stockActual = stockActual,
                stockMinimo = stockMinimo,
                stockMaximo = stockMaximo,
                ubicacionAlmacen = binding.etUbicacionAlmacen.text.toString().trim(),
                proveedorNombre = binding.etProveedorNombre.text.toString().trim(),
                proveedorContacto = binding.etProveedorContacto.text.toString().trim(),
                imagenLocalPath = imagenLocalPath,
                qrLocalPath = qrLocalPath
            )
        }
    }

    private fun generarYGuardarQR(texto: String) {
        try {
            val bitmap = com.lingomak.lingomakapp.utils.QRHelper.generarBitmapQR(texto)
            bitmap?.let {
                val fileName = "QR_${System.currentTimeMillis()}.png"
                val file = File(requireContext().filesDir, fileName)
                file.outputStream().use { out ->
                    it.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                qrLocalPath = file.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun verificarPermisosYCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            try {
                val photoFile: File? = try { createImageFile() } catch (ex: IOException) { null }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    cameraLauncher.launch(takePictureIntent)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply { currentPhotoPath = absolutePath }
    }

    private fun mostrarDialogoSeleccionImagen() {
        val opciones = arrayOf("Cámara", "Galería")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Subir Imagen")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> verificarPermisosYCamara()
                    1 -> {
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        galleryLauncher.launch(intent)
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
