package com.lingomak.lingomakapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.DashboardOperarioBinding
import com.lingomak.lingomakapp.ui.alertas.AlertasFragment
import com.lingomak.lingomakapp.ui.auth.LoginActivity
import com.lingomak.lingomakapp.ui.dashboard.operario.HomeOperarioFragment
import com.lingomak.lingomakapp.ui.dashboard.perfil.PerfilOperarioFragment
import com.lingomak.lingomakapp.ui.movimientos.MovimientosGlobalFragment
import com.lingomak.lingomakapp.ui.operario.OperarioMaquinariaFragment

class DashboardOperarioActivity : AppCompatActivity() {

    private lateinit var binding: DashboardOperarioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DashboardOperarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarToolbar()
        configurarBottomNavigation()

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId =
                R.id.nav_op_inicio
        }
    }


    private fun configurarToolbar() {

        binding.toolbar.inflateMenu(R.menu.toolbar_operario_menu)

        binding.toolbar.setOnMenuItemClickListener { item ->

            when (item.itemId) {

                R.id.menu_perfil_operario -> {

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.containerOperario,
                            PerfilOperarioFragment()
                        )
                        .addToBackStack(null)
                        .commit()

                    true
                }

                R.id.menu_cerrar_sesion_operario -> {

                    FirebaseAuth.getInstance().signOut()

                    val intent = Intent(
                        this,
                        LoginActivity::class.java
                    ).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }

                    startActivity(intent)
                    finish()

                    true
                }

                else -> false
            }
        }
    }


    private fun configurarBottomNavigation() {

        binding.bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_op_inicio -> {

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.containerOperario,
                            HomeOperarioFragment()
                        )
                        .commit()

                    true
                }

                R.id.nav_op_movimientos -> {

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.containerOperario,
                            MovimientosGlobalFragment()
                        )
                        .commit()

                    true
                }

                R.id.nav_op_qr -> {
                    true
                }

                R.id.nav_op_maquinaria -> {

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.containerOperario,
                            OperarioMaquinariaFragment()
                        )
                        .commit()

                    true
                }

                R.id.nav_op_alertas -> {
                    true
                }

                else -> false
            }
        }
    }
}