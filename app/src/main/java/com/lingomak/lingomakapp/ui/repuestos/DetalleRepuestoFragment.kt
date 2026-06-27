package com.lingomak.lingomakapp.ui.repuestos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.databinding.FragmentDetalleRepuestoBinding

/**
 * Fragment que muestra el detalle completo y estático (no editable)
 * de un repuesto: datos generales, imagen, código QR y acciones
 * (Editar, Activar/Inactivar, Volver).
 *
 * Recibe el uid del repuesto vía argumentos (Bundle) y carga sus
 * datos desde el InventarioViewModel.
 */
class DetalleRepuestoFragment : Fragment() {

    private var _binding: FragmentDetalleRepuestoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventarioViewModel by viewModels()

    private var uidRepuesto: String = ""

    // Se guarda el repuesto actual para reutilizarlo al navegar a Editar
    // o al cambiar su estado.
    private var repuestoActual: RepuestoModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDetalleRepuestoBinding.inflate(inflater, container, false)

        uidRepuesto = arguments?.getString("uid") ?: ""

        observarViewModel()

        viewModel.obtenerRepuestoPorUid(uidRepuesto)

        configurarEventos()

        return binding.root
    }

    private fun observarViewModel() {

        viewModel.repuestoSeleccionado.observe(viewLifecycleOwner) { repuesto ->
            if (repuesto != null) {
                repuestoActual = repuesto
                pintarDatos(repuesto)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Vuelca los datos del repuesto en las vistas (todas de solo lectura).
     */
    private fun pintarDatos(repuesto: RepuestoModel) {

        binding.tvNombre.text = repuesto.nombre
        binding.tvCodigoInterno.text = repuesto.codigoInterno
        binding.tvMarca.text = repuesto.marca
        binding.tvCategoria.text = repuesto.categoria
        binding.tvDescripcion.text =
            repuesto.descripcion.ifEmpty { "Sin descripción" }
        binding.tvStockActual.text = repuesto.stockActual.toString()
        binding.tvStockMinimo.text = repuesto.stockMinimo.toString()
        binding.tvStockMaximo.text = repuesto.stockMaximo.toString()
        binding.tvUbicacionAlmacen.text =
            repuesto.ubicacionAlmacen.ifEmpty { "No especificada" }
        binding.tvEstado.text = repuesto.estado

        val colorEstado = if (repuesto.estado == "ACTIVO") {
            R.color.success
        } else {
            R.color.danger
        }
        binding.tvEstado.setTextColor(requireContext().getColor(colorEstado))

        // Imagen del repuesto (o placeholder si no tiene).
        if (repuesto.imagenUrl.isNotEmpty()) {
            Glide.with(this)
                .load(repuesto.imagenUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(binding.ivImagenRepuesto)
        } else {
            binding.ivImagenRepuesto.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Código QR generado automáticamente para el repuesto.
        if (repuesto.codigoQR.isNotEmpty()) {
            Glide.with(this)
                .load(repuesto.codigoQR)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(binding.ivCodigoQR)
        } else {
            binding.ivCodigoQR.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun configurarEventos() {

        binding.ivBotonRegresar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnVerMovimientos.setOnClickListener {
            val fragment = MovimientosFragment()
            val bundle = Bundle().apply {
                putString("repuestoUid", uidRepuesto)
                putString("nombre", repuestoActual?.nombre)
                putInt("stockActual", repuestoActual?.stockActual ?: 0)
            }
            fragment.arguments = bundle
            
            parentFragmentManager.beginTransaction()
                .replace((requireView().parent as ViewGroup).id, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnEditar.setOnClickListener {
            val fragment = EditarRepuestoFragment()

            val bundle = Bundle()
            bundle.putString("uid", uidRepuesto)

            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(
                    (requireView().parent as ViewGroup).id,
                    fragment
                )
                .addToBackStack(null)
                .commit()
        }

        binding.btnCambiarEstado.setOnClickListener {
            repuestoActual?.let { repuesto ->
                mostrarDialogoCambiarEstado(repuesto)
            }
        }

        binding.btnVolver.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun mostrarDialogoCambiarEstado(repuesto: RepuestoModel) {

        val nuevoEstado = if (repuesto.estado == "ACTIVO") "INACTIVO" else "ACTIVO"

        val mensaje = if (nuevoEstado == "INACTIVO") {
            "Deseas inactivar ${repuesto.nombre}"
        } else {
            "Deseas activar ${repuesto.nombre}"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar acción")
            .setMessage(mensaje)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                viewModel.cambiarEstadoRepuesto(repuesto.uid, nuevoEstado)
                viewModel.obtenerRepuestoPorUid(repuesto.uid)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
