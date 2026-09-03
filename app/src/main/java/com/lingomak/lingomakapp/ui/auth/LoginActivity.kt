package com.lingomak.lingomakapp.ui.auth

import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.lingomak.lingomakapp.databinding.AuthLoginBinding
import android.os.Bundle
import android.widget.Toast

import android.content.Intent
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity
import com.lingomak.lingomakapp.ui.dashboard.DashboardOperarioActivity
import com.lingomak.lingomakapp.ui.auth.CambiarPasswordFragment
import com.lingomak.lingomakapp.utils.Constants

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: AuthLoginBinding

    private val viewModel : LoginViewModel by viewModels()

    override fun onCreate(savedInstance: Bundle?) {
        com.lingomak.lingomakapp.utils.ThemeManager.applyTheme(this)
        super.onCreate(savedInstance)

        binding = AuthLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Forzar ocultación instantánea de caracteres (eliminar previsualización)
        binding.etPassword.transformationMethod = PasswordTransformationMethod()

        configurarEventos()
        observarViewModel()

        viewModel.verificarSesionActiva()
    }

    private fun configurarEventos(){
        binding.btnLogin.setOnClickListener {
            val correo = binding.etCorreo.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingrese correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.login(correo,password)
        }
    }

    private fun observarViewModel() {
        viewModel.loading.observe(this) { cargando ->
            binding.progressLogin.visibility = if (cargando) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { mensaje ->
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        }

        viewModel.usuario.observe(this) { usuario ->
            if (usuario.estado != Constants.ESTADO_ACTIVO) {
                Toast.makeText(this, "Usuario inactivo", Toast.LENGTH_SHORT).show()
                return@observe
            }

            when (usuario.rol) {
                Constants.ROL_ADMIN -> {
                    startActivity(Intent(this, DashboardAdminActivity::class.java))
                    finish()
                }

                Constants.ROL_OPERARIO -> {
                    if (usuario.debeCambiarPassword) {

                        supportFragmentManager
                            .beginTransaction()
                            .replace(
                                android.R.id.content,
                                CambiarPasswordFragment()
                            )
                            .commit()

                    } else {

                        startActivity(
                            Intent(this, DashboardOperarioActivity::class.java)
                        )
                        finish()
                    }
                }

                else -> {
                    Toast.makeText(this, "Rol no reconocido", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}