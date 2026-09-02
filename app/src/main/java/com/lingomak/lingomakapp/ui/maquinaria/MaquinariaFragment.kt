package com.lingomak.lingomakapp.ui.maquinaria

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.BitacoraUsoModel
import com.lingomak.lingomakapp.databinding.FragmentMaquinariaAdminBinding
import com.lingomak.lingomakapp.utils.CsvExporter
import com.google.gson.Gson

class MaquinariaFragment : Fragment() {

    private var _binding: FragmentMaquinariaAdminBinding? = null
    private val binding get() = _binding!!

    private val maquinariaViewModel: MaquinariaViewModel by viewModels()
    private val bitacoraViewModel: BitacoraUsoViewModel by viewModels()

    private lateinit var pagerAdapter: MaquinariaAdminPagerAdapter
    private var bitacoraFiltrada: List<BitacoraUsoModel> = emptyList()

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val csvContent = CsvExporter.buildBitacoraCsvContent(bitacoraFiltrada)
            CsvExporter.saveCsvToUri(requireContext(), it, csvContent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // El pagerAdapter se crea UNA sola vez por instancia de Fragment (onCreate, no onCreateView).
        // Antes se creaba dentro de setupViewPager(), que corre en cada onCreateView — y onCreateView
        // se vuelve a ejecutar completo cada vez que se vuelve de DetalleBitacoraFragment (porque esa
        // navegación usa replace() + addToBackStack, que destruye la vista de este fragment y la
        // recrea al volver). Eso generaba un pagerAdapter nuevo en cada regreso, con su propio
        // expandedFilters reiniciado, mientras el ViewPager2/TabLayout podían restaurar currentItem=1
        // por su cuenta — el desfase entre "adapter nuevo, vacío" y "vista restaurada en la pestaña 1"
        // es lo que rompía el toggle de filtros.
        pagerAdapter = MaquinariaAdminPagerAdapter(
            onMaquinariaClick = { abrirDetalleMaquinaria(it.uid) },
            onBitacoraClick = { abrirDetalleBitacora(it) },
            onBitacoraFiltered = { bitacoraFiltrada = it }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMaquinariaAdminBinding.inflate(inflater, container, false)

        setupViewPager()
        configurarBusqueda()
        configurarEventos()
        observarViewModels()

        maquinariaViewModel.listarMaquinarias()
        bitacoraViewModel.cargarBitacora()

        return binding.root
    }

    private fun setupViewPager() {
        // pagerAdapter ya existe (creado en onCreate) — solo se reengancha a la vista nueva.
        binding.viewPagerMaquinaria.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayoutMaquinaria, binding.viewPagerMaquinaria) { tab, position ->
            tab.text = if (position == 0) "MAQUINARIA" else "BITÁCORA DE USO"
        }.attach()

        binding.viewPagerMaquinaria.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                actualizarFabPorPagina(position)
            }
        })
        
        // Llamada manual inicial (Tarea 9)
        actualizarFabPorPagina(binding.viewPagerMaquinaria.currentItem)
    }

    private fun actualizarFabPorPagina(position: Int) {
        binding.btnExportarBitacoraCsv.visibility = if (position == 1) View.VISIBLE else View.GONE

        // Actualizar FAB según pestaña
        if (position == 0) {
            binding.fabAccionMaquinaria.text = "REGISTRAR ACTIVO"
        } else {
            binding.fabAccionMaquinaria.text = "REGISTRAR USO"
        }
    }

    private fun observarViewModels() {
        maquinariaViewModel.listaMaquinarias.observe(viewLifecycleOwner) {
            pagerAdapter.updateMaquinarias(it)
            maquinariaViewModel.cargarNivelesCombustible(it)
        }
        maquinariaViewModel.nivelesCombustible.observe(viewLifecycleOwner) {
            pagerAdapter.updateNivelesCombustible(it)
        }
        maquinariaViewModel.maquinariasActivas.observe(viewLifecycleOwner) {
            pagerAdapter.updateCatalogoMaquinas(it)
        }
        maquinariaViewModel.operariosActivos.observe(viewLifecycleOwner) {
            pagerAdapter.updateCatalogoOperarios(it)
        }
        maquinariaViewModel.categorias.observe(viewLifecycleOwner) {
            pagerAdapter.updateCategorias(it)
        }
        bitacoraViewModel.listaBitacora.observe(viewLifecycleOwner) {
            pagerAdapter.updateBitacora(it)
        }
    }

    private fun configurarBusqueda() {
        binding.etBuscarMaquinaria.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pagerAdapter.updateSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun configurarEventos() {
        binding.fabAccionMaquinaria.setOnClickListener {
            if (binding.viewPagerMaquinaria.currentItem == 0) {
                abrirAgregarMaquinaria()
            } else {
                abrirRegistrarUso()
            }
        }

        binding.btnExportarBitacoraCsv.setOnClickListener {
            if (bitacoraFiltrada.isNotEmpty()) {
                CsvExporter.exportBitacora(
                    requireContext(),
                    bitacoraFiltrada,
                    createDocumentLauncher,
                    pagerAdapter.bitacoraFechaInicio,
                    pagerAdapter.bitacoraFechaFin
                )
            } else {
                Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirDetalleMaquinaria(uid: String) {
        val fragment = DetalleMaquinariaFragment()
        val bundle = Bundle()
        bundle.putString("uid", uid)
        fragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun abrirDetalleBitacora(model: BitacoraUsoModel) {
        val fragment = DetalleBitacoraFragment()
        val bundle = Bundle()
        bundle.putString("registro_uid", model.uid)
        fragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun abrirAgregarMaquinaria() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerAdmin, AgregarMaquinariaFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun abrirRegistrarUso() {
        val maquinas = pagerAdapter.catalogoMaquinasActual.map { it.nombre }.sorted()
        if (maquinas.isEmpty()) {
            Toast.makeText(requireContext(), "No hay maquinarias operativas disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.RoundedAlertDialog)
            .setTitle("Seleccione maquinaria")
            .setItems(maquinas.toTypedArray()) { _, which ->
                val nombre = maquinas[which]
                val maq = pagerAdapter.catalogoMaquinasActual.first { it.nombre == nombre }
                val fragment = RegistrarUsoMaquinariaFragment()
                val bundle = Bundle().apply {
                    putString("uidMaquinaria", maq.uid)
                    putString("codigoMaquinaria", maq.codigoMaquinaria)
                    putString("nombreMaquinaria", maq.nombre)
                    putString("tipoMaquinaria", maq.tipo)
                    putDouble("horometroActual", maq.horometroActual)
                    putDouble("horometroUltimoMantenimiento", maq.horometroUltimoMantenimiento)
                    putDouble("intervaloMantenimientoHoras", maq.intervaloMantenimientoHoras)
                    if (maq.capacidadTanqueGls != null) {
                        putDouble("capacidadTanqueGls", maq.capacidadTanqueGls!!)
                    }
                }
                fragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerAdmin, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
