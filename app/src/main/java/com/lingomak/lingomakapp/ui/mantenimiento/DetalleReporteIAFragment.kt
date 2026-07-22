package com.lingomak.lingomakapp.ui.mantenimiento

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.model.ReporteIAData
import com.lingomak.lingomakapp.databinding.FragmentDetalleReporteIaBinding
import com.lingomak.lingomakapp.utils.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetalleReporteIAFragment : Fragment() {

    private var _binding: FragmentDetalleReporteIaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MantenimientoViewModel by viewModels()
    private var uidMantenimiento = ""
    private var mantenimientoActual: MantenimientoModel? = null
    private var reporteActual: ReporteIAData? = null

    // Launcher para guardar archivo PDF
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { savePdfToUri(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleReporteIaBinding.inflate(inflater, container, false)
        uidMantenimiento = arguments?.getString("uid") ?: ""

        configurarToolbar()
        observarViewModel()
        cargarDatos()

        return binding.root
    }

    private fun configurarToolbar() {
        binding.toolbarReporteIA.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun cargarDatos() {
        viewModel.obtenerMantenimientoPorUid(uidMantenimiento) { m ->
            mantenimientoActual = m
            if (m.reporteIA.isNotEmpty()) {
                val data = ReporteIAData.desdeJson(m.reporteIA)
                if (data != null) {
                    reporteActual = data
                    mostrarReporteUI(data)
                    binding.btnGuardarReporteTelefono.visibility = View.VISIBLE
                } else {
                    // Soporte para Markdown antiguo
                    reporteActual = ReporteIAData(sintoma = m.reporteIA)
                    binding.cardSintoma.visibility = View.VISIBLE
                    binding.tvSintoma.text = m.reporteIA
                    binding.btnGuardarReporteTelefono.visibility = View.VISIBLE
                }
            } else {
                viewModel.generarReporteIA(m)
            }
        }
    }

    private fun observarViewModel() {
        viewModel.reporteGenerado.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                reporteActual = data
                mostrarReporteUI(data)
                
                // Guardar automáticamente en DB para que persista
                viewModel.guardarReporte(uidMantenimiento, data.aJson())
                
                binding.btnGuardarReporteTelefono.visibility = View.VISIBLE
            }
        }

        binding.btnGuardarReporteTelefono.setOnClickListener {
            lanzarSelectorDeArchivo()
        }

        viewModel.loadingAI.observe(viewLifecycleOwner) { estaCargando ->
            binding.progressCargandoReporte.visibility = if (estaCargando) View.VISIBLE else View.GONE
            binding.tvEstadoCarga.visibility = if (estaCargando) View.VISIBLE else View.GONE
        }

        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            if (!mensaje.isNullOrEmpty()) {
                binding.tvEstadoCarga.visibility = View.VISIBLE
                binding.tvEstadoCarga.text = "ERROR: $mensaje"
                binding.tvEstadoCarga.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarReporteUI(data: ReporteIAData) {
        binding.tvEstadoCarga.visibility = View.GONE
        
        binding.cardSintoma.visibility = View.VISIBLE
        binding.tvSintoma.text = data.sintoma
        
        binding.cardCausa.visibility = View.VISIBLE
        binding.tvCausa.text = data.causa
        
        binding.cardAcciones.visibility = View.VISIBLE
        binding.tvAcciones.text = data.acciones
        
        binding.cardResultado.visibility = View.VISIBLE
        binding.tvResultado.text = data.resultado
    }

    private fun lanzarSelectorDeArchivo() {
        val cod = mantenimientoActual?.codigoMantenimiento ?: "DOC"
        val fileName = "Reporte $cod.pdf"
        createDocumentLauncher.launch(fileName)
    }

    private fun savePdfToUri(uri: Uri) {
        val m = mantenimientoActual ?: return
        val r = reporteActual ?: return

        binding.progressCargandoReporte.visibility = View.VISIBLE
        Toast.makeText(requireContext(), "Generando PDF...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Cargar fotos de Reporte (Inicial)
                val bitmapsReporte = mutableListOf<Bitmap>()
                val sourcesReporte = m.imagenesReporte + m.imagenesReporteLocal
                for (source in sourcesReporte.filter { it.isNotEmpty() }) {
                    cargarBitmap(source)?.let { bitmapsReporte.add(it) }
                }

                // 2. Cargar fotos de Finalización
                val bitmapsFinal = mutableListOf<Bitmap>()
                val sourcesFinal = m.imagenesFinalizacion + m.imagenesFinalizacionLocal
                for (source in sourcesFinal.filter { it.isNotEmpty() }) {
                    cargarBitmap(source)?.let { bitmapsFinal.add(it) }
                }

                // 3. Generar PDF con listas separadas
                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    PdfGenerator(requireContext()).generateMaintenanceReport(
                        outputStream, m, r, bitmapsReporte, bitmapsFinal
                    )
                }

                withContext(Dispatchers.Main) {
                    binding.progressCargandoReporte.visibility = View.GONE
                    Toast.makeText(requireContext(), "Reporte guardado exitosamente", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressCargandoReporte.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error al guardar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun cargarBitmap(source: String): Bitmap? {
        return try {
            Glide.with(requireContext())
                .asBitmap()
                .load(source)
                .centerCrop()
                .submit(400, 400)
                .get()
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.limpiarEstadoAI()
        _binding = null
    }
}
