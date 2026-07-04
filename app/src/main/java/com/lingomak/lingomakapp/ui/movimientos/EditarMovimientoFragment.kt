package com.lingomak.lingomakapp.ui.movimientos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.databinding.FragmentEditarMovimientoBinding

class EditarMovimientoFragment : Fragment() {

    private var _binding: FragmentEditarMovimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosGlobalViewModel by viewModels()
    private var movimientoUid: String = ""
    private var movimientoOriginal: MovimientoModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditarMovimientoBinding.inflate(inflater, container, false)
        movimientoUid = arguments?.getString("movimientoUid") ?: ""
        
        setupUI()
        observarViewModel()
        
        return binding.root
    }

    private fun setupUI() {
        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.rgTipo.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbSalida) {
                binding.tvLabelDestino.visibility = View.VISIBLE
                binding.rgDestino.visibility = View.VISIBLE
            } else {
                binding.tvLabelDestino.visibility = View.GONE
                binding.rgDestino.visibility = View.GONE
            }
        }

        binding.btnActualizar.setOnClickListener {
            val original = movimientoOriginal ?: return@setOnClickListener
            val cantidadStr = binding.etCantidad.text.toString()
            
            if (cantidadStr.isEmpty()) {
                binding.etCantidad.error = "Campo obligatorio"
                return@setOnClickListener
            }
            
            val cantidad = cantidadStr.toIntOrNull() ?: 0
            if (cantidad <= 0) {
                binding.etCantidad.error = "La cantidad debe ser mayor a 0"
                return@setOnClickListener
            }
            
            val tipo = if (binding.rbEntrada.isChecked) "ENTRADA" else "SALIDA"
            val destinoSalida = if (tipo == "SALIDA") {
                when (binding.rgDestino.checkedRadioButtonId) {
                    R.id.rbConsumoInterno -> "CONSUMO_INTERNO"
                    R.id.rbDistribucionExterna -> "DISTRIBUCION_EXTERNA"
                    else -> {
                        Toast.makeText(requireContext(), "Seleccione el destino de la salida", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
            } else ""

            val actualizado = original.copy(
                tipo = tipo,
                cantidad = cantidad,
                observacion = binding.etObservacion.text.toString(),
                destinoSalida = destinoSalida
            )
            
            viewModel.actualizarMovimiento(actualizado, original.cantidad, original.tipo)
        }
    }

    private fun observarViewModel() {
        viewModel.movimientosFiltrados.observe(viewLifecycleOwner) { lista ->
            val pair = lista.find { it.first.uid == movimientoUid }
            if (pair != null && movimientoOriginal == null) {
                movimientoOriginal = pair.first
                binding.tvNombreRepuesto.text = "Producto: ${pair.second}"
                cargarDatos(pair.first)
            }
        }

        viewModel.registroExitoso.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(requireContext(), "Movimiento actualizado", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarDatos(movimiento: MovimientoModel) {
        if (movimiento.tipo == "ENTRADA") {
            binding.rbEntrada.isChecked = true
        } else {
            binding.rbSalida.isChecked = true
            binding.tvLabelDestino.visibility = View.VISIBLE
            binding.rgDestino.visibility = View.VISIBLE
            if (movimiento.destinoSalida == "CONSUMO_INTERNO") {
                binding.rbConsumoInterno.isChecked = true
            } else if (movimiento.destinoSalida == "DISTRIBUCION_EXTERNA") {
                binding.rbDistribucionExterna.isChecked = true
            }
        }
        binding.etCantidad.setText(movimiento.cantidad.toString())
        binding.etObservacion.setText(movimiento.observacion)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
