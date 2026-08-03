package com.lingomak.lingomakapp.ui.mantenimiento

import android.os.Bundle
import android.util.Log
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

        setupUI()
        observarMantenimiento()
        configurarEventos()

        return binding.root
    }

    private fun setupUI() {
        // Inicialmente ocultamos botones que dependen del estado
        binding.btnEditarMantenimiento.visibility = View.GONE
        binding.btnCambiarEstado.visibility = View.GONE
        binding.btnCancelarMantenimiento.visibility = View.GONE
        binding.btnSolicitarReprogramacion.visibility = View.GONE
        binding.btnFinalizarMantenimiento.visibility = View.GONE
        binding.btnGenerarReporteIA.visibility = View.GONE
    }

    private fun observarMantenimiento() {
        if (uidMantenimiento.isNotEmpty()) {
            viewModel.obtenerMantenimientoPorUidObservable(uidMantenimiento).observe(viewLifecycleOwner) { m ->
                m?.let {
                    mantenimientoActual = it
                    pintarDatos(it)
                }
            }
        }
        
        viewModel.mensajeError.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun pintarDatos(m: MantenimientoModel) {
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

        if (m.modificadoPorUid != null) {
            binding.separatorAuditoria.visibility = View.VISIBLE
            binding.tvAuditoria.visibility = View.VISIBLE
            binding.tvAuditoria.text = "Última modificación: ${m.fechaUltimaModificacion} — por ${m.modificadoPorNombre}"
        } else {
            binding.separatorAuditoria.visibility = View.GONE
            binding.tvAuditoria.visibility = View.GONE
        }

        // Pestaña Resolutor
        if (m.estado == "FINALIZADO") {
            binding.separatorResolutor.visibility = View.VISIBLE
            binding.tvResolutorDetalle.visibility = View.VISIBLE
            binding.tvResolutorDetalle.text = if (m.resolutorNombre.isNotBlank()) m.resolutorNombre else "No registrado"
        } else {
            binding.separatorResolutor.visibility = View.GONE
            binding.tvResolutorDetalle.visibility = View.GONE
        }

        aplicarColorEstado(estadoActual)
        actualizarAccionesPorEstado()
        configurarImagenes()
        mostrarBannerAlerta()
    }

    private fun configurarEventos() {
        binding.btnEditarMantenimiento.setOnClickListener { abrirEditarMantenimiento() }
        binding.btnCambiarEstado.setOnClickListener { 
            when (estadoActual) {
                "PENDIENTE" -> confirmarInicioMantenimiento()
                "VENCIDO" -> mostrarDatePickerReprogramar()
            }
        }
        binding.btnFinalizarMantenimiento.setOnClickListener { abrirFinalizarMantenimiento() }
        binding.btnCancelarMantenimiento.setOnClickListener { mostrarDialogoCancelarMantenimiento() }
        binding.btnSolicitarReprogramacion.setOnClickListener { solicitarReprogramacion() }
        
        binding.btnGenerarReporteIA.setOnClickListener {
            abrirDetalleReporteIA()
        }
    }

    private fun mostrarDialogoCancelarMantenimiento() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancelar Mantenimiento")
            .setMessage("¿Estás seguro de cancelar este mantenimiento?")
            .setPositiveButton("Sí, cancelar") { _, _ ->
                viewModel.cancelarMantenimiento(uidMantenimiento) {
                    Toast.makeText(requireContext(), "Mantenimiento cancelado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun mostrarDatePickerReprogramar() {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val nuevaFecha = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                viewModel.reprogramarMantenimiento(uidMantenimiento, nuevaFecha) {
                    Toast.makeText(requireContext(), "Mantenimiento reprogramado para $nuevaFecha", Toast.LENGTH_SHORT).show()
                }
            },
            year, month, day
        )

        // Restricción: Solo fechas estrictamente mayores a hoy (Fix 4.4)
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    private fun confirmarInicioMantenimiento() {
        AlertDialog.Builder(requireContext())
            .setTitle("Iniciar Mantenimiento")
            .setMessage("¿Desea marcar este mantenimiento como EN PROCESO?")
            .setPositiveButton("Sí, iniciar") { _, _ ->
                viewModel.iniciarMantenimiento(uidMantenimiento, uidMaquinaria) {
                    Toast.makeText(requireContext(), "Mantenimiento iniciado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun solicitarReprogramacion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Solicitar Reprogramación")
            .setMessage("Se enviará una solicitud al administrador para reprogramar este mantenimiento vencido. ¿Desea continuar?")
            .setPositiveButton("Enviar solicitud") { _, _ ->
                viewModel.solicitarReprogramacion(uidMantenimiento) {
                    Toast.makeText(requireContext(), "Solicitud enviada al administrador", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cerrar", null)
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

        // Acciones generales por estado (Fix 4.4)
        when (estadoActual) {
            "PENDIENTE" -> {
                binding.btnEditarMantenimiento.visibility = if (isAdmin) View.VISIBLE else View.GONE
                binding.btnCambiarEstado.visibility = View.VISIBLE
                binding.btnCambiarEstado.text = "INICIAR MANTENIMIENTO"
                binding.btnCancelarMantenimiento.visibility = if (isAdmin) View.VISIBLE else View.GONE
                binding.btnFinalizarMantenimiento.visibility = View.GONE
            }
            "VENCIDO" -> {
                binding.btnEditarMantenimiento.visibility = if (isAdmin) View.VISIBLE else View.GONE
                binding.btnCambiarEstado.visibility = if (isAdmin) View.VISIBLE else View.GONE
                binding.btnCambiarEstado.text = "REPROGRAMAR MANTENIMIENTO"
                binding.btnCancelarMantenimiento.visibility = if (isAdmin) View.VISIBLE else View.GONE
                binding.btnSolicitarReprogramacion.visibility = if (!isAdmin) View.VISIBLE else View.GONE
                binding.btnFinalizarMantenimiento.visibility = View.GONE
            }
            "EN_PROCESO" -> {
                binding.btnEditarMantenimiento.visibility = View.GONE
                binding.btnCambiarEstado.visibility = View.GONE
                binding.btnCancelarMantenimiento.visibility = View.GONE
                binding.btnFinalizarMantenimiento.visibility = View.VISIBLE
            }
            else -> {
                // CANCELADO o FINALIZADO
                binding.btnEditarMantenimiento.visibility = View.GONE
                binding.btnCambiarEstado.visibility = View.GONE
                binding.btnCancelarMantenimiento.visibility = View.GONE
                binding.btnFinalizarMantenimiento.visibility = View.GONE
            }
        }
    }

    private fun configurarImagenes() {
        mantenimientoActual?.let { m ->
            Log.d("DetalleMantenimiento", "Reporte: ${m.imagenesReporte.size} rem / ${m.imagenesReporteLocal.size} loc. Final: ${m.imagenesFinalizacion.size} rem / ${m.imagenesFinalizacionLocal.size} loc")
            
            val todasReporte = m.imagenesReporte + m.imagenesReporteLocal
            if (todasReporte.isNotEmpty()) {
                binding.rvImagenesReporteDetalle.visibility = View.VISIBLE
                val adapter = EvidenciasReadOnlyAdapter(todasReporte) { url ->
                    mostrarImagenAmpliada(url)
                }
                binding.rvImagenesReporteDetalle.adapter = adapter
            } else {
                binding.rvImagenesReporteDetalle.visibility = View.GONE
            }
            
            val todasFinal = m.imagenesFinalizacion + m.imagenesFinalizacionLocal
            if (todasFinal.isNotEmpty()) {
                binding.rvImagenesFinalDetalle.visibility = View.VISIBLE
                val adapter = EvidenciasReadOnlyAdapter(todasFinal) { url ->
                    mostrarImagenAmpliada(url)
                }
                binding.rvImagenesFinalDetalle.adapter = adapter
            } else {
                binding.rvImagenesFinalDetalle.visibility = View.GONE
            }
        }
    }

    private fun mostrarImagenAmpliada(url: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_imagen_ampliada, null)
        val imageView = dialogView.findViewById<android.widget.ImageView>(R.id.ivImagenAmpliada)
        
        com.bumptech.glide.Glide.with(this)
            .load(if (url.startsWith("http")) url else java.io.File(url))
            .into(imageView)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .show()
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
        bundle.putString("uid", uidMantenimiento)
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
