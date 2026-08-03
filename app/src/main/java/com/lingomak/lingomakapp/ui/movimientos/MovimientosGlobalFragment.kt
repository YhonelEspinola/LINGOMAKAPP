package com.lingomak.lingomakapp.ui.movimientos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.contract.ActivityResultContracts
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.repository.UserRepository
import com.lingomak.lingomakapp.databinding.FragmentMovimientosGlobalBinding
import com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity
import com.lingomak.lingomakapp.utils.CsvExporter
import java.util.*

class MovimientosGlobalFragment : Fragment() {

    private var _binding: FragmentMovimientosGlobalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosGlobalViewModel by viewModels()
    private lateinit var adapter: MovimientosGlobalAdapter
    private var fechaInicio: Long = 0L
    private var fechaFin: Long = Long.MAX_VALUE

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val data = viewModel.movimientosFiltrados.value ?: emptyList()
            val csvContent = CsvExporter.buildMovimientosCsvContent(data)
            CsvExporter.saveCsvToUri(requireContext(), it, csvContent)
        }
    }

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
        // Toggle Filtros Colapsable
        binding.btnToggleFiltros.setOnClickListener {
            val currentlyVisible = binding.layoutFiltrosExpandible.visibility == View.VISIBLE
            val nextVisibility = if (currentlyVisible) View.GONE else View.VISIBLE
            binding.layoutFiltrosExpandible.visibility = nextVisibility
            
            // Animación del chevron
            binding.ivChevronFiltros.animate()
                .rotation(if (currentlyVisible) 0f else 180f)
                .setDuration(200)
                .start()
        }

        adapter = MovimientosGlobalAdapter(emptyList()) { pair ->
            val fragment = DetalleMovimientoFragment()
            val bundle = Bundle().apply {
                putString("movimientoUid", pair.first.uid)
            }
            fragment.arguments = bundle
            
            val containerId = if (requireActivity() is DashboardAdminActivity)
                R.id.fragmentContainerAdmin else R.id.containerOperario
                
            parentFragmentManager.beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvMovimientos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMovimientos.adapter = adapter

        setupDropdowns()

        // Implementar Scroll Infinito
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
            fechaInicio = inicio
            fechaFin = fin
            viewModel.setRangoFechas(inicio, fin)
            // Actualizar el texto del filtro actual arriba del buscador
            binding.tvFiltroActual.text = etiqueta
        }
        binding.selectorFechas.dispararSeleccionActual()

        binding.chipGroupTipo.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipTodos -> viewModel.filtrarPorTipo(null)
                R.id.chipEntradas -> viewModel.filtrarPorTipo("ENTRADA")
                R.id.chipSalidas -> viewModel.filtrarPorTipo("SALIDA")
            }
        }

        binding.btnExportarCsv.setOnClickListener {
            val label = binding.tvFiltroActual.text.toString()
            val fileName = "movimientos_${label.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
            createDocumentLauncher.launch(fileName)
        }

        validarAccesoAdmin()
    }


    private fun setupDropdowns() {
        // User Dropdown (Fix 6.1)
        val userRepo = UserRepository(requireContext())
        userRepo.obtenerUsuariosObservable().observe(viewLifecycleOwner) { usuarios ->
            val nombres = listOf("TODOS") + usuarios.map { it.nombre }.sorted()
            val adapterUser = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
            binding.spFiltroUsuario.setAdapter(adapterUser)
            binding.spFiltroUsuario.setText("TODOS", false)
        }
        binding.spFiltroUsuario.setOnItemClickListener { parent, _, position, _ ->
            viewModel.filtrarPorUsuario(parent.getItemAtPosition(position) as String)
        }

        // Repuesto Dropdown (Reemplaza HistorialRepuestoFragment - Punto 2)
        viewModel.todosLosRepuestos.observe(viewLifecycleOwner) { repuestos ->
            val nombres = listOf("TODOS") + repuestos.map { it.nombre }.sorted()
            val adapterRep = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
            binding.spFiltroRepuesto.setAdapter(adapterRep)
            
            // Si venimos con un filtro inicial de repuesto (desde DetalleRepuestoFragment)
            val initialRep = arguments?.getString("nombreRepuesto")
            if (!initialRep.isNullOrEmpty()) {
                binding.spFiltroRepuesto.setText(initialRep, false)
                viewModel.filtrarPorRepuesto(initialRep)
            } else {
                binding.spFiltroRepuesto.setText("TODOS", false)
            }
        }
        binding.spFiltroRepuesto.setOnItemClickListener { parent, _, position, _ ->
            viewModel.filtrarPorRepuesto(parent.getItemAtPosition(position) as String)
        }
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
