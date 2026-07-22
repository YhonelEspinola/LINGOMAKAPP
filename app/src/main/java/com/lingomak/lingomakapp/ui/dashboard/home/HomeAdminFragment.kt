package com.lingomak.lingomakapp.ui.dashboard.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.lingomak.lingomakapp.R
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.databinding.FragmentHomeAdminBinding

class HomeAdminFragment : Fragment() {

    private var _binding: FragmentHomeAdminBinding? = null

    private val binding get() = _binding!!

    private val viewModel: HomeAdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeAdminBinding.inflate(inflater, container, false)
        observarViewModel()
        viewModel.cargarUsuarioLogado()
        return binding.root
    }
    private fun observarViewModel() {

        // Navegación a estadísticas
        binding.cardEstadisticas.setOnClickListener {
            val fragment = com.lingomak.lingomakapp.ui.movimientos.MovimientosEstadisticasFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerAdmin, fragment)
                .addToBackStack(null)
                .commit()
        }

        viewModel.usuario.observe(viewLifecycleOwner) { usuario ->
            binding.tvBienvenida.text = "Bienvenido, ${usuario.nombre}"
        }

        viewModel.totalRepuestos.observe(viewLifecycleOwner) { total ->
            binding.tvTotalRepuestosValor.text = total.toString()
        }

        viewModel.totalMantenimientos.observe(viewLifecycleOwner) { total ->
            binding.tvMantenimientosValor.text = total.toString()
        }

        viewModel.stockCritico.observe(viewLifecycleOwner) { total ->
            binding.tvStockCriticoValor.text = total.toString()
        }

        // Si ocurre un error al cargar el usuario.
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

}