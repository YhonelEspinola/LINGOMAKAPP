package com.lingomak.lingomakapp.ui.mantenimiento

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel
import com.lingomak.lingomakapp.databinding.FragmentSolicitudesMantenimientoBinding

class SolicitudesMantenimientoFragment : Fragment() {


    private var _binding: FragmentSolicitudesMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SolicitudMantenimientoViewModel by viewModels()

    private lateinit var adapter: SolicitudMantenimientoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSolicitudesMantenimientoBinding.inflate(
            inflater,
            container,
            false
        )

        configurarRecyclerView()
        observarViewModel()

        return binding.root
    }

    private fun configurarRecyclerView() {

        adapter = SolicitudMantenimientoAdapter(
            listaSolicitudes = emptyList(),

            onRevisarClick = { solicitud ->
                revisarSolicitud(solicitud)
            },

            onRechazarClick = { solicitud ->
                mostrarDialogoRechazo(solicitud)
            }
        )

        binding.rvSolicitudesMantenimiento.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvSolicitudesMantenimiento.adapter = adapter
    }

    private fun observarViewModel() {

        viewModel.solicitudesPendientes.observe(
            viewLifecycleOwner
        ) { lista ->

            adapter.actualizarLista(lista)

            binding.tvTotalSolicitudes.text =
                lista.size.toString()

            val sinSolicitudes = lista.isEmpty()

            binding.layoutSinSolicitudes.visibility =
                if (sinSolicitudes) View.VISIBLE else View.GONE

            binding.rvSolicitudesMantenimiento.visibility =
                if (sinSolicitudes) View.GONE else View.VISIBLE
        }

        viewModel.cargando.observe(
            viewLifecycleOwner
        ) { cargando ->

            binding.progressSolicitudes.visibility =
                if (cargando) View.VISIBLE else View.GONE
        }

        viewModel.mensajeError.observe(
            viewLifecycleOwner
        ) { mensaje ->

            if (!mensaje.isNullOrBlank()) {
                Toast.makeText(
                    requireContext(),
                    mensaje,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.solicitudRechazada.observe(
            viewLifecycleOwner
        ) { rechazada ->

            if (rechazada == true) {

                Toast.makeText(
                    requireContext(),
                    "Solicitud rechazada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.limpiarEstadoRechazo()
            }
        }
    }

    private fun revisarSolicitud(
        solicitud: SolicitudMantenimientoModel
    ) {

        val fragment = ProgramarMantenimientoFragment()

        val bundle = Bundle().apply {

            putString(
                "uidSolicitud",
                solicitud.uid
            )

            putString(
                "origen",
                "SOLICITUD_MANTENIMIENTO"
            )


            putString(
                "uidMaquinaria",
                solicitud.uidMaquinaria
            )

            putString(
                "codigoMaquinaria",
                solicitud.codigoMaquinaria
            )

            putString(
                "nombreMaquinaria",
                solicitud.nombreMaquinaria
            )

            putString(
                "tipoMaquinaria",
                solicitud.tipoMaquinaria
            )

            putDouble(
                "horometroActual",
                solicitud.horometroActual
            )

            putDouble(
                "horometroProgramado",
                solicitud.horometroActual
            )


            putString(
                "motivoSolicitud",
                solicitud.motivo
            )

            putString(
                "fechaSugerida",
                solicitud.fechaSugerida
            )

            // Fix 4.5: Pasar datos para reprogramación si aplica
            putString("origenSolicitud", solicitud.origen)
            putString("uidMantenimientoOriginal", solicitud.uidMantenimientoGenerado)
        }

        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainerAdmin,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }


    private fun mostrarDialogoRechazo(
        solicitud: SolicitudMantenimientoModel
    ) {

        val inputMotivo = EditText(requireContext()).apply {
            hint = "Ingrese el motivo del rechazo"
            minLines = 3
            maxLines = 5
            setPadding(40, 24, 40, 24)
        }

        val dialogo = AlertDialog.Builder(requireContext())
            .setTitle("Rechazar solicitud")
            .setMessage(
                "Explique por qué no se programará el mantenimiento " +
                        "de ${solicitud.nombreMaquinaria}."
            )
            .setView(inputMotivo)
            .setNegativeButton("Cancelar", null)

            .setPositiveButton("Rechazar", null)
            .create()

        dialogo.setOnShowListener {

            val botonRechazar =
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE)

            botonRechazar.setOnClickListener {

                val motivo =
                    inputMotivo.text.toString().trim()

                if (motivo.isBlank()) {

                    inputMotivo.error =
                        "El motivo es obligatorio"

                    return@setOnClickListener
                }

                val uidAdministrador =
                    FirebaseAuth.getInstance()
                        .currentUser
                        ?.uid
                        .orEmpty()

                if (uidAdministrador.isBlank()) {

                    Toast.makeText(
                        requireContext(),
                        "No se encontró la sesión del administrador",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                viewModel.rechazarSolicitud(
                    uidSolicitud = solicitud.uid,
                    uidAdministrador = uidAdministrador,
                    motivoRechazo = motivo
                )

                dialogo.dismiss()
            }
        }

        dialogo.show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.listarSolicitudesPendientes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}