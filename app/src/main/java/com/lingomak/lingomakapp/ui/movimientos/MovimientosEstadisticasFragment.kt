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
                val csvContent = buildEstadisticasCsvContent(data, label)
                CsvExporter.saveCsvToUri(requireContext(), it, csvContent)
            }
        }
    }

    private fun buildEstadisticasCsvContent(data: MovimientosEstadisticasViewModel.EstadisticasData, etiquetaRango: String): String {
        val sb = StringBuilder()
        
        // 1. Resumen General
        sb.append("1. RESUMEN GENERAL\n")
        sb.append("Rango de fechas,Total Entradas,Total Salidas,Total Movimientos\n")
        sb.append("${CsvExporter.escapeCsv(etiquetaRango)},${data.totalEntradas},${data.totalSalidas},${data.totalMovimientos}\n\n")

        // 2. Tendencia Mensual
        sb.append("2. TENDENCIA MENSUAL\n")
        sb.append("Mes,Entradas,Salidas\n")
        data.tendenciaMensual.forEach { (mes, vals) ->
            sb.append("${CsvExporter.escapeCsv(mes)},${vals.first},${vals.second}\n")
        }
        sb.append("\n")

        // 3. Top Repuestos Consumidos
        sb.append("3. TOP REPUESTOS CONSUMIDOS\n")
        sb.append("Nombre,Cantidad de Salidas\n")
        data.topProductos.forEach { (nombre, cant) ->
            sb.append("${CsvExporter.escapeCsv(nombre)},$cant\n")
        }
        sb.append("\n")

        // 4. Baja/Nula Rotación
        sb.append("4. BAJA/NULA ROTACIÓN\n")
        sb.append("Nombre,Cantidad de Salidas\n")
        data.bajaRotacion.forEach { (nombre, cant) ->
            sb.append("${CsvExporter.escapeCsv(nombre)},$cant\n")
        }
        sb.append("\n")

        // 5. Consumo por Máquina
        sb.append("5. CONSUMO POR MÁQUINA\n")
        sb.append("Nombre,Cantidad de Repuestos Consumidos\n")
        data.consumoMaquina.forEach { (nombre, cant) ->
            sb.append("${CsvExporter.escapeCsv(nombre)},$cant\n")
        }
        sb.append("\n")

        // 6. Consumo Interno vs Externo
        sb.append("6. CONSUMO INTERNO VS EXTERNO\n")
        sb.append("Categoría,Cantidad,Porcentaje\n")
        val totalSalidas = data.totalSalidas.toDouble()
        data.distribucionSalida.forEach { (cat, cant) ->
            val perc = if (totalSalidas > 0) (cant / totalSalidas) * 100 else 0.0
            val catLabel = if(cat == "CONSUMO_INTERNO") "Interno" else "Externo"
            sb.append("${CsvExporter.escapeCsv(catLabel)},$cant,${String.format(java.util.Locale.getDefault(), "%.1f%%", perc)}\n")
        }
        sb.append("\n")

        // 7. Salidas con OM vs Sueltas (Omitida en carrusel pero mantenida en CSV si se desea)
        sb.append("7. SALIDAS CON OM VS SUELTAS\n")
        sb.append("Categoría,Cantidad,Porcentaje\n")
        val om = data.omVsSueltas.first
        val sueltas = data.omVsSueltas.second
        val totalOM = (om + sueltas).toDouble()
        val percOM = if (totalOM > 0) (om / totalOM) * 100 else 0.0
        val percSueltas = if (totalOM > 0) (sueltas / totalOM) * 100 else 0.0
        sb.append("Con OM,$om,${String.format(java.util.Locale.getDefault(), "%.1f%%", percOM)}\n")
        sb.append("Sueltas,$sueltas,${String.format(java.util.Locale.getDefault(), "%.1f%%", percSueltas)}\n\n")

        // 8. Costo Real vs Estimado
        sb.append("8. COSTO REAL VS ESTIMADO\n")
        sb.append("Categoría,Monto (Soles),Diferencia\n")
        sb.append("Estimado,${data.costosComparativa.first},-\n")
        sb.append("Real,${data.costosComparativa.second},${data.costosComparativa.third}\n")

        return sb.toString()
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
            if (data != null) {
                CsvExporter.exportEstadisticas(requireContext(), data, label, createDocumentLauncher)
            }
        }

        binding.selectorFechasEstadisticas.onRangoSeleccionado = { inicio, fin, etiqueta ->
            viewModel.setRango(inicio, fin, etiqueta)
        }

        pagerAdapter = EstadisticasPagerAdapter { meses ->
            viewModel.setMesesTendencia(meses)
        }
        binding.viewPagerEstadisticas.adapter = pagerAdapter

        // Indicador de puntos
        TabLayoutMediator(binding.tabLayoutIndicator, binding.viewPagerEstadisticas) { _, _ ->
            // Sin texto
        }.attach()

        // Reaccionar al cambio de página
        binding.viewPagerEstadisticas.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Página 2 (índice 1) es Tendencia
                if (position == 1) {
                    binding.selectorFechasEstadisticas.visibility = View.GONE
                    binding.tvLabelFijoPeriodo.visibility = View.VISIBLE
                    binding.tvRangoActual.visibility = View.INVISIBLE
                } else {
                    binding.selectorFechasEstadisticas.visibility = View.VISIBLE
                    binding.tvLabelFijoPeriodo.visibility = View.GONE
                    binding.tvRangoActual.visibility = View.VISIBLE
                }
            }
        })
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
