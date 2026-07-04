package com.lingomak.lingomakapp.ui.repuestos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentMovimientosBinding
import java.text.SimpleDateFormat
import java.util.*

class MovimientosFragment : Fragment() {

    private var _binding: FragmentMovimientosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosViewModel by viewModels()
    private lateinit var adapter: MovimientosAdapter

    private var repuestoUid: String = ""
    private var nombreRepuesto: String = ""
    private var stockActual: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovimientosBinding.inflate(inflater, container, false)
        
        repuestoUid = arguments?.getString("repuestoUid") ?: ""
        nombreRepuesto = arguments?.getString("nombreRepuesto") ?: ""
        stockActual = arguments?.getInt("stockActual") ?: 0

        viewModel.setRepuestoUid(repuestoUid)
        
        setupUI()
        observarViewModel()
        
        return binding.root
    }

    private fun setupUI() {
        binding.tvTituloRepuesto.text = "Historial - $nombreRepuesto"
        
        adapter = MovimientosAdapter(emptyList())
        binding.rvMovimientos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMovimientos.adapter = adapter

        binding.selectorFechas.onRangoSeleccionado = { inicio, fin, etiqueta ->
            viewModel.setRangoFechas(inicio, fin)
            binding.tvTituloRepuesto.text = etiqueta
        }
        binding.selectorFechas.dispararSeleccionActual()
    }


    private fun observarViewModel() {
        viewModel.historialFiltrado.observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista)
        }

        viewModel.resumen.observe(viewLifecycleOwner) { resumen ->
            binding.tvTotalEntradas.text = resumen.totalEntradas.toString()
            binding.tvTotalSalidas.text = resumen.totalSalidas.toString()
            binding.tvConsumoPromedio.text = resumen.balance.toString()
            
            val colorBalance = if (resumen.balance >= 0) R.color.success else R.color.danger
            binding.tvConsumoPromedio.setTextColor(ContextCompat.getColor(requireContext(), colorBalance))

            binding.tvLabelEntradas.text = "Entradas${resumen.periodoLabel}"
            binding.tvLabelSalidas.text = "Salidas${resumen.periodoLabel}"
            binding.tvLabelPromedio.text = "Balance${resumen.periodoLabel}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
