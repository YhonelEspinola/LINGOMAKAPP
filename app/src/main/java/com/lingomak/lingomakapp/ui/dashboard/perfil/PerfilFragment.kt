package com.lingomak.lingomakapp.ui.dashboard.perfil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.messaging.FirebaseMessaging
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentPerfilBinding
import com.lingomak.lingomakapp.ui.auth.CambiarPasswordFragment
import com.lingomak.lingomakapp.ui.auth.LoginActivity

class PerfilFragment : Fragment() {


    private var _binding: FragmentPerfilBinding? = null

    private val binding get() = _binding!!

    private val viewModel : PerfilViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)

        observarViewModel()

        viewModel.cargarUsuarioLogado()

        binding.btnCambiarPassword.setOnClickListener {
            val fragment = CambiarPasswordFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("modoForzado", false)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerAdmin, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnCerrarSesion.setOnClickListener {
            // Desuscripción en background para no bloquear el logout si falla la red
            FirebaseMessaging.getInstance().unsubscribeFromTopic("administradores")

            viewModel.cerrarSesion()

            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }

        return binding.root
    }

    private fun observarViewModel() {
        viewModel.usuario.observe(viewLifecycleOwner){ usuario ->
            binding.tvNombreUsuario.text = "Nombre : ${usuario.nombre}"
            binding.tvCorreoUsuario.text = "Correo: ${usuario.correo}"
            binding.tvRolUsuario.text = "Rol: ${usuario.rol}"
        }

        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

}