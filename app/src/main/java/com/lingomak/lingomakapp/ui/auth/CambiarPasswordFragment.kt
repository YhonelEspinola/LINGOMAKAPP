package com.lingomak.lingomakapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.databinding.FragmentCambiarPasswordBinding
import com.lingomak.lingomakapp.ui.dashboard.DashboardOperarioActivity
import com.lingomak.lingomakapp.utils.Constants

class CambiarPasswordFragment : Fragment() {

    private var _binding: FragmentCambiarPasswordBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseFirestore.getInstance()

    private var modoForzado = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCambiarPasswordBinding.inflate(inflater, container, false)

        // Forzar ocultación instantánea de caracteres
        binding.etNuevaPassword.transformationMethod = PasswordTransformationMethod()
        binding.etConfirmarPassword.transformationMethod = PasswordTransformationMethod()

        modoForzado = arguments?.getBoolean("modoForzado", true) ?: true

        configurarEventos()

        return binding.root
    }

    private fun configurarEventos() {
        binding.btnGuardarPassword.setOnClickListener {
            validarFormulario()
        }
    }

    private fun validarFormulario() {
        val nuevaPassword = binding.etNuevaPassword.text.toString().trim()
        val confirmarPassword = binding.etConfirmarPassword.text.toString().trim()

        if (nuevaPassword.isEmpty() || confirmarPassword.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Complete todos los campos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (nuevaPassword.length < 6) {
            Toast.makeText(
                requireContext(),
                "La contraseña debe tener mínimo 6 caracteres",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (nuevaPassword != confirmarPassword) {
            Toast.makeText(
                requireContext(),
                "Las contraseñas no coinciden",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        cambiarPassword(nuevaPassword)
    }

    private fun cambiarPassword(nuevaPassword: String) {
        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            Toast.makeText(
                requireContext(),
                "No se encontró sesión activa",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        mostrarCargando(true)

        usuarioActual.updatePassword(nuevaPassword)
            .addOnSuccessListener {

                database.collection(Constants.USUARIOS)
                    .document(usuarioActual.uid)
                    .update("debeCambiarPassword", false)
                    .addOnSuccessListener {
                        mostrarCargando(false)

                        Toast.makeText(
                            requireContext(),
                            "Contraseña actualizada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        if (modoForzado) {
                            startActivity(
                                Intent(
                                    requireContext(),
                                    DashboardOperarioActivity::class.java
                                )
                            )
                            requireActivity().finish()
                        } else {
                            parentFragmentManager.popBackStack()
                        }
                    }
                    .addOnFailureListener { exception ->
                        mostrarCargando(false)

                        Toast.makeText(
                            requireContext(),
                            exception.message ?: "Error al actualizar usuario",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->
                mostrarCargando(false)

                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Error al cambiar contraseña",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressCambiarPassword.visibility =
            if (cargando) View.VISIBLE else View.GONE

        binding.btnGuardarPassword.isEnabled = !cargando
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}