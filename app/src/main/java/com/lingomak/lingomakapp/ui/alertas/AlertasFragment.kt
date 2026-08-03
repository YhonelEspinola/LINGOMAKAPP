package com.lingomak.lingomakapp.ui.alertas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.FragmentAlertasBinding
import com.lingomak.lingomakapp.ui.mantenimiento.DetalleMantenimientoFragment
import com.lingomak.lingomakapp.ui.mantenimiento.MantenimientoViewModel
import com.lingomak.lingomakapp.ui.mantenimiento.SolicitudesMantenimientoFragment
import com.lingomak.lingomakapp.ui.repuestos.DetalleRepuestoFragment
import com.lingomak.lingomakapp.ui.repuestos.InventarioContainerFragment

class AlertasFragment : Fragment() {

    private val mantenimientoViewModel: MantenimientoViewModel by viewModels()
    private var _binding: FragmentAlertasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlertasViewModel by viewModels()

    private lateinit var adapter: AlertasAdapter
    
    private var listaOriginal: List<AlertaModel> = emptyList()
    private var categoriaFiltro: String = "Todas"
    private var subtipoFiltro: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertasBinding.inflate(inflater, container, false)

        configurarRecyclerView()
        configurarFiltros()
        observarViewModel()
        solicitarPermisoNotificaciones()
        
        val isOperario = requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardOperarioActivity
        viewModel.listarAlertas(isOperario)

        return binding.root
    }

    private fun configurarRecyclerView() {
        adapter = AlertasAdapter(emptyList()) { alerta ->
            tomarAccionAlerta(alerta)
        }
        binding.rvAlertas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAlertas.adapter = adapter
    }

    private fun configurarFiltros() {
        binding.cgCategorias.setOnCheckedStateChangeListener { _, checkedIds ->
            val chipId = checkedIds.firstOrNull()
            categoriaFiltro = when (chipId) {
                R.id.chipMantenimiento -> "MANTENIMIENTO"
                R.id.chipStock -> "INVENTARIO"
                R.id.chipMovimientos -> "MOVIMIENTOS"
                else -> "Todas"
            }
            subtipoFiltro = null
            actualizarSubtipos()
            aplicarFiltros()
        }
    }

    private fun actualizarSubtipos() {
        binding.cgSubtipos.removeAllViews()
        val subtipos = when (categoriaFiltro) {
            "MANTENIMIENTO" -> listOf("VENCIDO", "EN_PROCESO", "PROXIMO", "MANTENIMIENTO_PENDIENTE", "SOLICITUD_MANTENIMIENTO")
            "INVENTARIO" -> listOf("STOCK_AGOTADO", "STOCK_CRITICO", "STOCK_BAJO")
            "MOVIMIENTOS" -> listOf("ALTO_CONSUMO", "SIN_ROTACION")
            else -> emptyList()
        }

        if (subtipos.isEmpty()) {
            binding.scrollSubtipos.visibility = View.GONE
        } else {
            binding.scrollSubtipos.visibility = View.VISIBLE
            
            // Agregar opción "Todos los de esta categoría"
            val chipTodos = crearChipSubtipo("Todos los de $categoriaFiltro", null)
            chipTodos.isChecked = true
            binding.cgSubtipos.addView(chipTodos)

            subtipos.forEach { tipo ->
                binding.cgSubtipos.addView(crearChipSubtipo(tipo.replace("_", " "), tipo))
            }
        }
    }

    private fun crearChipSubtipo(texto: String, tipoValor: String?): Chip {
        return Chip(requireContext()).apply {
            text = texto
            isCheckable = true
            setChipBackgroundColorResource(R.color.selector_chip_choice)
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.selector_chip_text))
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    subtipoFiltro = tipoValor
                    aplicarFiltros()
                }
            }
        }
    }

    private fun aplicarFiltros() {
        var listaFiltrada = if (categoriaFiltro == "Todas") {
            listaOriginal
        } else {
            listaOriginal.filter { it.categoria == categoriaFiltro }
        }

        subtipoFiltro?.let { tipo ->
            listaFiltrada = listaFiltrada.filter { it.tipo == tipo }
        }

        adapter.actualizarLista(listaFiltrada)
        
        if (listaFiltrada.isEmpty()) {
            binding.rvAlertas.visibility = View.GONE
            binding.tvSinAlertas.visibility = View.VISIBLE
        } else {
            binding.rvAlertas.visibility = View.VISIBLE
            binding.tvSinAlertas.visibility = View.GONE
        }
    }

    private fun observarViewModel() {
        viewModel.listaAlertas.observe(viewLifecycleOwner) { lista ->
            listaOriginal = lista
            if (lista.isEmpty()) {
                binding.rvAlertas.visibility = View.GONE
                binding.tvSinAlertas.visibility = View.VISIBLE
                actualizarContadores(0, 0, 0)
                adapter.actualizarLista(emptyList())
            } else {
                aplicarFiltros()
                
                val criticas = lista.count { 
                    it.tipo == "STOCK_CRITICO" || 
                    it.tipo == "MANTENIMIENTO_VENCIDO" || 
                    it.prioridad == "ALTA" 
                }
                val avisos = lista.size - criticas
                actualizarContadores(criticas, avisos, lista.size)
            }
        }

        viewModel.mensajeError.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarContadores(criticas: Int, avisos: Int, total: Int) {
        binding.tvAlertasCriticas.text = criticas.toString()
        binding.tvAlertasAdvertencias.text = avisos.toString()
        binding.tvAlertasTotal.text = total.toString()
    }

    private val permisoNotificacionesLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(requireContext(), "Notificaciones habilitadas", Toast.LENGTH_SHORT).show()
            }
        }

    private fun solicitarPermisoNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permisoNotificacionesLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun tomarAccionAlerta(alerta: AlertaModel) {

        when (alerta.tipo) {

            "SOLICITUD_MANTENIMIENTO" -> {
                abrirSolicitudesMantenimiento()
            }

            "VENCIDO", "PROXIMO", "EN_PROCESO",
            "MANTENIMIENTO_PENDIENTE", "MANTENIMIENTO_VENCIDO" -> {

                if (alerta.uidMantenimiento.isBlank()) {
                    Toast.makeText(
                        requireContext(),
                        "No se encontró el mantenimiento relacionado",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                mantenimientoViewModel.obtenerMantenimientoPorUid(
                    alerta.uidMantenimiento
                ) { mantenimiento ->

                    abrirDetalleMantenimientoDesdeAlerta(
                        mantenimiento,
                        alerta
                    )
                }
            }

            "STOCK_AGOTADO", "STOCK_CRITICO",
            "STOCK_BAJO", "STOCK_MINIMO" -> {
                abrirDetalleRepuestoDesdeAlerta(alerta)
            }

            "ALTO_CONSUMO", "SIN_ROTACION",
            "MOVIMIENTO_ANORMAL" -> {
                abrirMovimientosDesdeAlerta(alerta)
            }

            else -> {
                Toast.makeText(
                    requireContext(),
                    alerta.mensaje,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun abrirSolicitudesMantenimiento() {

        val fragment = SolicitudesMantenimientoFragment()

        parentFragmentManager.beginTransaction()
            .replace(
                com.lingomak.lingomakapp.R.id.fragmentContainerAdmin,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }

    private fun abrirDetalleMantenimientoDesdeAlerta(m: MantenimientoModel, alerta: AlertaModel) {
        val fragment = DetalleMantenimientoFragment()
        val bundle = Bundle()
        bundle.putString("uid", m.uid)
        bundle.putString("origen", "ALERTA")
        bundle.putString("tituloAlerta", alerta.titulo)
        bundle.putString("mensajeAlerta", alerta.mensaje)
        fragment.arguments = bundle

        val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
            com.lingomak.lingomakapp.R.id.fragmentContainerAdmin else com.lingomak.lingomakapp.R.id.containerOperario

        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun abrirDetalleRepuestoDesdeAlerta(alerta: AlertaModel) {
        val fragment = DetalleRepuestoFragment()
        val bundle = Bundle()
        bundle.putString("uid", alerta.uidRepuesto)
        bundle.putString("origen", "ALERTA")
        bundle.putString("tituloAlerta", alerta.titulo)
        bundle.putString("mensajeAlerta", alerta.mensaje)
        fragment.arguments = bundle

        val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
            com.lingomak.lingomakapp.R.id.fragmentContainerAdmin else com.lingomak.lingomakapp.R.id.containerOperario

        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun abrirMovimientosDesdeAlerta(alerta: AlertaModel) {
        val fragment = InventarioContainerFragment()
        val bundle = Bundle()
        bundle.putInt("tab", 1)
        bundle.putString("nombreRepuesto", alerta.nombreRepuesto)
        bundle.putString("origen", "ALERTA")
        bundle.putString("tituloAlerta", alerta.titulo)
        bundle.putString("mensajeAlerta", alerta.mensaje)
        fragment.arguments = bundle

        val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
            com.lingomak.lingomakapp.R.id.fragmentContainerAdmin else com.lingomak.lingomakapp.R.id.containerOperario

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
