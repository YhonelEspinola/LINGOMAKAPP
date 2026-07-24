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
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentPerfilOperarioBinding
import com.lingomak.lingomakapp.ui.auth.CambiarPasswordFragment
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
            // Cerramos sesión de Firebase
            FirebaseAuth.getInstance().signOut()

            // Limpiamos el stack y volvemos al login
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnCambiarPassword.setOnClickListener {
            val fragment = CambiarPasswordFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("modoForzado", false)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.containerOperario, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}