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
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.FragmentMantenimientoBinding
import com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity

class MantenimientoFragment : Fragment() {

    private var _binding: FragmentMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MantenimientoViewModel by viewModels()

    private var listaCompleta: List<MantenimientoModel> = emptyList()

    private var filtroTipo: String = "TODOS"
    private var filtroEstado: String = "TODOS"

    private lateinit var adapter: MantenimientoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMantenimientoBinding.inflate(inflater, container, false)

        configurarRecyclerView()
        configurarEventos()
        configurarFiltros()
        configurarBusqueda()
        observarViewModel()

        viewModel.listarMantenimientos()

        return binding.root
    }

    private fun configurarRecyclerView() {
        val isAdmin = requireActivity() is DashboardAdminActivity

        adapter = MantenimientoAdapter(
            listaMantenimientos = emptyList(),
            isOperario = !isAdmin,
            onMantenimientoClick = { mantenimiento ->
                abrirDetalleMantenimiento(mantenimiento)
            },
            onEditarClick = { mantenimiento ->
                abrirEditarMantenimiento(mantenimiento)
            },
            onCambiarEstadoClick = { mantenimiento ->
                mostrarDialogoCambiarEstado(mantenimiento)
            },
            onCancelarClick = { mantenimiento ->
                mostrarDialogoCancelarMantenimiento(mantenimiento)
            },
            onFinalizarClick = { mantenimiento ->
                abrirFinalizarMantenimiento(mantenimiento)
            }
        )

        binding.rvMantenimientos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMantenimientos.adapter = adapter
    }

    private fun observarViewModel() {
        viewModel.listaMantenimientos.observe(viewLifecycleOwner) { lista ->
            listaCompleta = lista
            aplicarFiltros()
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
        }
    }

    private fun configurarFiltros() {
        binding.chipGroupTipoMantenimiento.setOnCheckedStateChangeListener { _, checkedIds ->
            filtroTipo = when (checkedIds.firstOrNull()) {
                R.id.chipTipoPreventivo -> "PREVENTIVO"
                R.id.chipTipoCorrectivo -> "CORRECTIVO"
                R.id.chipTipoPredictivo -> "PREDICTIVO"
                else -> "TODOS"
            }
            aplicarFiltros()
        }

        binding.chipGroupEstadoMantenimiento.setOnCheckedStateChangeListener { _, checkedIds ->
            filtroEstado = when (checkedIds.firstOrNull()) {
                R.id.chipEstadoPendiente -> "PENDIENTE"
                R.id.chipEstadoProceso -> "EN_PROCESO"
                R.id.chipEstadoFinalizado -> "FINALIZADO"
                R.id.chipEstadoVencido -> "VENCIDO"
                else -> "TODOS"
            }
            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {
        var listaFiltrada = listaCompleta

        if (filtroTipo != "TODOS") {
            listaFiltrada = listaFiltrada.filter { it.tipoMantenimiento == filtroTipo }
        }

        if (filtroEstado != "TODOS") {
            listaFiltrada = listaFiltrada.filter { it.estado == filtroEstado }
        }

        val query = binding.etBuscarMantenimiento.text.toString().trim()
        if (query.isNotEmpty()) {
            listaFiltrada = listaFiltrada.filter {
                it.codigoMantenimiento.contains(query, ignoreCase = true) ||
                        it.nombreMaquinaria.contains(query, ignoreCase = true) ||
                        it.descripcion.contains(query, ignoreCase = true)
            }
        }

        adapter.actualizarLista(listaFiltrada)
        actualizarResumen(listaCompleta)
    }

    private fun configurarBusqueda() {
        binding.etBuscarMantenimiento.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                aplicarFiltros()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun actualizarResumen(lista: List<MantenimientoModel>) {
        val pendientes = lista.count { it.estado == "PENDIENTE" }
        val vencidos = lista.count { it.estado == "VENCIDO" }
        val finalizados = lista.count { it.estado == "FINALIZADO" }

        binding.tvPendientesMantenimiento.text = pendientes.toString()
        binding.tvVencidosMantenimiento.text = vencidos.toString()
        binding.tvFinalizadosMantenimiento.text = finalizados.toString()
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
