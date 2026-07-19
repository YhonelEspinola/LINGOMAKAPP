package com.lingomak.lingomakapp.ui.mantenimiento

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.FragmentDetalleMantenimientoBinding
import com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity
import com.lingomak.lingomakapp.utils.ImageOptimizer

class DetalleMantenimientoFragment : Fragment() {

    private var uidMaquinaria: String = ""

    private var _binding: FragmentDetalleMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MantenimientoViewModel by viewModels()
    private var uidMantenimiento: String = ""
    private var estadoActual: String = ""
    private var mantenimientoActual: MantenimientoModel? = null

    private var codigoMantenimiento: String = ""
    private var tipoMantenimiento: String = ""
    private var nombreMaquinaria: String = ""
    private var codigoMaquinaria: String = ""
    private var tipoMaquinaria: String = ""
    private var descripcion: String = ""
    private var fechaProgramada: String = ""
    private var responsable: String = ""
    private var prioridad: String = ""
    private var observaciones: String = ""
    private var horometroProgramado: Int = 0
    private var costoEstimado: Double = 0.0

    private var origen: String = ""
    private var tituloAlerta: String = ""
    private var mensajeAlerta: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleMantenimientoBinding.inflate(inflater, container, false)

        uidMantenimiento = arguments?.getString("uid") ?: ""
        origen = arguments?.getString("origen") ?: ""
        tituloAlerta = arguments?.getString("tituloAlerta") ?: ""
        mensajeAlerta = arguments?.getString("mensajeAlerta") ?: ""

        cargarDatos()
        configurarEventos()
        observarViewModel()

        return binding.root
    }

    private fun cargarDatos() {
        viewModel.obtenerMantenimientoPorUid(uidMantenimiento) { m ->
            mantenimientoActual = m
            uidMaquinaria = m.uidMaquinaria
            estadoActual = m.estado
            codigoMantenimiento = m.codigoMantenimiento
            tipoMantenimiento = m.tipoMantenimiento
            nombreMaquinaria = m.nombreMaquinaria
            codigoMaquinaria = m.codigoMaquinaria
            tipoMaquinaria = m.tipoMaquinaria
            descripcion = m.descripcion
            fechaProgramada = m.fechaProgramada
            responsable = m.responsable
            prioridad = m.prioridad
            observaciones = m.observaciones
            horometroProgramado = m.horometroProgramado
            costoEstimado = m.costoEstimado

            binding.tvCodigoDetalle.text = codigoMantenimiento
            binding.tvTipoDetalle.text = tipoMantenimiento
            binding.tvMaquinariaDetalle.text = nombreMaquinaria
            binding.tvCodigoMaquinariaDetalle.text = codigoMaquinaria
            binding.tvTipoMaquinariaDetalle.text = tipoMaquinaria
            binding.tvDescripcionDetalle.text = descripcion
            binding.tvFechaDetalle.text = fechaProgramada
            binding.tvHorometroDetalle.text = "$horometroProgramado h"
            binding.tvResponsableDetalle.text = responsable
            binding.tvPrioridadDetalle.text = prioridad
            binding.tvCostoEstimadoDetalle.text = "S/ $costoEstimado"
            binding.tvObservacionesDetalle.text = observaciones.ifEmpty { "Sin observaciones" }

            aplicarColorEstado(estadoActual)
            actualizarAccionesPorEstado()
            configurarImagenes()
            mostrarBannerAlerta()
        }
    }

    private fun configurarEventos() {
        binding.btnEditarMantenimiento.setOnClickListener { abrirEditarMantenimiento() }
        binding.btnCambiarEstado.setOnClickListener { 
            val isAdmin = requireActivity() is DashboardAdminActivity
            if (isAdmin) {
                mostrarDialogoCambiarEstado()
            } else {
                confirmarInicioMantenimiento()
            }
        }
        binding.btnFinalizarMantenimiento.setOnClickListener { abrirFinalizarMantenimiento() }
        
        binding.btnGenerarReporteIA.setOnClickListener {
            mantenimientoActual?.let { m ->
                if (m.reporteIA.isNotEmpty()) {
                    abrirDetalleReporteIA()
                } else {
                    viewModel.generarReporteIA(m)
                }
            }
        }
    }

    private fun confirmarInicioMantenimiento() {
        AlertDialog.Builder(requireContext())
            .setTitle("Iniciar Mantenimiento")
            .setMessage("¿Desea marcar este mantenimiento como EN PROCESO?")
            .setPositiveButton("Sí, iniciar") { _, _ ->
                viewModel.iniciarMantenimiento(uidMantenimiento, uidMaquinaria) {
                    cargarDatos()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoCambiarEstado() {
        val estados = arrayOf("PENDIENTE", "EN_PROCESO", "CANCELADO")
        AlertDialog.Builder(requireContext())
            .setTitle("Cambiar estado")
            .setItems(estados) { _, which ->
                val nuevoEstado = estados[which]
                if (nuevoEstado == "EN_PROCESO") {
                    viewModel.iniciarMantenimiento(uidMantenimiento, uidMaquinaria) {
                        cargarDatos()
                    }
                } else {
                    viewModel.cambiarEstadoMantenimiento(uidMantenimiento, nuevoEstado) {
                        cargarDatos()
                    }
                }
            }
            .show()
    }

    private fun abrirEditarMantenimiento() {
        val fragment = EditarMantenimientoFragment()
        val bundle = Bundle()
        bundle.putString("uid", uidMantenimiento)
        fragment.arguments = bundle
        
        val containerId = if (requireActivity() is DashboardAdminActivity) R.id.fragmentContainerAdmin else R.id.containerOperario
        
        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun aplicarColorEstado(estado: String) {
        binding.tvEstadoDetalle.text = estado
        val color = when (estado) {
            "PENDIENTE" -> R.color.warning
            "EN_PROCESO" -> R.color.primary
            "FINALIZADO" -> R.color.success
            else -> R.color.text_secondary
        }
        binding.tvEstadoDetalle.setTextColor(requireContext().getColor(color))
    }

    private fun actualizarAccionesPorEstado() {
        val isAdmin = requireActivity() is DashboardAdminActivity
        
        // El botón de IA solo para Admin y si está Finalizado
        if (isAdmin && estadoActual == "FINALIZADO") {
            binding.btnGenerarReporteIA.visibility = View.VISIBLE
        } else {
            binding.btnGenerarReporteIA.visibility = View.GONE
        }

        // Acciones generales por estado (para todos los roles autorizados)
        when (estadoActual) {
            "PENDIENTE" -> {
                binding.btnEditarMantenimiento.visibility = if (isAdmin) View.VISIBLE else View.GONE
                binding.btnCambiarEstado.visibility = View.VISIBLE
                binding.btnFinalizarMantenimiento.visibility = View.GONE
            }
            "EN_PROCESO" -> {
                binding.btnEditarMantenimiento.visibility = View.GONE
                binding.btnCambiarEstado.visibility = View.GONE
                binding.btnFinalizarMantenimiento.visibility = View.VISIBLE
            }
            else -> {
                binding.btnEditarMantenimiento.visibility = View.GONE
                binding.btnCambiarEstado.visibility = View.GONE
                binding.btnFinalizarMantenimiento.visibility = View.GONE
            }
        }
    }

    private fun configurarImagenes() {
        mantenimientoActual?.let { m ->
            if (m.imagenesReporte.isNotEmpty()) {
                binding.tvLabelImagenesReporte.visibility = View.VISIBLE
                binding.rvImagenesReporteDetalle.visibility = View.VISIBLE
                val adapter = EvidenciasReadOnlyAdapter(m.imagenesReporte) { url ->
                    mostrarImagenAmpliada(url)
                }
                binding.rvImagenesReporteDetalle.adapter = adapter
            }
            if (m.imagenesFinalizacion.isNotEmpty()) {
                binding.tvLabelImagenesFinal.visibility = View.VISIBLE
                binding.rvImagenesFinalDetalle.visibility = View.VISIBLE
                val adapter = EvidenciasReadOnlyAdapter(m.imagenesFinalizacion) { url ->
                    mostrarImagenAmpliada(url)
                }
                binding.rvImagenesFinalDetalle.adapter = adapter
            }
        }
    }

    private fun mostrarImagenAmpliada(url: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_imagen_ampliada, null)
        val imageView = dialogView.findViewById<android.widget.ImageView>(R.id.ivImagenAmpliada)
        
        com.bumptech.glide.Glide.with(this)
            .load(url)
            .into(imageView)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun observarViewModel() {
        viewModel.mensajeError.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }

        viewModel.reporteGenerado.observe(viewLifecycleOwner) { reporte ->
            if (reporte != null) {
                abrirDetalleReporteIA()
            }
        }

        viewModel.loadingAI.observe(viewLifecycleOwner) { loading ->
            // Se puede mostrar un progress si se desea
        }
    }

    private fun abrirDetalleReporteIA() {
        val fragment = DetalleReporteIAFragment()
        val bundle = Bundle()
        bundle.putString("uid", uidMantenimiento)
        fragment.arguments = bundle
        
        parentFragmentManager.beginTransaction()
            .replace((requireView().parent as ViewGroup).id, fragment)
            .addToBackStack(null)
            .commit()
            
        viewModel.limpiarEstadoAI()
    }

    private fun abrirFinalizarMantenimiento() {
        val fragment = FinalizarMantenimientoFragment()
        val bundle = Bundle()
        bundle.putString("uid", uidMantenimiento) // Corregido: FinalizarMantenimientoFragment espera "uid"
        fragment.arguments = bundle
        
        val containerId = if (requireActivity() is DashboardAdminActivity) R.id.fragmentContainerAdmin else R.id.containerOperario
        
        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun mostrarBannerAlerta() {
        if (origen == "NOTIFICACION" || origen == "ALERTA") {
            binding.cardAlerta.visibility = View.VISIBLE
            binding.tvTituloAlerta.text = tituloAlerta
            binding.tvTipoAlerta.text = tipoMantenimiento
            binding.tvMensajeAlerta.text = mensajeAlerta
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
