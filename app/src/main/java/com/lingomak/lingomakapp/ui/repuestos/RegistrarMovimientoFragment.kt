package com.lingomak.lingomakapp.ui.repuestos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.databinding.FragmentRegistrarMovimientoBinding
import java.util.Date
import java.util.UUID

class RegistrarMovimientoFragment : Fragment() {

    private var _binding: FragmentRegistrarMovimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosViewModel by viewModels()
    private var repuestoUid: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrarMovimientoBinding.inflate(inflater, container, false)
        repuestoUid = arguments?.getString("repuestoUid") ?: ""
        
        viewModel.setRepuestoUid(repuestoUid)
        
        setupUI()
        observarViewModel()
        
        return binding.root
    }

    private fun setupUI() {
        binding.rgTipo.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbSalida) {
                binding.tvLabelDestino.visibility = View.VISIBLE
                binding.rgDestino.visibility = View.VISIBLE
            } else {
                binding.tvLabelDestino.visibility = View.GONE
                binding.rgDestino.visibility = View.GONE
                binding.rgDestino.clearCheck()
            }
        }

        binding.btnGuardar.setOnClickListener {
            val cantidadStr = binding.etCantidad.text.toString()
            val observacion = binding.etObservacion.text.toString()
            
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

            val registradoPor = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            
            val movimiento = MovimientoModel(
                uid = UUID.randomUUID().toString(),
                repuestoUid = repuestoUid,
                tipo = tipo,
                cantidad = cantidad,
                fecha = Date(),
                registradoPor = registradoPor,
                observacion = observacion,
                destinoSalida = destinoSalida
            )
            
            viewModel.registrarMovimiento(movimiento)
        }
    }

    private fun observarViewModel() {
        // Observar repuesto para activar el LiveData y asegurar que los datos estén cargados para la validación
        viewModel.repuesto.observe(viewLifecycleOwner) { repuesto ->
            // Simplemente observando se activa el LiveData en el ViewModel
        }

        viewModel.registroExitoso.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(requireContext(), "Movimiento registrado", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
