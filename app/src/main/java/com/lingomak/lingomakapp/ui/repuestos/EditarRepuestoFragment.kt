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
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.databinding.FragmentEditarRepuestoBinding
import com.yalantis.ucrop.UCrop
import com.lingomak.lingomakapp.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditarRepuestoFragment : Fragment() {
    private var _binding: FragmentEditarRepuestoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventarioViewModel by activityViewModels()
    private var uidRepuesto: String = ""
    private var repuestoOriginal: RepuestoModel? = null

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
            result.data?.data?.let { uri -> iniciarRecorte(uri) }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = File(currentPhotoPath ?: "")
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
                iniciarRecorte(uri)
            }
        }
    }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri -> procesarImagenFinal(uri) }
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
        _binding = FragmentEditarRepuestoBinding.inflate(inflater, container, false)
        
        binding.etCodigoInterno.isEnabled = false // No permitir edición manual del código identificador

        uidRepuesto = arguments?.getString("uid") ?: ""
        configurarObservadores()
        configurarEventos()
        if (uidRepuesto.isNotEmpty()) {
            viewModel.obtenerRepuestoPorUid(uidRepuesto)
        }
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
        }

        val uCrop = UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800)
            .withOptions(options)

        cropLauncher.launch(uCrop.getIntent(requireContext()))
    }

    private fun procesarImagenFinal(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            binding.ivFotoRepuesto.setImageBitmap(bitmap)
            
            // Guardar imagen en almacenamiento interno persistente
            val fileName = "REP_EDIT_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            imagenLocalPath = file.absolutePath
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al procesar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarSpinnerCategorias(categorias: List<String>) {
        listaCategorias = categorias
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listaCategorias)
        binding.spinnerCategoria.setAdapter(adapter)
        
        // Si ya tenemos los datos del repuesto cargados, re-seleccionar la categoría
        repuestoOriginal?.let { repuesto ->
            if (listaCategorias.contains(repuesto.categoria)) {
                binding.spinnerCategoria.setText(repuesto.categoria, false)
            }
        }
    }

    private fun configurarObservadores() {
        viewModel.categorias.observe(viewLifecycleOwner) { lista ->
            configurarSpinnerCategorias(lista.map { it.nombre })
        }
        viewModel.repuestoSeleccionado.observe(viewLifecycleOwner) { repuesto ->
            if (repuesto != null) {
                repuestoOriginal = repuesto
                pintarDatos(repuesto)
            }
        }

        viewModel.actualizacionExitosa.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(requireContext(), "Repuesto actualizado", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                viewModel.limpiarEstados()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun pintarDatos(repuesto: RepuestoModel) {
        binding.etCodigoInterno.setText(repuesto.codigoInterno)
        binding.etNombre.setText(repuesto.nombre)
        binding.etMarca.setText(repuesto.marca)
        binding.etDescripcion.setText(repuesto.descripcion)
        binding.etStockActual.setText(repuesto.stockActual.toString())
        binding.etStockMinimo.setText(repuesto.stockMinimo.toString())
        binding.etStockMaximo.setText(repuesto.stockMaximo.toString())
        binding.etUbicacionAlmacen.setText(repuesto.ubicacionAlmacen)
        binding.etProveedorNombre.setText(repuesto.proveedorNombre)
        binding.etProveedorContacto.setText(repuesto.proveedorContacto)
        
        if (listaCategorias.contains(repuesto.categoria)) {
            binding.spinnerCategoria.setText(repuesto.categoria, false)
        }

        if (repuesto.imagenUrl.isNotEmpty()) {
            Glide.with(this).load(repuesto.imagenUrl).placeholder(R.drawable.bg_image_placeholder).into(binding.ivFotoRepuesto)
        }

        if (repuesto.codigoQR.isNotEmpty()) {
            Glide.with(this).load(repuesto.codigoQR).placeholder(R.drawable.bg_image_placeholder).into(binding.ivCodigoQR)
        }
    }

    private fun configurarEventos() {
        binding.fabGaleria.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }
        binding.fabCamara.setOnClickListener { verificarPermisosYCamara() }

        binding.ivCodigoQR.setOnClickListener {
            val codigo = binding.etCodigoInterno.text.toString().trim()
            if (codigo.isNotEmpty()) {
                generarYGuardarQR(codigo)
                Toast.makeText(requireContext(), "Nuevo QR generado localmente", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnActualizarRepuesto.setOnClickListener {
            val stockActual = binding.etStockActual.text.toString().toIntOrNull() ?: 0
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
                Toast.makeText(requireContext(), "El stock actual no puede ser mayor al máximo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val repuestoEditado = repuestoOriginal?.copy(
                codigoInterno = binding.etCodigoInterno.text.toString(),
                nombre = binding.etNombre.text.toString(),
                categoria = binding.spinnerCategoria.text.toString(),
                marca = binding.etMarca.text.toString(),
                descripcion = binding.etDescripcion.text.toString(),
                stockActual = stockActual,
                stockMinimo = stockMinimo,
                stockMaximo = stockMaximo,
                ubicacionAlmacen = binding.etUbicacionAlmacen.text.toString().trim(),
                proveedorNombre = binding.etProveedorNombre.text.toString().trim(),
                proveedorContacto = binding.etProveedorContacto.text.toString().trim()
            )
            repuestoEditado?.let { viewModel.actualizarRepuesto(it, imagenLocalPath, qrLocalPath) }
        }
    }

    private fun generarYGuardarQR(texto: String) {
        try {
            val bitmap = com.lingomak.lingomakapp.utils.QRHelper.generarBitmapQR(texto)
            bitmap?.let {
                binding.ivCodigoQR.setImageBitmap(it)
                val fileName = "QR_EDIT_${System.currentTimeMillis()}.png"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
