package com.lingomak.lingomakapp.ui.mantenimiento

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.tabs.TabLayoutMediator
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.FragmentMantenimientoBinding
import com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity
import com.lingomak.lingomakapp.utils.CsvExporter

class MantenimientoFragment : Fragment() {

    private var _binding: FragmentMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MantenimientoViewModel by viewModels()

    private lateinit var pagerAdapter: MantenimientoPagerAdapter
    private var historyVisibleList: List<MantenimientoModel> = emptyList()

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val csvContent = CsvExporter.buildMantenimientoCsvContent(historyVisibleList)
            CsvExporter.saveCsvToUri(requireContext(), it, csvContent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMantenimientoBinding.inflate(inflater, container, false)

        setupViewPager()
        configurarEventos()
        configurarBusqueda()
        observarViewModel()

        val isAdmin = requireActivity() is DashboardAdminActivity
        viewModel.listarMantenimientos(soloAsignados = !isAdmin)

        return binding.root
    }

    private fun setupViewPager() {
        val isAdmin = requireActivity() is DashboardAdminActivity
        
        pagerAdapter = MantenimientoPagerAdapter(
            isOperario = !isAdmin,
            onMantenimientoClick = { abrirDetalleMantenimiento(it) },
            onEditarClick = { abrirEditarMantenimiento(it) },
            onCambiarEstadoClick = { mostrarDialogoCambiarEstado(it) },
            onCancelarClick = { mostrarDialogoCancelarMantenimiento(it) },
            onFinalizarClick = { abrirFinalizarMantenimiento(it) },
            onHistoryFiltersChanged = { list -> historyVisibleList = list }
        )
        
        binding.viewPagerMantenimiento.adapter = pagerAdapter
        
        if (!isAdmin) {
            // Si es operario, ocultamos las pestañas y bloqueamos la navegación.
            // Solo verá la primera página (Mantenimientos Activos).
            binding.tabLayoutMantenimiento.visibility = View.GONE
            binding.viewPagerMantenimiento.isUserInputEnabled = false
        }

        TabLayoutMediator(binding.tabLayoutMantenimiento, binding.viewPagerMantenimiento) { tab, position ->
            tab.text = if (position == 0) "ACTIVOS" else "HISTORIAL"
        }.attach()
        
        binding.viewPagerMantenimiento.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // El botón exportar solo vive en Historial (Pestaña 1) para el Admin
                binding.btnExportarCsv.visibility = if (position == 1 && isAdmin) View.VISIBLE else View.GONE
            }
        })
    }

    private fun observarViewModel() {
        viewModel.listaMantenimientos.observe(viewLifecycleOwner) { lista ->
            pagerAdapter.updateData(lista)
        }

        viewModel.mensajeError.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirDetalleMantenimiento(mantenimiento: MantenimientoModel) {
        val fragment = DetalleMantenimientoFragment()
        val bundle = Bundle()
        bundle.putString("uid", mantenimiento.uid)
        fragment.arguments = bundle

        val isAdmin = requireActivity() is DashboardAdminActivity
        val containerId = if (isAdmin) R.id.fragmentContainerAdmin else R.id.containerOperario

        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun configurarEventos() {
        binding.fabAgregarMantenimiento.setOnClickListener {
            val fragment = ProgramarMantenimientoFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerAdmin, fragment)
                .addToBackStack(null)
                .commit()
        }
        
        if (requireActivity() !is DashboardAdminActivity) {
            binding.fabAgregarMantenimiento.visibility = View.GONE
            binding.btnExportarCsv.visibility = View.GONE
        }

        binding.btnExportarCsv.setOnClickListener {
            if (historyVisibleList.isNotEmpty()) {
                CsvExporter.exportMantenimiento(requireContext(), historyVisibleList, createDocumentLauncher)
            } else {
                Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarBusqueda() {
        binding.etBuscarMantenimiento.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pagerAdapter.updateSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun mostrarDialogoCambiarEstado(mantenimiento: MantenimientoModel) {
        val estados = arrayOf("PENDIENTE", "EN_PROCESO", "CANCELADO")
        AlertDialog.Builder(requireContext())
            .setTitle("Cambiar estado")
            .setItems(estados) { _, which ->
                val nuevoEstado = estados[which]
                if (nuevoEstado == "EN_PROCESO") {
                    viewModel.iniciarMantenimiento(mantenimiento.uid, mantenimiento.uidMaquinaria) {
                        Toast.makeText(requireContext(), "Mantenimiento iniciado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.cambiarEstadoMantenimiento(mantenimiento.uid, nuevoEstado) {
                        Toast.makeText(requireContext(), "Estado actualizado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun abrirEditarMantenimiento(mantenimiento: MantenimientoModel) {
        val fragment = EditarMantenimientoFragment()
        val bundle = Bundle()
        bundle.putString("uid", mantenimiento.uid)
        fragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun mostrarDialogoCancelarMantenimiento(mantenimiento: MantenimientoModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancelar Mantenimiento")
            .setMessage("¿Estás seguro de cancelar el mantenimiento ${mantenimiento.codigoMantenimiento}?")
            .setPositiveButton("Sí, cancelar") { _, _ ->
                viewModel.cambiarEstadoMantenimiento(mantenimiento.uid, "CANCELADO") {
                    Toast.makeText(requireContext(), "Mantenimiento cancelado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun abrirFinalizarMantenimiento(mantenimiento: MantenimientoModel) {
        val fragment = FinalizarMantenimientoFragment()
        val bundle = Bundle()
        bundle.putString("uid", mantenimiento.uid)
        fragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
