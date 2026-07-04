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

import android.widget.AdapterView
import android.widget.ArrayAdapter
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

        // Configurar Spinner de Periodo
        val opciones = listOf("Hoy", "Esta Semana", "Este Mes", "Este Año")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opciones)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriodo.adapter = spinnerAdapter
        binding.spinnerPeriodo.setSelection(1) // Semana por defecto

        binding.spinnerPeriodo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val p = when(position) {
                    0 -> "DIA"
                    1 -> "SEMANA"
                    2 -> "MES"
                    3 -> "ANIO"
                    else -> "SEMANA"
                }
                viewModel.setPeriodo(p)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observarViewModel() {
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
            actualizarListaTop(data.topProductos)
        }
    }

    private fun actualizarGrafica(entradas: Int, salidas: Int) {
        val max = maxOf(entradas, salidas, 1).toFloat()
        
        // Ajustar altura de las barras proporcionalmente (max 150dp aprox)
        val factor = 150f / max
        
        binding.barEntrada.layoutParams.height = (entradas * factor).toInt().toPx()
        binding.barSalida.layoutParams.height = (salidas * factor).toInt().toPx()
        
        binding.barEntrada.requestLayout()
        binding.barSalida.requestLayout()
    }

    private fun actualizarListaTop(top: List<Pair<String, Int>>) {
        binding.containerTopProductos.removeAllViews()
        top.forEachIndexed { index, pair ->
            val textView = TextView(requireContext()).apply {
                text = "${index + 1}. ${pair.first} (${pair.second} mov.)"
                setPadding(0, 8, 0, 8)
                textSize = 14f
                setTextColor(requireContext().getColor(com.lingomak.lingomakapp.R.color.text_primary))
            }
            binding.containerTopProductos.addView(textView)
        }
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
