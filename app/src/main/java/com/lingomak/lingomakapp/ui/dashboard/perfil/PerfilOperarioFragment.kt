package com.lingomak.lingomakapp.ui.dashboard.perfil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.databinding.FragmentPerfilOperarioBinding
import com.lingomak.lingomakapp.ui.auth.LoginActivity

class PerfilOperarioFragment : Fragment() {

    private var _binding : FragmentPerfilOperarioBinding? = null

    private val binding get() = _binding!!

    private val viewModel : PerfilViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPerfilOperarioBinding.inflate(inflater,container,false)

        observarViewModel()
        configurarEventos()

        viewModel.cargarUsuarioLogado()

        return binding.root
    }

    private fun observarViewModel(){
        viewModel.usuario.observe(viewLifecycleOwner){ usuario ->
            binding.tvNombreOperario.text = "Nombre: ${usuario.nombre}"
            binding.tvCorreoOperario.text = "Correo: ${usuario.correo}"
            binding.tvRolOperario.text = "Rol: ${usuario.rol}"
        }
        viewModel.error.observe(viewLifecycleOwner){mensaje ->
            Toast.makeText(requireContext(),mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarEventos(){
        binding.btnCerrarSesionOperario.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        binding.btnCambiarPassword.setOnClickListener {
            Toast.makeText(requireContext(),"prueba", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}