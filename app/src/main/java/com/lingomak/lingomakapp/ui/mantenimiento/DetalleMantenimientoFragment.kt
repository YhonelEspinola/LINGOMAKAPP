package com.lingomak.lingomakapp.ui.mantenimiento

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.databinding.FragmentDetalleMantenimientoBinding
import com.lingomak.lingomakapp.R

class DetalleMantenimientoFragment : Fragment() {

    private var uidMaquinaria = ""

    private var _binding: FragmentDetalleMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MantenimientoViewModel by viewModels()
    private var uidMantenimiento: String = ""
    private var estadoActual: String = ""

    private var codigoMantenimiento = ""
    private var tipoMantenimiento = ""
    private var nombreMaquinaria = ""
    private var codigoMaquinaria = ""
    private var tipoMaquinaria = ""
    private var descripcion = ""
    private var fechaProgramada = ""
    private var responsable = ""
    private var prioridad = ""
    private var observaciones = ""
    private var horometroProgramado = 0
    private var costoEstimado = 0.0

    private var origen = ""
    private var tituloAlerta = ""
    private var mensajeAlerta = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleMantenimientoBinding.inflate(inflater, container, false)

        cargarDatos()
        configurarEventos()
        observarViewModel()

        return binding.root
    }

    private fun cargarDatos() {
        uidMantenimiento = arguments?.getString("uid") ?: ""
        uidMaquinaria = arguments?.getString("uidMaquinaria") ?: ""

        codigoMantenimiento = arguments?.getString("codigoMantenimiento") ?: ""
        tipoMantenimiento = arguments?.getString("tipoMantenimiento") ?: ""
        nombreMaquinaria = arguments?.getString("nombreMaquinaria") ?: ""
        descripcion = arguments?.getString("descripcion") ?: ""
        fechaProgramada = arguments?.getString("fechaProgramada") ?: ""
        responsable = arguments?.getString("responsable") ?: ""
        horometroProgramado = arguments?.getInt("horometroProgramado") ?: 0
        estadoActual = arguments?.getString("estado") ?: ""
        codigoMaquinaria = arguments?.getString("codigoMaquinaria") ?: ""
        tipoMaquinaria = arguments?.getString("tipoMaquinaria") ?: ""
        prioridad = arguments?.getString("prioridad") ?: ""
        costoEstimado = arguments?.getDouble("costoEstimado") ?: 0.0
        observaciones = arguments?.getString("observaciones") ?: ""

        origen = arguments?.getString("origen") ?: ""
        tituloAlerta = arguments?.getString("tituloAlerta") ?: ""
        mensajeAlerta = arguments?.getString("mensajeAlerta") ?: ""

        binding.tvCodigoDetalle.text = codigoMantenimiento
        binding.tvTipoDetalle.text = tipoMantenimiento
        binding.tvMaquinariaDetalle.text = nombreMaquinaria
        binding.tvDescripcionDetalle.text = descripcion
        binding.tvFechaDetalle.text = fechaProgramada
        binding.tvResponsableDetalle.text = responsable
        binding.tvHorometroDetalle.text = "$horometroProgramado horas"
        binding.tvEstadoDetalle.text = estadoActual
        binding.tvCodigoMaquinariaDetalle.text = codigoMaquinaria
        binding.tvTipoMaquinariaDetalle.text = tipoMaquinaria
        binding.tvPrioridadDetalle.text = prioridad
        binding.tvCostoEstimadoDetalle.text = "S/ $costoEstimado"
        binding.tvObservacionesDetalle.text =
            observaciones.ifEmpty { "Sin observaciones" }

        actualizarAccionesPorEstado()

        mostrarBannerAlerta()
    }

    private fun configurarEventos() {
        binding.btnCambiarEstado.setOnClickListener {
            mostrarDialogoCambiarEstado()
        }
        binding.btnEditarMantenimiento.setOnClickListener {
            abrirEditarMantenimiento()
        }

        binding.btnFinalizarMantenimiento.setOnClickListener {
            abrirFinalizarMantenimiento()
        }
    }

    private fun mostrarDialogoCambiarEstado() {
        if (estadoActual != "PENDIENTE" && estadoActual != "VENCIDO") {

            Toast.makeText(
                requireContext(),
                "Solo los mantenimientos pendientes pueden iniciarse",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Iniciar mantenimiento")
            .setMessage(
                "¿Desea iniciar este mantenimiento?"
            )

            .setPositiveButton("Iniciar") { dialog, _ ->

                if (uidMaquinaria.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "No se encontró la maquinaria relacionada",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                viewModel.iniciarMantenimiento(
                    uidMantenimiento = uidMantenimiento,
                    uidMaquinaria = uidMaquinaria,
                    onSuccess = {
                        estadoActual = "EN_PROCESO"
                        binding.tvEstadoDetalle.text = "EN_PROCESO"
                        actualizarAccionesPorEstado()
                        Toast.makeText(
                            requireContext(),
                            "Mantenimiento iniciado correctamente",
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

    private fun abrirEditarMantenimiento() {

        val fragment = EditarMantenimientoFragment()

        val bundle = Bundle().apply {
            putString("uid", uidMantenimiento)
            putString("uidMaquinaria", uidMaquinaria)
            putString("codigoMantenimiento", codigoMantenimiento)

            putString("codigoMaquinaria", codigoMaquinaria)
            putString("nombreMaquinaria", nombreMaquinaria)
            putString("tipoMaquinaria", tipoMaquinaria)

            putString("tipoMantenimiento", tipoMantenimiento)
            putString("descripcion", descripcion)
            putString("fechaProgramada", fechaProgramada)

            putString("responsable", responsable)
            putString("estado", estadoActual)
            putString("prioridad", prioridad)

            putInt("horometroProgramado", horometroProgramado)
            putDouble("costoEstimado", costoEstimado)
            putString("observaciones", observaciones)
        }

        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun actualizarAccionesPorEstado() {

        when (estadoActual) {

            /*
             * PENDIENTE:
             * El administrador todavía puede editar,
             * iniciar o cancelar desde otras opciones.
             */
            "PENDIENTE" -> {
                binding.btnEditarMantenimiento.visibility = View.VISIBLE
                binding.btnCambiarEstado.visibility = View.VISIBLE
                binding.btnFinalizarMantenimiento.visibility = View.GONE

                binding.btnCambiarEstado.text = "Iniciar mantenimiento"
            }

            /*
             * VENCIDO:
             * Todavía no se realizó.
             * Por eso permitimos editar para reprogramar
             * o iniciar mantenimiento.
             */
            "VENCIDO" -> {
                binding.btnEditarMantenimiento.visibility = View.VISIBLE
                binding.btnCambiarEstado.visibility = View.VISIBLE
                binding.btnFinalizarMantenimiento.visibility = View.GONE

                binding.btnCambiarEstado.text = "Iniciar mantenimiento"
            }

            /*
             * EN_PROCESO:
             * Ya no se debe editar.
             * Aquí la acción correcta es finalizar.
             */
            "EN_PROCESO" -> {
                binding.btnEditarMantenimiento.visibility = View.GONE
                binding.btnCambiarEstado.visibility = View.GONE
                binding.btnFinalizarMantenimiento.visibility = View.VISIBLE
            }

            /*
             * FINALIZADO / CANCELADO:
             * Son estados históricos.
             * Solo se permite ver detalle.
             */
            "FINALIZADO", "CANCELADO" -> {
                binding.btnEditarMantenimiento.visibility = View.GONE
                binding.btnCambiarEstado.visibility = View.GONE
                binding.btnFinalizarMantenimiento.visibility = View.GONE
            }

            else -> {
                binding.btnEditarMantenimiento.visibility = View.GONE
                binding.btnCambiarEstado.visibility = View.GONE
                binding.btnFinalizarMantenimiento.visibility = View.GONE
            }
        }
    }

    private fun observarViewModel() {
        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirFinalizarMantenimiento() {

        /*
         * RN05:
         * Solo se puede finalizar un mantenimiento
         * que está en estado EN_PROCESO.
         */
        if (estadoActual != "EN_PROCESO") {
            Toast.makeText(
                requireContext(),
                "Solo se puede finalizar un mantenimiento en proceso",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val fragment = FinalizarMantenimientoFragment()

        val bundle = Bundle().apply {
            putString("uid", uidMantenimiento)
            putString("uidMaquinaria", uidMaquinaria)
            putString("codigoMantenimiento", codigoMantenimiento)
            putString("nombreMaquinaria", nombreMaquinaria)
            putString("descripcion", descripcion)
            putString("estado", estadoActual)
        }

        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun mostrarBannerAlerta() {
        if (origen == "ALERTA") {
            binding.cardAlerta.visibility = View.VISIBLE
            binding.tvTipoAlerta.text = tituloAlerta
            binding.tvMensajeAlerta.text = mensajeAlerta
        } else {
            binding.cardAlerta.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}