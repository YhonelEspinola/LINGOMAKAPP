package com.lingomak.lingomakapp.ui.configuracion

import com.lingomak.lingomakapp.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.databinding.FragmentConfiguracionBinding

class ConfiguracionFragment : Fragment() {

    private var _binding: FragmentConfiguracionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConfiguracionViewModel by viewModels()

    private lateinit var adapterRepuesto: CategoriaAdapter
    private lateinit var adapterMaquinaria: CategoriaAdapter
    private lateinit var adapterCombustible: CategoriaAdapter

    private var repuestosExpanded = false
    private var maquinariaExpanded = false
    private var combustibleExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfiguracionBinding.inflate(inflater, container, false)

        setupRecyclerViews()
        setupThemeSwitch()
        setupEventos()
        observarViewModel()
        
        validarRol()

        return binding.root
    }

    private fun setupThemeSwitch() {
        val themeManager = com.lingomak.lingomakapp.utils.ThemeManager
        val estaOscuro = themeManager.isDarkMode(requireContext())
        binding.switchDarkMode.isChecked = estaOscuro
        actualizarIconoTema(estaOscuro)

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            actualizarIconoTema(isChecked)
            themeManager.setDarkMode(requireContext(), isChecked)
        }
    }

    private fun actualizarIconoTema(modoOscuro: Boolean) {
        binding.ivIconoTema.setImageResource(
            if (modoOscuro) R.drawable.ic_moon else R.drawable.ic_sun
        )
    }

    private fun validarRol() {
        val isAdmin = requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity
        binding.layoutMaestrosAdmin.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerViews() {
        adapterRepuesto = CategoriaAdapter(emptyList()) { categoria ->
            mostrarDialogoEliminar(categoria)
        }
        binding.rvCategoriasRepuesto.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategoriasRepuesto.adapter = adapterRepuesto

        adapterMaquinaria = CategoriaAdapter(emptyList()) { categoria ->
            mostrarDialogoEliminar(categoria)
        }
        binding.rvCategoriasMaquinaria.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategoriasMaquinaria.adapter = adapterMaquinaria

        adapterCombustible = CategoriaAdapter(emptyList()) { categoria ->
            mostrarDialogoEliminar(categoria)
        }
        binding.rvCategoriasCombustible.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategoriasCombustible.adapter = adapterCombustible
        
        binding.rvCategoriasRepuesto.isNestedScrollingEnabled = false
        binding.rvCategoriasMaquinaria.isNestedScrollingEnabled = false
        binding.rvCategoriasCombustible.isNestedScrollingEnabled = false

        binding.ivChevronRepuestos.rotation = -180f
        binding.ivChevronMaquinaria.rotation = -180f
        binding.ivChevronCombustible.rotation = -180f
    }

    private fun setupEventos() {
        binding.btnAgregarCatRepuesto.setOnClickListener {
            mostrarDialogoAgregar("REPUESTO")
        }
        binding.btnAgregarCatMaquinaria.setOnClickListener {
            mostrarDialogoAgregar("MAQUINARIA")
        }
        binding.btnAgregarCatCombustible.setOnClickListener {
            mostrarDialogoAgregar("COMBUSTIBLE")
        }

        binding.btnToggleRepuestos.setOnClickListener {
            repuestosExpanded = !repuestosExpanded
            toggleSection(binding.layoutExpandibleRepuestos, binding.ivChevronRepuestos, repuestosExpanded)
        }

        binding.btnToggleMaquinaria.setOnClickListener {
            maquinariaExpanded = !maquinariaExpanded
            toggleSection(binding.layoutExpandibleMaquinaria, binding.ivChevronMaquinaria, maquinariaExpanded)
        }

        binding.btnToggleCombustible.setOnClickListener {
            combustibleExpanded = !combustibleExpanded
            toggleSection(binding.layoutExpandibleCombustible, binding.ivChevronCombustible, combustibleExpanded)
        }

        binding.etBuscarRepuestos.addTextChangedListener {
            viewModel.buscarRepuesto(it?.toString() ?: "")
        }

        binding.etBuscarMaquinaria.addTextChangedListener {
            viewModel.buscarMaquinaria(it?.toString() ?: "")
        }

        binding.etBuscarCombustible.addTextChangedListener {
            viewModel.buscarCombustible(it?.toString() ?: "")
        }
    }

    private fun toggleSection(layout: View, chevron: android.widget.ImageView, expanded: Boolean) {
        layout.visibility = if (expanded) View.VISIBLE else View.GONE
        chevron.animate().rotation(if (expanded) 0f else -180f).setDuration(200).start()
    }

    private fun observarViewModel() {
        viewModel.categoriasRepuesto.observe(viewLifecycleOwner) {
            adapterRepuesto.actualizarLista(it)
        }
        viewModel.categoriasMaquinaria.observe(viewLifecycleOwner) {
            adapterMaquinaria.actualizarLista(it)
        }
        viewModel.categoriasCombustible.observe(viewLifecycleOwner) {
            adapterCombustible.actualizarLista(it)
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.exito.observe(viewLifecycleOwner) { exito ->
            if (exito) {
                Toast.makeText(requireContext(), "Guardado correctamente", Toast.LENGTH_SHORT).show()
                viewModel.resetExito()
            }
        }
    }

    private fun mostrarDialogoAgregar(tipo: String) {
        val input = EditText(requireContext())
        input.hint = "Ej: Diesel"
        val padding = (16 * resources.displayMetrics.density).toInt()
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = padding
        params.rightMargin = padding
        input.layoutParams = params
        container.addView(input)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Nueva Categoría - $tipo")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = input.text.toString().trim()
                viewModel.guardarCategoria(nombre, tipo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEliminar(categoria: com.lingomak.lingomakapp.data.model.CategoriaModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Categoría")
            .setMessage("¿Estás seguro de eliminar '${categoria.nombre}'? Los elementos existentes que la usen conservarán el nombre, pero la categoría ya no aparecerá en las nuevas listas.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarCategoria(categoria.uid)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
