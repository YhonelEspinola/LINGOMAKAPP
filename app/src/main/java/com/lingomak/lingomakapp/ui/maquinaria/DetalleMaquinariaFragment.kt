package com.lingomak.lingomakapp.ui.maquinaria

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentDetalleMaquinariaBinding

class DetalleMaquinariaFragment : Fragment() {

    private var _binding: FragmentDetalleMaquinariaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleMaquinariaBinding.inflate(inflater, container, false)

        cargarDatos()

        return binding.root
    }

    private fun cargarDatos() {
        val nombre = arguments?.getString("nombre") ?: ""
        val codigo = arguments?.getString("codigoMaquinaria") ?: ""
        val tipo = arguments?.getString("tipo") ?: ""
        val marca = arguments?.getString("marca") ?: ""
        val modelo = arguments?.getString("modelo") ?: ""
        val placaSerie = arguments?.getString("placaSerie") ?: ""
        val anio = arguments?.getInt("anio") ?: 0
        val estado = arguments?.getString("estado") ?: ""
        val horometroActual = arguments?.getInt("horometroActual") ?: 0
        val horometroUltimo = arguments?.getInt("horometroUltimoMantenimiento") ?: 0
        val ubicacion = arguments?.getString("ubicacionActual") ?: ""
        val observaciones = arguments?.getString("observaciones") ?: ""
        val imagenUrl = arguments?.getString("imagenUrl") ?: ""

        if (imagenUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imagenUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_maquinaria_placeholder)
                .error(R.drawable.ic_maquinaria_placeholder)
                .into(binding.imgDetalleMaquinaria)
        } else {
            binding.imgDetalleMaquinaria.setImageResource(R.drawable.ic_maquinaria_placeholder)
        }

        binding.tvNombreDetalleMaquinaria.text = nombre
        binding.tvCodigoDetalleMaquinaria.text = "CÓDIGO: $codigo"
        binding.tvEstadoDetalleMaquinaria.text = estado
        
        // Aplicar color según estado
        val colorEstado = when(estado) {
            "OPERATIVA" -> requireContext().getColor(R.color.success)
            "INACTIVA" -> requireContext().getColor(R.color.danger)
            else -> requireContext().getColor(R.color.warning)
        }
        binding.tvEstadoDetalleMaquinaria.setTextColor(colorEstado)

        binding.tvTipoDetalleMaquinaria.text = tipo
        binding.tvMarcaDetalleMaquinaria.text = marca
        binding.tvModeloDetalleMaquinaria.text = modelo
        binding.tvAnioDetalleMaquinaria.text = anio.toString()
        binding.tvPlacaDetalleMaquinaria.text = placaSerie
        binding.tvHorometroDetalleMaquinaria.text = "$horometroActual h"
        binding.tvHorometroUltimoDetalleMaquinaria.text = "$horometroUltimo h"
        binding.tvUbicacionDetalleMaquinaria.text = ubicacion
        binding.tvObservacionesDetalleMaquinaria.text = observaciones.ifEmpty { "Sin observaciones registradas." }

        // Configurar botones
        binding.btnVolverMaquinaria.setOnClickListener { parentFragmentManager.popBackStack() }
        
        binding.btnEditarMaquinaria.setOnClickListener {
            // Lógica para ir a editar (si existe el fragmento)
            val fragment = EditarMaquinariaFragment()
            val bundle = Bundle()
            bundle.putString("uid", arguments?.getString("uid"))
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace((requireView().parent as ViewGroup).id, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}