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
        val isOperario = requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardOperarioActivity

        adapter = MaquinariaAdapter(
            listaMaquinarias = emptyList(),
            isOperario = isOperario,
            onMaquinariaClick = { maquinaria ->


                val detalleFragment = DetalleMaquinariaFragment()


                val bundle = Bundle()


                bundle.putString("nombre", maquinaria.nombre)
                bundle.putString("codigoMaquinaria", maquinaria.codigoMaquinaria)
                bundle.putString("tipo", maquinaria.tipo)
                bundle.putString("marca", maquinaria.marca)
                bundle.putString("modelo", maquinaria.modelo)
                bundle.putString("placaSerie", maquinaria.placaSerie)
                bundle.putInt("anio", maquinaria.anio)
                bundle.putString("estado", maquinaria.estado)
                bundle.putInt("horometroActual", maquinaria.horometroActual)
                bundle.putInt(
                    "horometroUltimoMantenimiento",
                    maquinaria.horometroUltimoMantenimiento
                )
                bundle.putString("ubicacionActual", maquinaria.ubicacionActual)
                bundle.putString("observaciones", maquinaria.observaciones)
                bundle.putString("imagenUrl", maquinaria.imagenUrl)


                detalleFragment.arguments = bundle

                val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
                    R.id.fragmentContainerAdmin else R.id.containerOperario

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        containerId,
                        detalleFragment
                    )

                    .addToBackStack(null)
                    .commit()
            },

            onEditarClick = { maquinaria ->
                val editarFragment = EditarMaquinariaFragment()


                val bundle = Bundle().apply {
                    putString("uid", maquinaria.uid)
                    putString("codigoMaquinaria", maquinaria.codigoMaquinaria)
                    putString("nombre", maquinaria.nombre)
                    putString("tipo", maquinaria.tipo)
                    putString("marca", maquinaria.marca)
                    putString("modelo", maquinaria.modelo)
                    putString("placaSerie", maquinaria.placaSerie)
                    putInt("anio", maquinaria.anio)
                    putString("estado", maquinaria.estado)
                    putInt("horometroActual", maquinaria.horometroActual)
                    putInt("horometroUltimoMantenimiento", maquinaria.horometroUltimoMantenimiento)
                    putString("ubicacionActual", maquinaria.ubicacionActual)
                    putString("observaciones", maquinaria.observaciones)
                    putString("imagenUrl", maquinaria.imagenUrl)
                    putString("fechaRegistro", maquinaria.fechaRegistro)
                    putString("registradoPor", maquinaria.registradoPor)
                }


                editarFragment.arguments = bundle

                val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
                    R.id.fragmentContainerAdmin else R.id.containerOperario

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        containerId,
                        editarFragment
                    )
                    .addToBackStack(null)
                    .commit()
            },

            onCambiarEstadoClick = { maquinaria ->
                mostrarDialogoCambiarEstado(maquinaria)
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

    private fun mostrarDialogoCambiarEstado(maquinaria: MaquinariaModel) {


        val estados = arrayOf(
            "OPERATIVA",
            "EN_MANTENIMIENTO",
            "INACTIVA"
        )


        val estadoActual = maquinaria.estado


        val posicionActual = estados.indexOf(estadoActual)


        var estadoSeleccionado = estadoActual

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cambiar estado")
            .setSingleChoiceItems(
                estados,
                posicionActual
            ) { _, which ->
                estadoSeleccionado = estados[which]
            }
            .setPositiveButton("Guardar") { dialog, _ ->

                if (estadoSeleccionado == estadoActual) {
                    Toast.makeText(
                        requireContext(),
                        "No se realizaron cambios",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                    return@setPositiveButton
                }

                viewModel.cambiarEstadoMaquinaria(
                    uid = maquinaria.uid,
                    nuevoEstado = estadoSeleccionado,
                    onSuccess = {

                        Toast.makeText(
                            requireContext(),
                            "Estado actualizado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
