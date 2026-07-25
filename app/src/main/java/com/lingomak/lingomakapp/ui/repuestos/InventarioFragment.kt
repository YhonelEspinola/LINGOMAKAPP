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
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.contract.ActivityResultContracts
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.databinding.FragmentInventarioBinding
import com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity
import com.lingomak.lingomakapp.utils.CsvExporter

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

    private val viewModel: InventarioViewModel by activityViewModels()

    private lateinit var adapter: RepuestosAdapter
    private var listaCategoriasFiltro: List<String> = emptyList()

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val data = viewModel.repuestos.value ?: emptyList()
            val header = "Código Interno,Nombre,Categoría,Marca,Stock Actual,Stock Mínimo,Stock Máximo,Ubicación,Estado,Proveedor Nombre,Proveedor Contacto,Fecha de Registro"
            val rows = data.map {
                val fecha = it.fechaRegistro?.let { d -> java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(d) } ?: ""
                "${CsvExporter.escapeCsv(it.codigoInterno)},${CsvExporter.escapeCsv(it.nombre)},${CsvExporter.escapeCsv(it.categoria)},${CsvExporter.escapeCsv(it.marca)},${it.stockActual},${it.stockMinimo},${it.stockMaximo},${CsvExporter.escapeCsv(it.ubicacionAlmacen)},${CsvExporter.escapeCsv(it.estado)},${CsvExporter.escapeCsv(it.proveedorNombre)},${CsvExporter.escapeCsv(it.proveedorContacto)},$fecha"
            }
            val csvContent = header + "\n" + rows.joinToString("\n")
            CsvExporter.saveCsvToUri(requireContext(), it, csvContent)
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventarioBinding.inflate(inflater, container, false)

        configurarRecyclerView()
        observarViewModel()

        // Forzar sincronización con Firestore al entrar para asegurar datos frescos
        viewModel.listarRepuestos()

        configurarEventos()
        validarAccesoAdmin()

        return binding.root
    }

    private fun validarAccesoAdmin() {
        val isAdmin = requireActivity() is DashboardAdminActivity
        binding.btnExportarCsv.visibility = if (isAdmin) View.VISIBLE else View.GONE
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

        // Paginación (Scroll Infinito)
        binding.rvRepuestos.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
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
    }

    private fun configurarSpinnerCategorias(categorias: List<String>) {
        listaCategoriasFiltro = listOf("Todas las categorías") + categorias
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaCategoriasFiltro
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategoria.adapter = spinnerAdapter
    }

    private fun observarViewModel() {
        viewModel.categorias.observe(viewLifecycleOwner) { lista ->
            configurarSpinnerCategorias(lista.map { it.nombre })
        }

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
                if (listaCategoriasFiltro.isEmpty()) return
                val categoriaSeleccionada = listaCategoriasFiltro[position]

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

        // Filtros rápidos de criticidad (Selección Única)
        binding.chipTodosCriticidad.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorCriticidad(InventarioViewModel.NivelCriticidad.TODOS)
        }

        binding.chipEnStock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorCriticidad(InventarioViewModel.NivelCriticidad.EN_STOCK)
        }

        binding.chipBajoStock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorCriticidad(InventarioViewModel.NivelCriticidad.BAJO_STOCK)
        }

        binding.chipSinStock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorCriticidad(InventarioViewModel.NivelCriticidad.SIN_STOCK)
        }

        // Filtros rápidos de estado (Selección Única)
        binding.chipTodosEstado.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorEstado(null)
        }

        binding.chipActivos.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorEstado("ACTIVO")
        }

        binding.chipInactivos.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filtrarPorEstado("INACTIVO")
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

        binding.btnExportarCsv.setOnClickListener {
            val data = viewModel.repuestos.value ?: emptyList()
            CsvExporter.exportInventario(requireContext(), data, createDocumentLauncher)
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
