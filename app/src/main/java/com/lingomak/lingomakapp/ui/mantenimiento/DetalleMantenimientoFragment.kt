package com.lingomak.lingomakapp.ui.mantenimiento

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lingomak.lingomakapp.databinding.FragmentDetalleMantenimientoBinding

class DetalleMantenimientoFragment : Fragment() {

    private var _binding: FragmentDetalleMantenimientoBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDetalleMantenimientoBinding.inflate(inflater, container, false)

        cargarDatos()

        return binding.root
    }

    private fun cargarDatos() {

        val codigo = arguments?.getString("codigoMantenimiento") ?: ""
        val tipo = arguments?.getString("tipoMantenimiento") ?: ""
        val maquinaria = arguments?.getString("nombreMaquinaria") ?: ""
        val descripcion = arguments?.getString("descripcion") ?: ""
        val fecha = arguments?.getString("fechaProgramada") ?: ""
        val responsable = arguments?.getString("responsable") ?: ""
        val horometro = arguments?.getInt("horometroProgramado") ?: 0
        val estado = arguments?.getString("estado") ?: ""

        binding.tvCodigoDetalle.text = codigo
        binding.tvTipoDetalle.text = tipo
        binding.tvMaquinariaDetalle.text = maquinaria
        binding.tvDescripcionDetalle.text = descripcion
        binding.tvFechaDetalle.text = fecha
        binding.tvResponsableDetalle.text = responsable
        binding.tvHorometroDetalle.text = "$horometro horas"
        binding.tvEstadoDetalle.text = estado
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}