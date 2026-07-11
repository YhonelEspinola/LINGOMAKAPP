package com.lingomak.lingomakapp.ui.movimientos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.databinding.FragmentEstadisticasMovimientosBinding

import android.widget.TableRow
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.lingomak.lingomakapp.R

class MovimientosEstadisticasFragment : Fragment() {

    private var _binding: FragmentEstadisticasMovimientosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosEstadisticasViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEstadisticasMovimientosBinding.inflate(inflater, container, false)
        
        setupUI()
        observarViewModel()
        
        return binding.root
    }

    private fun setupUI() {
        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.selectorFechasEstadisticas.onRangoSeleccionado = { inicio, fin, etiqueta ->
            viewModel.setRango(inicio, fin, etiqueta)
        }
        
        setupChart()
    }

    private fun setupChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            
            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(listOf("Entradas", "Salidas"))
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun observarViewModel() {
        viewModel.etiquetaRango.observe(viewLifecycleOwner) { etiqueta ->
            binding.tvRangoActual.text = etiqueta
        }

        viewModel.estadisticas.observe(viewLifecycleOwner) { data ->
            binding.tvTotalEntradasGlobal.text = data.totalEntradas.toString()
            binding.tvTotalSalidasGlobal.text = data.totalSalidas.toString()
            binding.tvMovimientosTotalesGlobal.text = data.totalMovimientos.toString()
            
            binding.tvLabelResumen.text = data.resumenTexto
            binding.tvProductoMasUsado.text = "Producto más usado: ${data.productoMasUsado}"

            if (data.recomendacion != null) {
                binding.tvRecomendacionStock.visibility = View.VISIBLE
                binding.tvRecomendacionStock.text = "Basado en el ritmo actual, se recomienda revisar stock de: ${data.recomendacion}."
            } else {
                binding.tvRecomendacionStock.visibility = View.GONE
            }

            actualizarGrafica(data.totalEntradas, data.totalSalidas)
            actualizarTablaTop(data.topProductos)
        }
    }

    private fun actualizarGrafica(entradas: Int, salidas: Int) {
        val entries = mutableListOf<BarEntry>()
        entries.add(BarEntry(0f, entradas.toFloat()))
        entries.add(BarEntry(1f, salidas.toFloat()))

        val dataSet = BarDataSet(entries, "Movimientos")
        dataSet.colors = listOf(
            requireContext().getColor(R.color.success),
            requireContext().getColor(R.color.danger)
        )
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = requireContext().getColor(R.color.text_primary)

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f

        binding.barChart.data = barData
        binding.barChart.animateY(800)
        binding.barChart.invalidate()
    }

    private fun actualizarTablaTop(top: List<Pair<String, Int>>) {
        // Mantener el encabezado (índice 0)
        val count = binding.tableTopProductos.childCount
        if (count > 1) {
            binding.tableTopProductos.removeViews(1, count - 1)
        }

        top.forEach { (nombre, movs) ->
            val row = TableRow(requireContext()).apply {
                setPadding(0, 12, 0, 12)
            }

            val tvNombre = TextView(requireContext()).apply {
                text = nombre
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(8.toPx(), 0, 8.toPx(), 0)
                setTextColor(requireContext().getColor(R.color.text_primary))
                textSize = 14f
            }

            val tvMovs = TextView(requireContext()).apply {
                text = movs.toString()
                setPadding(8.toPx(), 0, 8.toPx(), 0)
                setTextColor(requireContext().getColor(R.color.text_primary))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.END
            }

            row.addView(tvNombre)
            row.addView(tvMovs)
            binding.tableTopProductos.addView(row)
        }
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
