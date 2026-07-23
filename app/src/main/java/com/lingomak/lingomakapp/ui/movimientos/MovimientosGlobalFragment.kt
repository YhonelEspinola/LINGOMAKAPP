package com.lingomak.lingomakapp.ui.movimientos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentMovimientosGlobalBinding
import com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity
import com.lingomak.lingomakapp.utils.CsvExporter
import java.util.*

class MovimientosGlobalFragment : Fragment() {

    private var _binding: FragmentMovimientosGlobalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosGlobalViewModel by viewModels()
    private lateinit var adapter: MovimientosGlobalAdapter

    private var filtroInicialTexto: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovimientosGlobalBinding.inflate(inflater, container, false)

        filtroInicialTexto = arguments?.getString("filtroTexto") ?: ""

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

        // Implementar Scroll Infinito (Paginación para Admin)
        binding.rvMovimientos.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    viewModel.cargarSiguienteLote()
                }
            }
        })

        binding.selectorFechas.onRangoSeleccionado = { inicio, fin, etiqueta ->
            viewModel.setRangoFechas(inicio, fin)
            // Actualizar el texto del filtro actual arriba del buscador
            binding.root.findViewById<TextView>(R.id.tvFiltroActual)?.text = etiqueta
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

        binding.btnExportarCsv.setOnClickListener {
            val data = viewModel.movimientosFiltrados.value ?: emptyList()
            CsvExporter.exportMovimientos(requireContext(), data)
        }
        
        validarAccesoAdmin()

        /*binding.btnEstadisticas.setOnClickListener {
            val fragment = MovimientosEstadisticasFragment()
            val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
                R.id.fragmentContainerAdmin else R.id.containerOperario
                
            parentFragmentManager.beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }*/
        filtroInicialTexto = arguments?.getString("filtroTexto") ?: ""
    }


    private fun validarAccesoAdmin() {
        val isAdmin = requireActivity() is DashboardAdminActivity
        binding.btnExportarCsv.visibility = if (isAdmin) View.VISIBLE else View.GONE
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
