package com.lingomak.lingomakapp.ui.maquinaria

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.databinding.FragmentMaquinariaBinding

class MaquinariaFragment : Fragment() {

    private var _binding: FragmentMaquinariaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MaquinariaViewModel by viewModels()

    private lateinit var adapter: MaquinariaAdapter

    private var listaCompleta = listOf<MaquinariaModel>()

    private var filtroEstado = "TODOS"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMaquinariaBinding.inflate(inflater, container, false)

        configurarRecyclerView()
        observarViewModel()
        configurarBusqueda()
        configurarFiltros()
        configurarEventos()

        viewModel.listarMaquinarias()

        return binding.root
    }

    private fun configurarRecyclerView() {
        adapter = MaquinariaAdapter(
            listaMaquinarias = emptyList(),
            onMaquinariaClick = { maquinaria ->
                val detalleFragment = DetalleMaquinariaFragment()
                val bundle = Bundle()
                bundle.putString("uid", maquinaria.uid)
                detalleFragment.arguments = bundle

                val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
                    R.id.fragmentContainerAdmin else R.id.containerOperario

                parentFragmentManager
                    .beginTransaction()
                    .replace(containerId, detalleFragment)
                    .addToBackStack(null)
                    .commit()
            }
        )

        binding.rvMaquinarias.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMaquinarias.adapter = adapter
    }

    private fun observarViewModel() {

        viewModel.listaMaquinarias.observe(viewLifecycleOwner) { lista ->

            listaCompleta = lista

            aplicarFiltros()
        }

        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarBusqueda() {

        binding.etBuscarMaquinaria.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                aplicarFiltros()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun configurarFiltros() {
        binding.chipGroupEstado.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: View.NO_ID
            filtroEstado = when (checkedId) {
                binding.chipEstadoOperativa.id -> "OPERATIVA"
                binding.chipEstadoMantenimiento.id -> "EN_MANTENIMIENTO"
                binding.chipEstadoInactiva.id -> "INACTIVA"
                else -> "TODOS"
            }
            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {

        val textoBusqueda = binding.etBuscarMaquinaria.text
            .toString()
            .trim()
            .lowercase()

        val listaFiltrada = listaCompleta.filter { maquinaria ->

            val coincideBusqueda =
                maquinaria.nombre.lowercase().contains(textoBusqueda) ||
                        maquinaria.codigoMaquinaria.lowercase().contains(textoBusqueda) ||
                        maquinaria.placaSerie.lowercase().contains(textoBusqueda)

            val coincideEstado =
                filtroEstado == "TODOS" || maquinaria.estado == filtroEstado

            coincideBusqueda && coincideEstado
        }

        adapter.actualizarLista(listaFiltrada)

        actualizarResumen(listaFiltrada)
    }

    private fun actualizarResumen(lista: List<MaquinariaModel>) {

        binding.tvTotalMaquinaria.text = lista.size.toString()

        binding.tvOperativasMaquinaria.text =
            lista.count { it.estado == "OPERATIVA" }.toString()

        binding.tvMantenimientoMaquinaria.text =
            lista.count { it.estado == "EN_MANTENIMIENTO" }.toString()

        binding.tvInactivasMaquinaria.text =
            lista.count { it.estado == "INACTIVA" }.toString()
    }

    private fun configurarEventos() {
        if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardOperarioActivity) {
            binding.fabAgregarMaquinaria.visibility = View.GONE
        }

        binding.fabAgregarMaquinaria.setOnClickListener {
            val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
                R.id.fragmentContainerAdmin else R.id.containerOperario

            parentFragmentManager
                .beginTransaction()
                .replace(
                    containerId,
                    AgregarMaquinariaFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
