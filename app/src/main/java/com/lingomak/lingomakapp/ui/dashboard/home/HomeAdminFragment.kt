package com.lingomak.lingomakapp.ui.dashboard.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        viewModel.usuario.observe(viewLifecycleOwner) { usuario ->
            binding.tvBienvenida.text = "Bienvenido, ${usuario.nombre}"
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