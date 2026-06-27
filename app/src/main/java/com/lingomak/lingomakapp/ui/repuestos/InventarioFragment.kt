package com.lingomak.lingomakapp.ui.repuestos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.databinding.FragmentInventarioBinding

/**
 * Fragment del Listado de Repuestos (módulo Inventario).
 * Controla la UI vía ViewBinding, observa el InventarioViewModel
 * y configura los eventos de búsqueda y filtros.
 *
 * NOTA: Asume un layout fragment_inventario.xml con, como mínimo,
 * los siguientes IDs (ajusta a los reales de tu XML):
 * - rvRepuestos (RecyclerView)
 * - etBuscar (EditText de búsqueda)
 * - spinnerCategoria (Spinner de filtro por categoría)
 * - chipTodos, chipActivos, chipInactivos (filtros rápidos de estado)
 * - fabAgregarRepuesto (botón para ir a AgregarRepuestoFragment)
 * - progressBar (indicador de carga)
 */
class InventarioFragment : Fragment() {

    private var _binding: FragmentInventarioBinding? = null

    private val binding get() = _binding!!

    private val viewModel: InventarioViewModel by viewModels()

    private lateinit var adapter: RepuestosAdapter

    // Categorías disponibles para el Spinner de filtro.
    // Ajusta esta lista a las categorías reales de tu negocio.
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


        configurarRecyclerView()

        configurarSpinnerCategorias()

        observarViewModel()

        viewModel.listarRepuestos()

        configurarEventos()

        return binding.root
    }

    private fun configurarRecyclerView() {


        adapter = RepuestosAdapter(
            listaRepuestos = emptyList(),

            onItemClick = { repuesto ->
                val fragment = DetalleRepuestoFragment()

                val bundle = Bundle()

                bundle.putString("uid", repuesto.uid)

                fragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(
                        (requireView().parent as ViewGroup).id,
                        fragment
                    )
                    .addToBackStack(null)
                    .commit()

            }
        )


        binding.rvRepuestos.layoutManager = LinearLayoutManager(requireContext())


        binding.rvRepuestos.adapter = adapter
    }

    private fun configurarSpinnerCategorias() {

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categorias
        )

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerCategoria.adapter = spinnerAdapter
    }

    private fun observarViewModel() {


        viewModel.repuestos.observe(viewLifecycleOwner) { listaRepuestos ->
            adapter.actualizarLista(listaRepuestos)
        }

        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }

        viewModel.loading.observe(viewLifecycleOwner) { estaCargando ->
            binding.progressBar.visibility = if (estaCargando) View.VISIBLE else View.GONE
        }
    }

    private fun configurarEventos() {

        // Búsqueda por nombre o código interno (se filtra en cada cambio de texto).
        binding.etBuscar.addTextChangedListener(
            onTextChanged = { texto, _, _, _ ->
                viewModel.buscarRepuesto(texto?.toString() ?: "")
            }
        )

        // Filtro por categoría (Spinner).
        binding.spinnerCategoria.onItemSelectedListener = object :
            android.widget.AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val categoriaSeleccionada = categorias[position]

                if (categoriaSeleccionada == "Todas las categorías") {
                    viewModel.filtrarPorCategoria(null)
                } else {
                    viewModel.filtrarPorCategoria(categoriaSeleccionada)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // No se requiere acción.
            }
        }

        // Filtros rápidos de criticidad.
        binding.chipTodosCriticidad.setOnClickListener {
            viewModel.filtrarPorCriticidad(InventarioViewModel.NivelCriticidad.TODOS)
        }

        binding.chipEnStock.setOnClickListener {
            viewModel.filtrarPorCriticidad(InventarioViewModel.NivelCriticidad.EN_STOCK)
        }

        binding.chipBajoStock.setOnClickListener {
            viewModel.filtrarPorCriticidad(InventarioViewModel.NivelCriticidad.BAJO_STOCK)
        }

        binding.chipSinStock.setOnClickListener {
            viewModel.filtrarPorCriticidad(InventarioViewModel.NivelCriticidad.SIN_STOCK)
        }

        // Filtros rápidos de estado.
        binding.chipTodosEstado.setOnClickListener {
            viewModel.filtrarPorEstado(null)
        }

        binding.chipActivos.setOnClickListener {
            viewModel.filtrarPorEstado("ACTIVO")
        }

        binding.chipInactivos.setOnClickListener {
            viewModel.filtrarPorEstado("INACTIVO")
        }

        // Botón para ir a la pantalla de Agregar Repuesto.
        binding.fabAgregarRepuesto.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    (requireView().parent as ViewGroup).id,
                    AgregarRepuestoFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun mostrarDialogoCambiarEstado(repuesto: RepuestoModel) {
        val nuevoEstado =
            if (repuesto.estado == "ACTIVO") {
                "INACTIVO"
            } else {
                "ACTIVO"
            }

        val mensaje =
            if (nuevoEstado == "INACTIVO") {
                "Deseas inactivar ${repuesto.nombre}"
            } else {
                "Deseas activar ${repuesto.nombre}"
            }

        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar acción")
            .setMessage(mensaje)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                viewModel.cambiarEstadoRepuesto(repuesto.uid, nuevoEstado)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
