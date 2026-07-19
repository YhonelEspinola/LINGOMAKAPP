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
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.FragmentAlertasBinding
import com.lingomak.lingomakapp.ui.mantenimiento.DetalleMantenimientoFragment
import com.lingomak.lingomakapp.ui.mantenimiento.MantenimientoViewModel
import com.lingomak.lingomakapp.ui.repuestos.DetalleRepuestoFragment
import com.lingomak.lingomakapp.ui.repuestos.MovimientosFragment

class AlertasFragment : Fragment() {

    private val mantenimientoViewModel: MantenimientoViewModel by viewModels()
    private var _binding: FragmentAlertasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlertasViewModel by viewModels()

    private lateinit var adapter: AlertasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertasBinding.inflate(inflater, container, false)

        configurarRecyclerView()
        observarViewModel()
        solicitarPermisoNotificaciones()

        return binding.root
    }

    private fun configurarRecyclerView() {
        adapter = AlertasAdapter(emptyList()) { alerta ->
            tomarAccionAlerta(alerta)
        }
        binding.rvAlertas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAlertas.adapter = adapter
    }

    private fun observarViewModel() {
        viewModel.listaAlertas.observe(viewLifecycleOwner) { lista ->
            if (lista.isEmpty()) {
                binding.rvAlertas.visibility = View.GONE
                // Si tienes un layout para vacio, úsalo aquí
            } else {
                binding.rvAlertas.visibility = View.VISIBLE
                adapter.actualizarLista(lista)
            }
        }

        viewModel.mensajeError.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
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
            "MANTENIMIENTO_PENDIENTE", "MANTENIMIENTO_VENCIDO" -> {
                mantenimientoViewModel.obtenerMantenimientoPorUid(alerta.uidMantenimiento) { mantenimiento ->
                    abrirDetalleMantenimientoDesdeAlerta(mantenimiento, alerta)
                }
            }
            "STOCK_CRITICO", "STOCK_MINIMO" -> {
                abrirDetalleRepuestoDesdeAlerta(alerta)
            }
            "MOVIMIENTO_ANORMAL" -> {
                abrirMovimientosDesdeAlerta(alerta)
            }
            else -> {
                Toast.makeText(requireContext(), alerta.mensaje, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirDetalleMantenimientoDesdeAlerta(m: MantenimientoModel, alerta: AlertaModel) {
        val fragment = DetalleMantenimientoFragment()
        val bundle = Bundle()
        bundle.putString("uid", m.uid)
        bundle.putString("origen", "ALERTA")
        bundle.putString("tituloAlerta", alerta.titulo)
        bundle.putString("mensajeAlerta", alerta.mensaje)
        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(com.lingomak.lingomakapp.R.id.fragmentContainerAdmin, fragment)
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

        parentFragmentManager.beginTransaction()
            .replace(com.lingomak.lingomakapp.R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun abrirMovimientosDesdeAlerta(alerta: AlertaModel) {
        val fragment = MovimientosFragment()
        val bundle = Bundle()
        bundle.putString("repuestoUid", alerta.uidRepuesto)
        bundle.putString("origen", "ALERTA")
        bundle.putString("tituloAlerta", alerta.titulo)
        bundle.putString("mensajeAlerta", alerta.mensaje)
        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(com.lingomak.lingomakapp.R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
