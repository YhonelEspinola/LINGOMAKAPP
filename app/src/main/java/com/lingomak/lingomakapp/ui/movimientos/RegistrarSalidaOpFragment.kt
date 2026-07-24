package com.lingomak.lingomakapp.ui.movimientos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.databinding.FragmentRegistrarSalidaOpBinding
import java.util.*

class RegistrarSalidaOpFragment : Fragment() {

    private var _binding: FragmentRegistrarSalidaOpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosOpViewModel by viewModels()
    
    private var listaRepuestos: List<RepuestoModel> = emptyList()
    private var listaMaquinas: List<MaquinariaModel> = emptyList()
    
    private var repuestoSeleccionado: RepuestoModel? = null
    private var maquinaSeleccionada: MaquinariaModel? = null
    
    private var uidEscaneado: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrarSalidaOpBinding.inflate(inflater, container, false)
        uidEscaneado = arguments?.getString("repuestoUidScanned")
        
        setupUI()
        observarViewModel()
        
        return binding.root
    }

    private fun setupUI() {
        binding.autoCompleteRepuesto.setOnItemClickListener { parent, _, position, _ ->
            val seleccionado = parent.getItemAtPosition(position) as String
            repuestoSeleccionado = listaRepuestos.find { "${it.codigoInterno} - ${it.nombre}" == seleccionado }
            actualizarUICardRepuesto()
        }

        binding.btnGuardar.setOnClickListener {
            validarYRegistrar()
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
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.includeRepuestoCard.ivImagenRepuesto)
            } else {
                binding.includeRepuestoCard.ivImagenRepuesto.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            binding.includeRepuestoCard.cardRepuestoDetalle.visibility = View.GONE
        }
    }

    private fun observarViewModel() {
        viewModel.todosLosRepuestos.observe(viewLifecycleOwner) { repuestos ->
            listaRepuestos = repuestos.filter { it.estado == "ACTIVO" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, 
                listaRepuestos.map { "${it.codigoInterno} - ${it.nombre}" })
            binding.autoCompleteRepuesto.setAdapter(adapter)

            uidEscaneado?.let { valor ->
                val encontrado = listaRepuestos.find { it.uid == valor || it.codigoInterno == valor }
                if (encontrado != null) {
                    repuestoSeleccionado = encontrado
                    binding.autoCompleteRepuesto.setText("${encontrado.codigoInterno} - ${encontrado.nombre}", false)
                    actualizarUICardRepuesto()
                    uidEscaneado = null
                }
            }
        }

        viewModel.todasLasMaquinas.observe(viewLifecycleOwner) { maquinas ->
            listaMaquinas = maquinas.filter { it.estado == "OPERATIVA" || it.estado == "EN_MANTENIMIENTO" }
        }

        viewModel.registroExitoso.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(requireContext(), "Salida registrada con éxito", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun validarYRegistrar() {
        val repuesto = repuestoSeleccionado
        val cantidadStr = binding.etCantidad.text.toString()

        if (repuesto == null) {
            Toast.makeText(requireContext(), "Seleccione un repuesto", Toast.LENGTH_SHORT).show()
            return
        }

        val destinoSalida = "DISTRIBUCION_EXTERNA"

        /*
        if (destinoSalida == "CONSUMO_INTERNO" && maquinaSeleccionada == null) {
            Toast.makeText(requireContext(), "Seleccione una maquinaria", Toast.LENGTH_SHORT).show()
            return
        }
        */

        val cantidad = cantidadStr.toIntOrNull() ?: 0
        if (cantidad <= 0) {
            binding.etCantidad.error = "Cantidad inválida"
            return
        }

        val registradoPor = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val movimiento = MovimientoModel(
            uid = UUID.randomUUID().toString(),
            repuestoUid = repuesto.uid,
            tipo = "SALIDA",
            cantidad = cantidad,
            fecha = Date(),
            registradoPor = registradoPor,
            observacion = binding.etObservacion.text.toString(),
            destinoSalida = destinoSalida,
            maquinariaUid = maquinaSeleccionada?.uid
        )

        viewModel.registrarSalida(movimiento)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}