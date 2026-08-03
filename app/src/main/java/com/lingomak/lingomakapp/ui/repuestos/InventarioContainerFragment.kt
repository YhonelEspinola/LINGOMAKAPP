package com.lingomak.lingomakapp.ui.repuestos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.databinding.FragmentInventarioContainerBinding
import com.lingomak.lingomakapp.ui.movimientos.MovimientosGlobalViewModel

class InventarioContainerFragment : Fragment() {

    private var _binding: FragmentInventarioContainerBinding? = null
    private val binding get() = _binding!!

    private val inventarioViewModel: InventarioViewModel by viewModels()
    private val movimientosViewModel: MovimientosGlobalViewModel by viewModels()

    private lateinit var pagerAdapter: InventarioPagerAdapter
    private var movimientosFiltrados: List<Pair<MovimientoModel, String>> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventarioContainerBinding.inflate(inflater, container, false)
        
        setupViewPager()
        setupBusqueda()
        configurarEventos()
        observarViewModels()

        val tab = arguments?.getInt("tab", 0) ?: 0
        binding.viewPagerInventario.setCurrentItem(tab, false)
        actualizarFabYExportar(tab)

        return binding.root
    }

    private fun setupViewPager() {
        pagerAdapter = InventarioPagerAdapter(
            onRepuestoClick = { repuesto -> abrirDetalleRepuesto(repuesto.uid) },
            onMovimientoClick = { pair -> abrirDetalleMovimiento(pair.first.uid) },
            onMovimientosFiltered = { movimientosFiltrados = it }
        )
        binding.viewPagerInventario.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayoutInventario, binding.viewPagerInventario) { tab, position ->
            tab.text = if (position == 0) "INVENTARIO" else "MOVIMIENTOS"
        }.attach()

        binding.viewPagerInventario.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                actualizarFabYExportar(position)
            }
        })
    }

    private fun actualizarFabYExportar(position: Int) {
        val isAdmin = requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity
        binding.btnExportarCsv.visibility = if (isAdmin) View.VISIBLE else View.GONE
        
        if (position == 0) {
            binding.fabAccionInventario.text = "REGISTRAR REPUESTO"
        } else {
            binding.fabAccionInventario.text = "REGISTRAR MOVIMIENTO"
        }
    }

    private fun setupBusqueda() {
        binding.etBuscarInventario.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pagerAdapter.updateSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private val exportInventarioLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { destinationUri ->
            val data = inventarioViewModel.repuestos.value ?: emptyList()
            val header = "Código Interno,Nombre,Categoría,Marca,Stock Actual,Stock Mínimo,Stock Máximo,Ubicación,Estado,Proveedor Nombre,Proveedor Contacto,Fecha de Registro"
            val rows = data.map {
                val fecha = it.fechaRegistro?.let { d -> java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(d) } ?: ""
                "${com.lingomak.lingomakapp.utils.CsvExporter.escapeCsv(it.codigoInterno)},${com.lingomak.lingomakapp.utils.CsvExporter.escapeCsv(it.nombre)},${com.lingomak.lingomakapp.utils.CsvExporter.escapeCsv(it.categoria)},${com.lingomak.lingomakapp.utils.CsvExporter.escapeCsv(it.marca)},${it.stockActual},${it.stockMinimo},${it.stockMaximo},${com.lingomak.lingomakapp.utils.CsvExporter.escapeCsv(it.ubicacionAlmacen)},${com.lingomak.lingomakapp.utils.CsvExporter.escapeCsv(it.estado)},${com.lingomak.lingomakapp.utils.CsvExporter.escapeCsv(it.proveedorNombre)},${com.lingomak.lingomakapp.utils.CsvExporter.escapeCsv(it.proveedorContacto)},$fecha"
            }
            val csvContent = header + "\n" + rows.joinToString("\n")
            com.lingomak.lingomakapp.utils.CsvExporter.saveCsvToUri(requireContext(), destinationUri, csvContent)
        }
    }

    private val exportMovimientosLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { destinationUri ->
            val data = movimientosFiltrados
            val csvContent = com.lingomak.lingomakapp.utils.CsvExporter.buildMovimientosCsvContent(data)
            com.lingomak.lingomakapp.utils.CsvExporter.saveCsvToUri(requireContext(), destinationUri, csvContent)
        }
    }

    private fun configurarEventos() {
        binding.fabAccionInventario.setOnClickListener {
            if (binding.viewPagerInventario.currentItem == 0) {
                abrirAgregarRepuesto()
            } else {
                abrirRegistrarMovimiento()
            }
        }

        binding.btnExportarCsv.setOnClickListener {
            if (binding.viewPagerInventario.currentItem == 0) {
                val data = inventarioViewModel.repuestos.value ?: emptyList()
                com.lingomak.lingomakapp.utils.CsvExporter.exportInventario(requireContext(), data, exportInventarioLauncher)
            } else {
                com.lingomak.lingomakapp.utils.CsvExporter.exportMovimientos(
                    context = requireContext(),
                    data = movimientosFiltrados,
                    launcher = exportMovimientosLauncher,
                    inicio = pagerAdapter.movFechaInicio,
                    fin = pagerAdapter.movFechaFin
                )
            }
        }
    }

    private fun abrirAgregarRepuesto() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerAdmin, AgregarRepuestoFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun abrirRegistrarMovimiento() {
        // Asumiendo que el escaneo QR es el inicio del registro de movimiento
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerAdmin, com.lingomak.lingomakapp.ui.movimientos.EscaneoQRFragment())
            .addToBackStack(null)
            .commit()
    }


    private fun observarViewModels() {
        // Observamos los datos crudos/filtrados de los ViewModels existentes
        inventarioViewModel.repuestos.observe(viewLifecycleOwner) {
            pagerAdapter.updateRepuestos(it)
        }
        inventarioViewModel.categorias.observe(viewLifecycleOwner) {
            pagerAdapter.updateCategorias(it)
        }
        movimientosViewModel.movimientosFiltrados.observe(viewLifecycleOwner) {
            pagerAdapter.updateMovimientos(it)
        }
        
        // Usuarios para el dropdown de movimientos
        val userRepo = com.lingomak.lingomakapp.data.repository.UserRepository(requireContext())
        userRepo.obtenerUsuariosObservable().observe(viewLifecycleOwner) {
            pagerAdapter.updateUsuarios(it)
        }
    }

    private fun abrirDetalleRepuesto(uid: String) {
        val fragment = DetalleRepuestoFragment()
        fragment.arguments = Bundle().apply { putString("uid", uid) }
        
        val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
            R.id.fragmentContainerAdmin else R.id.containerOperario
            
        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun abrirDetalleMovimiento(uid: String) {
        val fragment = com.lingomak.lingomakapp.ui.movimientos.DetalleMovimientoFragment()
        fragment.arguments = Bundle().apply { putString("movimientoUid", uid) }
        
        val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
            R.id.fragmentContainerAdmin else R.id.containerOperario

        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
