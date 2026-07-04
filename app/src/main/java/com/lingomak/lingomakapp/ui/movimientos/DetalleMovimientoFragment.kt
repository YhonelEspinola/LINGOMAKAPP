package com.lingomak.lingomakapp.ui.movimientos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentDetalleMovimientoBinding
import java.text.SimpleDateFormat
import java.util.*

class DetalleMovimientoFragment : Fragment() {

    private var _binding: FragmentDetalleMovimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosGlobalViewModel by viewModels()
    private var movimientoUid: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleMovimientoBinding.inflate(inflater, container, false)
        movimientoUid = arguments?.getString("movimientoUid") ?: ""
        
        setupUI()
        observarViewModel()
        
        return binding.root
    }

    private fun setupUI() {
        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }
        
        binding.btnEliminar.setOnClickListener {
            mostrarDialogoEliminar()
        }

        binding.btnEditar.setOnClickListener {
            val fragment = EditarMovimientoFragment()
            val bundle = Bundle().apply {
                putString("movimientoUid", movimientoUid)
            }
            fragment.arguments = bundle
            
            val containerId = if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity) 
                R.id.fragmentContainerAdmin else R.id.containerOperario
                
            parentFragmentManager.beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observarViewModel() {
        viewModel.movimientosFiltrados.observe(viewLifecycleOwner) { lista ->
            val pair = lista.find { it.first.uid == movimientoUid }
            if (pair != null) {
                pintarDetalle(pair.first, pair.second)
            }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun pintarDetalle(movimiento: com.lingomak.lingomakapp.data.model.MovimientoModel, nombreRepuesto: String) {
        binding.tvNombreRepuesto.text = nombreRepuesto
        binding.tvTipo.text = movimiento.tipo
        binding.tvCantidad.text = movimiento.cantidad.toString()
        
        val idProvisional = if (movimiento.registradoPor.length >= 6) {
            movimiento.registradoPor.substring(0, 6)
        } else {
            movimiento.registradoPor
        }
        binding.tvUsuario.text = "Responsable: $idProvisional"
        
        val df = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        binding.tvFecha.text = movimiento.fecha?.let { df.format(it) } ?: "---"

        if (movimiento.tipo == "SALIDA") {
            binding.separatorDestino.visibility = View.VISIBLE
            binding.labelDestino.visibility = View.VISIBLE
            binding.tvDestino.visibility = View.VISIBLE
            binding.tvDestino.text = when(movimiento.destinoSalida) {
                "CONSUMO_INTERNO" -> "Consumo interno (reparación)"
                "DISTRIBUCION_EXTERNA" -> "Distribución externa (venta)"
                else -> "No especificado"
            }
        } else {
            binding.separatorDestino.visibility = View.GONE
            binding.labelDestino.visibility = View.GONE
            binding.tvDestino.visibility = View.GONE
        }

        if (movimiento.observacion.isNotEmpty()) {
            binding.separatorObservacion.visibility = View.VISIBLE
            binding.labelObservacion.visibility = View.VISIBLE
            binding.tvObservacion.visibility = View.VISIBLE
            binding.tvObservacion.text = movimiento.observacion
        } else {
            binding.separatorObservacion.visibility = View.GONE
            binding.labelObservacion.visibility = View.GONE
            binding.tvObservacion.visibility = View.GONE
        }
    }

    private fun mostrarDialogoEliminar() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar eliminación")
            .setMessage("¿Eliminar este movimiento? Esta acción revertirá el ajuste de stock que generó.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarMovimiento(movimientoUid)
                parentFragmentManager.popBackStack()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
