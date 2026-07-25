package com.lingomak.lingomakapp.ui.movimientos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import androidx.activity.result.contract.ActivityResultContracts
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentEstadisticasMovimientosBinding
import com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity
import com.lingomak.lingomakapp.utils.CsvExporter

class MovimientosEstadisticasFragment : Fragment() {

    private var _binding: FragmentEstadisticasMovimientosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosEstadisticasViewModel by viewModels()
    private lateinit var pagerAdapter: EstadisticasPagerAdapter

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val data = viewModel.estadisticas.value
            val label = viewModel.etiquetaRango.value ?: "Export"
            if (data != null) {
                val csvContent = CsvExporter.buildEstadisticasCsvContent(data, label)
                CsvExporter.saveCsvToUri(requireContext(), it, csvContent)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEstadisticasMovimientosBinding.inflate(inflater, container, false)
        
        setupUI()
        observarViewModel()
        
        validarAccesoAdmin()

        return binding.root
    }

    private fun validarAccesoAdmin() {
        val isAdmin = requireActivity() is DashboardAdminActivity
        binding.btnExportarCsv.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun setupUI() {
        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnExportarCsv.setOnClickListener {
            val data = viewModel.estadisticas.value
            val label = viewModel.etiquetaRango.value ?: "Export"
            val rango = viewModel.rango.value
            if (data != null) {
                CsvExporter.exportEstadisticas(
                    context = requireContext(),
                    data = data,
                    etiquetaRango = label,
                    launcher = createDocumentLauncher,
                    inicio = rango?.first,
                    fin = rango?.second
                )
            }
        }

        binding.selectorFechasEstadisticas.onRangoSeleccionado = { inicio, fin, etiqueta ->
            viewModel.setRango(inicio, fin, etiqueta)
        }

        pagerAdapter = EstadisticasPagerAdapter()
        binding.viewPagerEstadisticas.adapter = pagerAdapter

        // Indicador de puntos
        TabLayoutMediator(binding.tabLayoutIndicator, binding.viewPagerEstadisticas) { _, _ ->
            // Sin texto
        }.attach()
    }

    private fun observarViewModel() {
        viewModel.etiquetaRango.observe(viewLifecycleOwner) { etiqueta ->
            binding.tvRangoActual.text = etiqueta
        }

        viewModel.estadisticas.observe(viewLifecycleOwner) { data ->
            pagerAdapter.updateData(data)

            if (data.recomendacion != null) {
                binding.layoutRecomendacion.visibility = View.VISIBLE
                binding.tvRecomendacionStock.text = "Al ritmo de consumo de los últimos 30 días, este repuesto se agotaría pronto. Revisa su stock: ${data.recomendacion}."
            } else {
                binding.layoutRecomendacion.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
