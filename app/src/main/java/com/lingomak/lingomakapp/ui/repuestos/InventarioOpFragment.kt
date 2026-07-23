package com.lingomak.lingomakapp.ui.repuestos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentInventarioBinding
import androidx.core.widget.addTextChangedListener

class InventarioOpFragment : Fragment() {

    private var _binding: FragmentInventarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventarioOpViewModel by viewModels()
    private lateinit var adapter: RepuestosOpAdapter

    private val categorias = listOf(
        "Todas las categorías",
        "Aceites",
        "Filtros",
        "Frenos",
        "Eléctrico",
        "Motor"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventarioBinding.inflate(inflater, container, false)
        
        setupUI()
        observarViewModel()
        
        // Sincronizar al entrar
        viewModel.listarRepuestos()
        
        return binding.root
    }

    private fun setupUI() {
        // Ocultar el FAB ya que el operador no agrega repuestos
        binding.fabAgregarRepuesto.visibility = View.GONE
        binding.btnExportarCsv.visibility = View.GONE

        adapter = RepuestosOpAdapter(emptyList()) { repuesto ->
            // El operario solo consulta el detalle (puedes reutilizar el de admin o crear DetalleRepuestoOpFragment)
            val fragment = DetalleRepuestoFragment()
            val bundle = Bundle().apply { putString("uid", repuesto.uid) }
            fragment.arguments = bundle
            
            parentFragmentManager.beginTransaction()
                .replace(R.id.containerOperario, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.rvRepuestos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRepuestos.adapter = adapter

        // Paginación Operario
        binding.rvRepuestos.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if ((layoutManager.childCount + layoutManager.findFirstVisibleItemPosition()) >= layoutManager.itemCount) {
                    viewModel.cargarSiguienteLote()
                }
            }
        })

        // Buscador
        binding.etBuscar.addTextChangedListener { 
            viewModel.buscarRepuesto(it?.toString() ?: "")
        }

        // Categorías
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categorias)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategoria.adapter = spinnerAdapter
        binding.spinnerCategoria.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val cat = categorias[position]
                viewModel.filtrarPorCategoria(if (cat == "Todas las categorías") null else cat)
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        // Filtros de Criticidad
        binding.chipTodosCriticidad.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorCriticidad(InventarioOpViewModel.NivelCriticidad.TODOS)
        }
        binding.chipEnStock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorCriticidad(InventarioOpViewModel.NivelCriticidad.EN_STOCK)
        }
        binding.chipBajoStock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorCriticidad(InventarioOpViewModel.NivelCriticidad.BAJO_STOCK)
        }
        binding.chipSinStock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorCriticidad(InventarioOpViewModel.NivelCriticidad.SIN_STOCK)
        }

        // Filtros de Estado
        binding.chipTodosEstado.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorEstado(null)
        }
        binding.chipActivos.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorEstado("ACTIVO")
        }
        binding.chipInactivos.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorEstado("INACTIVO")
        }
    }

    private fun observarViewModel() {
        viewModel.repuestos.observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}