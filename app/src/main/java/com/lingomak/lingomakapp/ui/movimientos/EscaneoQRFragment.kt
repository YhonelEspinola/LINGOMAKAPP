package com.lingomak.lingomakapp.ui.movimientos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentEscaneoQrBinding
import com.lingomak.lingomakapp.ui.dashboard.DashboardOperarioActivity
import com.lingomak.lingomakapp.ui.repuestos.InventarioOpFragment
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EscaneoQRFragment : Fragment() {

    private var _binding: FragmentEscaneoQrBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var isScanning = true

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Permiso de cámara requerido para escanear", Toast.LENGTH_SHORT).show()
            abrirRegistroManual()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEscaneoQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnRegistroManual.setOnClickListener { abrirRegistroManual() }
        binding.btnClose.setOnClickListener { 
            val isOperario = requireActivity() is DashboardOperarioActivity
            if (isOperario) {
                if (parentFragmentManager.backStackEntryCount > 0) {
                    parentFragmentManager.popBackStack()
                } else {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.containerOperario, InventarioOpFragment())
                        .commit()
                }
            } else {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { qrValue ->
                        if (isScanning && qrValue != null) {
                            isScanning = false
                            processQR(qrValue)
                        }
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Toast.makeText(requireContext(), "Error al iniciar cámara", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processQR(uid: String) {
        activity?.runOnUiThread {
            val isOperario = requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardOperarioActivity
            
            val fragment = if (isOperario) RegistrarSalidaOpFragment() else RegistrarMovimientoGlobalFragment()
            val bundle = Bundle().apply {
                putString("repuestoUidScanned", uid)
            }
            fragment.arguments = bundle
            
            val containerId = if (isOperario) R.id.containerOperario else R.id.fragmentContainerAdmin

            parentFragmentManager.beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun abrirRegistroManual() {
        val isOperario = requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardOperarioActivity
        val fragment = if (isOperario) RegistrarSalidaOpFragment() else RegistrarMovimientoGlobalFragment()
        
        val containerId = if (isOperario) R.id.containerOperario else R.id.fragmentContainerAdmin

        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    private class BarcodeAnalyzer(private val onQrDetected: (String?) -> Unit) : ImageAnalysis.Analyzer {
        private val scanner: BarcodeScanner = BarcodeScanning.getClient()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            if (barcode.valueType == Barcode.TYPE_TEXT || barcode.valueType == Barcode.TYPE_PRODUCT) {
                                onQrDetected(barcode.rawValue)
                                break
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }
    }
}
