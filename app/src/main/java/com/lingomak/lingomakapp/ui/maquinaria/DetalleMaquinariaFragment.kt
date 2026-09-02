package com.lingomak.lingomakapp.ui.maquinaria

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.databinding.FragmentDetalleMaquinariaBinding
import com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity
import com.lingomak.lingomakapp.utils.formatoHoras

class DetalleMaquinariaFragment : Fragment() {

    private var _binding: FragmentDetalleMaquinariaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MaquinariaViewModel by viewModels()
    private var uid: String = ""
    private var maquinariaActual: MaquinariaModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleMaquinariaBinding.inflate(inflater, container, false)
        uid = arguments?.getString("uid") ?: ""

        configurarBotones()
        observarViewModel()

        return binding.root
    }

    private fun observarViewModel() {
        if (uid.isNotEmpty()) {
            viewModel.obtenerMaquinariaPorUid(uid).observe(viewLifecycleOwner) { maquinaria ->
                maquinaria?.let {
                    maquinariaActual = it
                    pintarDatos(it)
                }
            }
            
            viewModel.consumoPromedio.observe(viewLifecycleOwner) { consumo ->
                if (consumo != null) {
                    binding.tvConsumoPromedio.text = "%.2f Gls/h".format(consumo)
                    binding.tvConsumoPromedio.setTextColor(requireContext().getColor(R.color.text_primary))
                } else {
                    binding.tvConsumoPromedio.text = "Sin datos suficientes"
                    binding.tvConsumoPromedio.setTextColor(requireContext().getColor(R.color.text_secondary))
                }
            }
            
            viewModel.cargarConsumoPromedio(uid)
            
            viewModel.nivelCombustible.observe(viewLifecycleOwner) { nivel ->
                if (nivel != null) {
                    binding.tvNivelCombustibleDetalle.text = "%.1f%% (%.1f Gls)".format(nivel.porcentaje, nivel.galones)
                    val colorRes = when {
                        nivel.porcentaje <= 20 -> R.color.danger
                        nivel.porcentaje <= 50 -> R.color.warning
                        else -> R.color.success
                    }
                    val color = requireContext().getColor(colorRes)
                    binding.tvNivelCombustibleDetalle.setTextColor(color)
                    binding.ivIconoCombustibleDetalle.setColorFilter(color)
                    binding.ivIconoCombustibleDetalle.setImageResource(R.drawable.ic_gas_station)
                } else {
                    binding.tvNivelCombustibleDetalle.text = "Sin datos suficientes"
                    val colorSecondary = requireContext().getColor(R.color.text_secondary)
                    binding.tvNivelCombustibleDetalle.setTextColor(colorSecondary)
                    binding.ivIconoCombustibleDetalle.setColorFilter(colorSecondary)
                    binding.ivIconoCombustibleDetalle.setImageResource(R.drawable.ic_gas_station)
                }
            }
            viewModel.cargarNivelCombustible(uid)
        }
    }

    private fun pintarDatos(maquinaria: MaquinariaModel) {
        if (maquinaria.imagenUrl.isNotEmpty()) {
            Glide.with(this)
                .load(maquinaria.imagenUrl)
                .centerCrop()
                .placeholder(R.drawable.bg_image_placeholder)
                .error(R.drawable.bg_image_placeholder)
                .into(binding.imgDetalleMaquinaria)
        } else {
            binding.imgDetalleMaquinaria.setImageResource(R.drawable.bg_image_placeholder)
        }

        binding.tvNombreDetalleMaquinaria.text = maquinaria.nombre
        binding.tvCodigoDetalleMaquinaria.text = "CÓDIGO: ${maquinaria.codigoMaquinaria}"
        binding.tvEstadoDetalleMaquinaria.text = maquinaria.estado
        
        // Aplicar color según estado
        val colorEstado = when(maquinaria.estado) {
            "OPERATIVA" -> requireContext().getColor(R.color.success)
            "INACTIVA" -> requireContext().getColor(R.color.danger)
            else -> requireContext().getColor(R.color.warning)
        }
        binding.tvEstadoDetalleMaquinaria.setTextColor(colorEstado)

        binding.tvTipoDetalleMaquinaria.text = maquinaria.tipo
        binding.tvMarcaDetalleMaquinaria.text = maquinaria.marca
        binding.tvModeloDetalleMaquinaria.text = maquinaria.modelo
        binding.tvAnioDetalleMaquinaria.text = maquinaria.anio.toString()
        binding.tvPlacaDetalleMaquinaria.text = maquinaria.placaSerie
        binding.tvHorometroDetalleMaquinaria.text = "${maquinaria.horometroActual.formatoHoras()} h"
        binding.tvHorometroUltimoDetalleMaquinaria.text = "${maquinaria.horometroUltimoMantenimiento.formatoHoras()} h"

        // Próximo mantenimiento
        val horasDesdeUltimo = maquinaria.horometroActual - maquinaria.horometroUltimoMantenimiento
        val horasRestantes = maquinaria.intervaloMantenimientoHoras - horasDesdeUltimo

        when {
            horasRestantes <= 20 -> {
                binding.tvProximoMantenimientoDetalle.text = "Faltan ${horasRestantes.formatoHoras()} h para mantenimiento"
                binding.tvProximoMantenimientoDetalle.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.danger))
            }
            horasRestantes <= 50 -> {
                binding.tvProximoMantenimientoDetalle.text = "Faltan ${horasRestantes.formatoHoras()} h para mantenimiento"
                binding.tvProximoMantenimientoDetalle.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.warning))
            }
            else -> {
                binding.tvProximoMantenimientoDetalle.text = "Faltan ${horasRestantes.formatoHoras()} h para mantenimiento"
                binding.tvProximoMantenimientoDetalle.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success))
            }
        }

        binding.tvUbicacionDetalleMaquinaria.text = maquinaria.ubicacionActual
        binding.tvObservacionesDetalleMaquinaria.text = maquinaria.observaciones.ifEmpty { "Sin observaciones registradas." }

        // Actualizar label del botón Activar/Inactivar
        val esInactiva = maquinaria.estado == "INACTIVA"
        binding.btnCambiarEstado.text = if (esInactiva) "ACTIVAR MAQUINARIA" else "INACTIVAR MAQUINARIA"
    }

    private fun configurarBotones() {
        val isAdmin = requireActivity() is DashboardAdminActivity
        
        binding.btnEditarMaquinaria.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.btnCambiarEstado.visibility = if (isAdmin) View.VISIBLE else View.GONE

        binding.btnVolverMaquinaria.setOnClickListener { parentFragmentManager.popBackStack() }
        
        binding.btnEditarMaquinaria.setOnClickListener {
            val fragment = EditarMaquinariaFragment()
            val bundle = Bundle()
            bundle.putString("uid", uid)
            fragment.arguments = bundle
            
            val containerId = if (isAdmin) R.id.fragmentContainerAdmin else R.id.containerOperario
            
            parentFragmentManager.beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnCambiarEstado.setOnClickListener {
            maquinariaActual?.let { mostrarDialogoCambiarEstado(it) }
        }
    }

    private fun mostrarDialogoCambiarEstado(maquinaria: MaquinariaModel) {
        if (maquinaria.estado == "EN_MANTENIMIENTO") {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Acción no permitida")
                .setMessage("La maquinaria se encuentra actualmente EN MANTENIMIENTO. Debe finalizar el mantenimiento correspondiente para cambiar su estado.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        val esInactiva = maquinaria.estado == "INACTIVA"
        val titulo = if (esInactiva) "Activar maquinaria" else "Inactivar maquinaria"
        val mensaje = if (esInactiva) "¿Desea activar la maquinaria ${maquinaria.nombre}? Volverá al estado OPERATIVA." 
                      else "¿Desea inactivar la maquinaria ${maquinaria.nombre}?"
        val nuevoEstado = if (esInactiva) "OPERATIVA" else "INACTIVA"

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Confirmar") { dialog, _ ->
                viewModel.cambiarEstadoMaquinaria(
                    uid = maquinaria.uid,
                    nuevoEstado = nuevoEstado,
                    onSuccess = {
                        Toast.makeText(
                            requireContext(),
                            "Estado actualizado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}