package com.lingomak.lingomakapp.ui.dashboard.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentHomeOperarioBinding
import com.lingomak.lingomakapp.ui.alertas.AlertasFragment
import com.lingomak.lingomakapp.ui.maquinaria.OperarioMaquinariaFragment
import com.lingomak.lingomakapp.ui.maquinaria.RegistrarUsoMaquinariaFragment
import com.lingomak.lingomakapp.ui.repuestos.InventarioOpFragment

class HomeOperarioFragment : Fragment() {

    private var _binding: FragmentHomeOperarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeOperarioViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeOperarioBinding.inflate(
            inflater,
            container,
            false
        )

        configurarSaludo()
        configurarEventos()
        observarViewModel()
        
        viewModel.cargarAlertas()

        return binding.root
    }


    private fun configurarSaludo() {
        val usuarioActual = FirebaseAuth.getInstance().currentUser

        val nombre =
            usuarioActual?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: "Operario"

        binding.tvSaludoOperario.text = "Hola, $nombre"
    }


    private fun configurarEventos() {

        binding.cardRegistrarUso.setOnClickListener {
            abrirFragment(OperarioMaquinariaFragment())
        }

        binding.cardMovimientos.setOnClickListener {
            abrirFragment(InventarioOpFragment())
        }

        binding.cardAlertas.setOnClickListener {
            abrirFragment(AlertasFragment())
        }

        binding.cardHomeAlertas.setOnClickListener {
            abrirFragment(AlertasFragment())
        }
    }

    private fun observarViewModel() {
        // 1. Máquina en Uso
        viewModel.maquinariaEnUso.observe(viewLifecycleOwner) { maquinaria ->
            val ultimoUso = viewModel.ultimoUso.value
            // Criterio de "reciente": menos de 12 horas
            val esReciente = ultimoUso?.let { 
                (System.currentTimeMillis() - it.timestampLocal) < 12 * 60 * 60 * 1000 
            } ?: false

            if (maquinaria != null && esReciente) {
                binding.cardUltimoUso.visibility = View.VISIBLE
                binding.tvNombreMaquinaUso.text = maquinaria.nombre
                binding.tvHorometroMaquinaUso.text = "${maquinaria.horometroActual} h"
                
                val horasDesdeUltimo = maquinaria.horometroActual - maquinaria.horometroUltimoMantenimiento
                val horasRestantes = maquinaria.intervaloMantenimientoHoras - horasDesdeUltimo
                
                binding.tvRestanteMaquinaUso.text = "$horasRestantes h"
                val colorRestante = if (horasRestantes <= 20) R.color.danger else R.color.success
                binding.tvRestanteMaquinaUso.setTextColor(ContextCompat.getColor(requireContext(), colorRestante))

                binding.cardUltimoUso.setOnClickListener {
                    val fragment = RegistrarUsoMaquinariaFragment()
                    val bundle = Bundle().apply {
                        putString("uidMaquinaria", maquinaria.uid)
                        putString("codigoMaquinaria", maquinaria.codigoMaquinaria)
                        putString("nombreMaquinaria", maquinaria.nombre)
                        putString("tipoMaquinaria", maquinaria.tipo)
                        putInt("horometroActual", maquinaria.horometroActual)
                        putInt("horometroUltimoMantenimiento", maquinaria.horometroUltimoMantenimiento)
                        putInt("intervaloMantenimientoHoras", maquinaria.intervaloMantenimientoHoras)
                    }
                    fragment.arguments = bundle
                    abrirFragment(fragment)
                }
            } else {
                binding.cardUltimoUso.visibility = View.GONE
            }
        }

        // 2. Alertas
        viewModel.totalAlertas.observe(viewLifecycleOwner) { total ->
            binding.tvAlertasPendientesCount.text = total.toString()
            val color = if (total > 0) R.color.danger else R.color.text_secondary
            binding.tvAlertasPendientesCount.setTextColor(ContextCompat.getColor(requireContext(), color))
        }

        // 3. Sincronización
        viewModel.pendientesSync.observe(viewLifecycleOwner) { pendientes ->
            if (pendientes == 0) {
                binding.ivSyncIcon.setImageResource(R.drawable.ic_check)
                binding.ivSyncIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
                binding.tvSyncStatus.text = "Todo sincronizado"
                binding.tvSyncStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
            } else {
                binding.ivSyncIcon.setImageResource(R.drawable.ic_swap_vert)
                binding.ivSyncIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.brand_yellow))
                binding.tvSyncStatus.text = "$pendientes pendientes"
                binding.tvSyncStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_yellow))
            }
        }
    }


    private fun abrirFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.containerOperario,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}