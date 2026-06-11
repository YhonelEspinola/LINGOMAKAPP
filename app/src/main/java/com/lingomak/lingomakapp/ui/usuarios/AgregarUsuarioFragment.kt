package com.lingomak.lingomakapp.ui.usuarios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.databinding.FragmentAgregarUsuarioBinding
import com.lingomak.lingomakapp.utils.Constants


class AgregarUsuarioFragment : Fragment() {

    private var _binding : FragmentAgregarUsuarioBinding? = null

    private val binding get() = _binding!!

    private val viewModel : AgregarUsuarioViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAgregarUsuarioBinding.inflate(inflater,container, false)
        obtenerViewModel()

        configurarEventos()
        return binding.root
    }

    private fun configurarEventos(){
        binding.btnGuardarUsuario.setOnClickListener {
            val nombre = binding.etNombreUsuario.text.toString().trim()
            val correo = binding.etCorreoUsuario.text.toString().trim()
            val passwordTemporal = binding.etPasswordTemporal.text.toString().trim()
            val rol = obtenerRolSeleccionado()

            if(nombre.isEmpty() || correo.isEmpty() || passwordTemporal.isEmpty() || rol.isEmpty()){
                Toast.makeText(
                    requireContext(),
                    "Complete todos los campos",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            viewModel.crearUsuario(
                nombre = nombre,
                correo = correo,
                password = passwordTemporal,
                rol = rol
            )

        }
    }

    private fun obtenerRolSeleccionado(): String{

        return when (binding.rgRol.checkedRadioButtonId){
            binding.rbAdmin.id -> Constants.ROL_ADMIN

            binding.rbOperario.id -> Constants.ROL_OPERARIO

            else -> ""
        }

    }

    private fun obtenerViewModel(){
        viewModel.usuarioCreado.observe(viewLifecycleOwner) { creado ->
            if(creado){
                Toast.makeText(
                    requireContext(),
                    "Usuario registrado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()
            }
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