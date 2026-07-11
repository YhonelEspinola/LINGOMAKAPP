package com.lingomak.lingomakapp.ui.alertas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.databinding.FragmentAlertasBinding
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.lingomak.lingomakapp.ui.repuestos.DetalleRepuestoFragment
import com.lingomak.lingomakapp.ui.mantenimiento.MantenimientoViewModel
import com.lingomak.lingomakapp.ui.mantenimiento.DetalleMantenimientoFragment
import com.lingomak.lingomakapp.ui.mantenimiento.SolicitudesMantenimientoFragment
import com.lingomak.lingomakapp.R
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

        viewModel.listarAlertas()

        return binding.root
    }

    private fun configurarRecyclerView() {
        adapter = AlertasAdapter(
            listaAlertas = emptyList(),
            onTomarAccionClick = { alerta ->
                tomarAccionAlerta(alerta)
            }
        )

        binding.rvAlertas.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvAlertas.adapter = adapter
    }

    private fun observarViewModel() {

        viewModel.listaAlertas.observe(viewLifecycleOwner) { lista ->

            adapter.actualizarLista(lista)

            binding.tvAlertasCriticas.text =
                lista.count { alerta ->
                    alerta.prioridad == "ALTA"
                }.toString()

            binding.tvAlertasAdvertencias.text =
                lista.count { alerta ->
                    alerta.prioridad == "MEDIA"
                }.toString()

            binding.tvAlertasTotal.text =
                lista.size.toString()

            binding.tvSinAlertas.visibility =
                if (lista.isEmpty()) View.VISIBLE else View.GONE



        }

        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private val permisoNotificacionesLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { permitido ->

            if (permitido) {
                Toast.makeText(
                    requireContext(),
                    "Notificaciones activadas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun solicitarPermisoNotificaciones() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permisoConcedido =
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (!permisoConcedido) {
                permisoNotificacionesLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun tomarAccionAlerta(alerta: com.lingomak.lingomakapp.data.model.AlertaModel) {

        when (alerta.tipo) {

            "VENCIDO",
            "PROXIMO",
            "EN_PROCESO" -> {

                /*
                 * Buscamos el mantenimiento completo usando el uid
                 * que viene dentro de la alerta.
                 */
                mantenimientoViewModel.obtenerMantenimientoPorUid(
                    uid = alerta.uidMantenimiento,
                    onSuccess = { mantenimiento ->

                        /*
                         * Cuando encontramos el mantenimiento,
                         * abrimos su detalle.
                         */
                        abrirDetalleMantenimientoDesdeAlerta(mantenimiento,alerta)
                    }
                )
            }

            "STOCK_AGOTADO",
            "STOCK_CRITICO",
            "STOCK_BAJO",
            "SIN_ROTACION" -> {

                abrirDetalleRepuestoDesdeAlerta(alerta)
            }

            "ALTO_CONSUMO" -> {
                abrirMovimientosDesdeAlerta(alerta)
            }

            "SOLICITUD_MANTENIMIENTO" -> {

                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragmentContainerAdmin,
                        SolicitudesMantenimientoFragment()
                    )
                    .addToBackStack(null)
                    .commit()
            }

            else -> {
                Toast.makeText(
                    requireContext(),
                    "No hay acción disponible para esta alerta",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun abrirDetalleMantenimientoDesdeAlerta(
        mantenimiento: com.lingomak.lingomakapp.data.model.MantenimientoModel,
        alerta : com.lingomak.lingomakapp.data.model.AlertaModel
    ) {

        val fragment = DetalleMantenimientoFragment()

        val bundle = Bundle().apply {
            putString("uid", mantenimiento.uid)
            putString("uidMaquinaria", mantenimiento.uidMaquinaria)
            putString("codigoMantenimiento", mantenimiento.codigoMantenimiento)
            putString("tipoMantenimiento", mantenimiento.tipoMantenimiento)
            putString("nombreMaquinaria", mantenimiento.nombreMaquinaria)
            putString("codigoMaquinaria", mantenimiento.codigoMaquinaria)
            putString("tipoMaquinaria", mantenimiento.tipoMaquinaria)
            putString("descripcion", mantenimiento.descripcion)
            putString("fechaProgramada", mantenimiento.fechaProgramada)
            putString("responsable", mantenimiento.responsable)
            putInt("horometroProgramado", mantenimiento.horometroProgramado)
            putString("estado", mantenimiento.estado)
            putString("prioridad", mantenimiento.prioridad)
            putDouble("costoEstimado", mantenimiento.costoEstimado)
            putString("observaciones", mantenimiento.observaciones)

            putString("origen", "ALERTA")
            putString("tipoAlerta", alerta.tipo)
            putString("tituloAlerta", alerta.titulo)
            putString("mensajeAlerta", alerta.mensaje)
        }

        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun abrirDetalleRepuestoDesdeAlerta(
        alerta: com.lingomak.lingomakapp.data.model.AlertaModel
    ) {


        val fragment = DetalleRepuestoFragment()

        val bundle = Bundle().apply {
            putString("uid", alerta.uidRepuesto)

            putString("origen", "ALERTA")
            putString("tipoAlerta", alerta.tipo)
            putString("tituloAlerta", alerta.titulo)
            putString("mensajeAlerta", alerta.mensaje)
        }

        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun abrirMovimientosDesdeAlerta(
        alerta: com.lingomak.lingomakapp.data.model.AlertaModel
    ) {

        val fragment = MovimientosFragment()

        val bundle = Bundle().apply {
            putString("filtroTexto", alerta.nombreRepuesto)

            putString("origen", "ALERTA")
            putString("tipoAlerta", alerta.tipo)
            putString("tituloAlerta", alerta.titulo)
            putString("mensajeAlerta", alerta.mensaje)
        }

        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
