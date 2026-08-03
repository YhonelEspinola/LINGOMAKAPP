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

        // Categorías
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categorias)
        binding.spFiltroCategoria.setAdapter(spinnerAdapter)
        binding.spFiltroCategoria.setText(categorias[0], false)
        binding.spFiltroCategoria.setOnItemClickListener { _, _, position, _ ->
            val cat = categorias[position]
            viewModel.filtrarPorCategoria(if (cat == "Todas las categorías") null else cat)
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