package com.lingomak.lingomakapp.ui.usuarios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.databinding.FragmentEditarUsuarioBinding
import com.lingomak.lingomakapp.databinding.FragmentUsuariosBinding
import com.lingomak.lingomakapp.utils.Constants

class EditarUsuarioFragment : Fragment() {

    private  var _binding : FragmentEditarUsuarioBinding? = null

    private val binding get() = _binding!!

    private val viewModel : EditarUsuarioViewModel by viewModels()

    private var uid: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditarUsuarioBinding.inflate(
            inflater,
            container,
            false
        )

        obtenerDatos()

        cargarDatosFormulario()

        configurarEventos()

        observarViewModel()

        return  binding.root
    }

    private  fun obtenerDatos(){
        uid = arguments?.getString("uid") ?: ""
    }

    private fun cargarDatosFormulario(){
        val nombre = arguments?.getString("nombre") ?: ""
        val correo = arguments?.getString("correo") ?: ""
        val rol = arguments?.getString("rol") ?: ""

        binding.etNombreUsuario.setText(nombre)
        binding.etCorreoUsuario.setText(correo)

        if (rol == Constants.ROL_ADMIN){
            binding.rbAdmin.isChecked = true
        }else{
            binding.rbOperario.isChecked = true
        }

    }

    private fun configurarEventos(){
        binding.btnGuardarCambios.setOnClickListener {

            val nombre =
                binding.etNombreUsuario.text.toString().trim()


            val correo =
                binding.etCorreoUsuario.text.toString().trim()

            val rol =
                obtenerRolSeleccionado()

            viewModel.actualizarUsuario(
                uid = uid,
                nombre = nombre,
                correo = correo,
                rol = rol
            )

        }
    }

    private fun obtenerRolSeleccionado(): String{
        return  when (binding.rgRol.checkedRadioButtonId){
            binding.rbAdmin.id ->
                Constants.ROL_ADMIN

            binding.rbOperario.id ->
                Constants.ROL_OPERARIO

            else -> ""
        }
    }

    private fun observarViewModel(){
        viewModel.usuarioActualizado.observe(
            viewLifecycleOwner
        ){ actualizado ->
            if (actualizado) {

                Toast.makeText(
                    requireContext(),
                    "Usuario actualizado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()

            }

        }
        viewModel.error.observe(
            viewLifecycleOwner
        ){ mensaje ->
            Toast.makeText(
                requireContext(),
                mensaje,
                Toast.LENGTH_SHORT
            ).show()

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}