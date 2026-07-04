package com.lingomak.lingomakapp.ui.movimientos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentMovimientosGlobalBinding
import java.util.*

class MovimientosGlobalFragment : Fragment() {

    private var _binding: FragmentMovimientosGlobalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosGlobalViewModel by viewModels()
    private lateinit var adapter: MovimientosGlobalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovimientosGlobalBinding.inflate(inflater, container, false)
        
        setupUI()
        observarViewModel()
        
        return binding.root
    }

    private fun setupUI() {
        adapter = MovimientosGlobalAdapter(emptyList()) { pair ->
            val fragment = DetalleMovimientoFragment()
            val bundle = Bundle().apply {
                putString("movimientoUid", pair.first.uid)
            }
            fragment.arguments = bundle
            
            val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
                R.id.fragmentContainerAdmin else R.id.containerOperario
                
            parentFragmentManager.beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvMovimientos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMovimientos.adapter = adapter

        binding.selectorFechas.onRangoSeleccionado = { inicio, fin, etiqueta ->
            viewModel.setRangoFechas(inicio, fin)
        }
        binding.selectorFechas.dispararSeleccionActual()

        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filtrarPorTexto(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupTipo.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipTodos -> viewModel.filtrarPorTipo(null)
                R.id.chipEntradas -> viewModel.filtrarPorTipo("ENTRADA")
                R.id.chipSalidas -> viewModel.filtrarPorTipo("SALIDA")
            }
        }

        binding.btnRegistrar.setOnClickListener {
            val fragment = EscaneoQRFragment()
            val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
                R.id.fragmentContainerAdmin else R.id.containerOperario
                
            parentFragmentManager.beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnEstadisticas.setOnClickListener {
            val fragment = MovimientosEstadisticasFragment()
            val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
                R.id.fragmentContainerAdmin else R.id.containerOperario
                
            parentFragmentManager.beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }
    }


    private fun observarViewModel() {
        viewModel.movimientosFiltrados.observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
