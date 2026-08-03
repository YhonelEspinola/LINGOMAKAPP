package com.lingomak.lingomakapp.ui.movimientos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.databinding.FragmentRegistrarMovimientoGlobalBinding
import java.util.*

class RegistrarMovimientoGlobalFragment : Fragment() {

    private var _binding: FragmentRegistrarMovimientoGlobalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosGlobalViewModel by viewModels()
    private var listaRepuestos: List<RepuestoModel> = emptyList()
    private var repuestoSeleccionado: RepuestoModel? = null
    private var uidEscaneado: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrarMovimientoGlobalBinding.inflate(inflater, container, false)
        uidEscaneado = arguments?.getString("repuestoUidScanned")
        
        setupUI()
        observarViewModel()
        
        return binding.root
    }

    private fun setupUI() {
        binding.rgTipo.setOnCheckedChangeListener { _, checkedId ->
            // Se quitó la selección manual de destino
        }

        binding.autoCompleteRepuesto.setOnItemClickListener { parent, _, position, _ ->
            val nombreSeleccionado = parent.getItemAtPosition(position) as String
            // Encontrar el repuesto real por nombre o código (el adapter muestra nombres)
            repuestoSeleccionado = listaRepuestos.find { 
                val display = "${it.codigoInterno} - ${it.nombre}"
                display == nombreSeleccionado
            }
            actualizarUICardRepuesto()
        }

        binding.btnGuardar.setOnClickListener {
            val repuesto = repuestoSeleccionado
            // ... validaciones existentes
            if (repuesto == null) {
                Toast.makeText(requireContext(), "Seleccione un producto válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
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
            val destinoSalida = if (tipo == "SALIDA") "DISTRIBUCION_EXTERNA" else ""

            val registradoPor = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            val movimiento = MovimientoModel(
                uid = UUID.randomUUID().toString(),
                repuestoUid = repuesto.uid,
                tipo = tipo,
                cantidad = cantidad,
                fecha = Date(),
                registradoPor = registradoPor,
                observacion = binding.etObservacion.text.toString(),
                destinoSalida = destinoSalida
            )

            viewModel.registrarMovimiento(movimiento)
        }
    }

    private fun actualizarUICardRepuesto() {
        val repuesto = repuestoSeleccionado
        if (repuesto != null) {
            binding.includeRepuestoCard.cardRepuestoDetalle.visibility = View.VISIBLE
            
            binding.includeRepuestoCard.tvNombreCard.text = repuesto.nombre
            binding.includeRepuestoCard.tvCodigoMarcaCard.text = "${repuesto.codigoInterno} | ${repuesto.marca}"
            binding.includeRepuestoCard.tvCategoriaCard.text = repuesto.categoria
            binding.includeRepuestoCard.tvEstadoCard.text = repuesto.estado
            
            val colorEstado = if (repuesto.estado == "ACTIVO") R.color.success else R.color.danger
            binding.includeRepuestoCard.tvEstadoCard.setTextColor(ContextCompat.getColor(requireContext(), colorEstado))
            
            binding.includeRepuestoCard.tvStockActualCard.text = repuesto.stockActual.toString()
            binding.includeRepuestoCard.tvStockMinimoCard.text = repuesto.stockMinimo.toString()
            binding.includeRepuestoCard.tvStockMaximoCard.text = repuesto.stockMaximo.toString()
            binding.includeRepuestoCard.tvUbicacionCard.text = repuesto.ubicacionAlmacen.ifEmpty { "No especificada" }

            if (repuesto.imagenUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(repuesto.imagenUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .into(binding.includeRepuestoCard.ivImagenRepuesto)
            } else {
                binding.includeRepuestoCard.ivImagenRepuesto.setImageResource(R.drawable.bg_image_placeholder)
            }
        } else {
            binding.includeRepuestoCard.cardRepuestoDetalle.visibility = View.GONE
        }
    }

    private fun observarViewModel() {
        viewModel.todosLosRepuestos.observe(viewLifecycleOwner) { repuestos ->
            listaRepuestos = repuestos.filter { it.estado == "ACTIVO" }
            
            val sugerencias = listaRepuestos.map { "${it.codigoInterno} - ${it.nombre}" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sugerencias)
            binding.autoCompleteRepuesto.setAdapter(adapter)

            // Si viene de escaneo QR, pre-seleccionar
            uidEscaneado?.let { valorEscaneado ->
                // Buscar por UID o por Código Interno
                val encontrado = listaRepuestos.find { it.uid == valorEscaneado || it.codigoInterno == valorEscaneado }
                if (encontrado != null) {
                    repuestoSeleccionado = encontrado
                    binding.autoCompleteRepuesto.setText("${encontrado.codigoInterno} - ${encontrado.nombre}", false)
                    actualizarUICardRepuesto()
                    uidEscaneado = null // Limpiar para que no lo haga de nuevo si el fragment se recrea
                } else {
                    Toast.makeText(requireContext(), "QR no reconocido o producto inactivo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.registroExitoso.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(requireContext(), "Movimiento registrado", Toast.LENGTH_SHORT).show()
                
                // Volver al listado global quitando el escáner del historial
                parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }
}
