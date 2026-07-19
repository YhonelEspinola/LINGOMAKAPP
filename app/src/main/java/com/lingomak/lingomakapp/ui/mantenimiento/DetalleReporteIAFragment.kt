package com.lingomak.lingomakapp.ui.mantenimiento

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.FragmentDetalleReporteIaBinding

class DetalleReporteIAFragment : Fragment() {

    private var _binding: FragmentDetalleReporteIaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MantenimientoViewModel by viewModels()
    private var uidMantenimiento = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleReporteIaBinding.inflate(inflater, container, false)
        uidMantenimiento = arguments?.getString("uid") ?: ""

        configurarToolbar()
        observarViewModel()
        cargarDatos()

        return binding.root
    }

    private fun configurarToolbar() {
        binding.toolbarReporteIA.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun cargarDatos() {
        viewModel.obtenerMantenimientoPorUid(uidMantenimiento) { m ->
            if (m.reporteIA.isNotEmpty()) {
                binding.tvCuerpoReporte.text = m.reporteIA
                binding.btnGuardarReporteIA.visibility = View.GONE
            } else {
                // Si llegamos aquí y no hay reporte, es que se acaba de generar en el ViewModel
                // O se disparó la generación desde el Fragment anterior
            }
        }
    }

    private fun observarViewModel() {
        viewModel.reporteGenerado.observe(viewLifecycleOwner) { reporte ->
            if (reporte != null) {
                binding.tvCuerpoReporte.text = reporte
                binding.btnGuardarReporteIA.visibility = View.VISIBLE
                binding.btnGuardarReporteIA.setOnClickListener {
                    viewModel.guardarReporte(uidMantenimiento, reporte)
                    Toast.makeText(requireContext(), "Reporte guardado", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }

        viewModel.loadingAI.observe(viewLifecycleOwner) { estaCargando ->
            binding.progressCargandoReporte.visibility = if (estaCargando) View.VISIBLE else View.GONE
        }

        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje != null && mensaje.isNotEmpty()) {
                if (binding.tvCuerpoReporte.text.toString().contains("Generando")) {
                    binding.tvCuerpoReporte.text = mensaje
                }
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.limpiarEstadoAI()
        _binding = null
    }
}
