package com.lingomak.lingomakapp.ui.dashboard

import android.os.Bundle
import android.widget.Toast
import com.lingomak.lingomakapp.R
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.databinding.DashboardOperarioBinding
import android.content.Intent
import com.lingomak.lingomakapp.ui.auth.LoginActivity
import com.lingomak.lingomakapp.ui.dashboard.perfil.PerfilOperarioFragment

class DashboardOperarioActivity : AppCompatActivity() {


    private lateinit var  binding: DashboardOperarioBinding

    override fun onCreate(saveInstanceState: Bundle?){
        super.onCreate(saveInstanceState)

        binding = DashboardOperarioBinding.inflate(layoutInflater)

        setContentView(binding.root)

        configurarToolbar()
        configurarBottomVavigation()
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
                        .commit()
                    true
                }

                R.id.menu_cerrar_sesion_operario -> {
                    FirebaseAuth.getInstance().signOut()

                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()

                    true
                }

                else -> false
            }
        }
    }

    private fun configurarBottomVavigation(){
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId){
                R.id.nav_op_inventario -> true

                R.id.nav_op_movimientos -> true

                R.id.nav_op_qr -> true

                R.id.nav_op_mantenimiento -> true

                R.id.nav_op_alertas -> true

                else -> false
            }
        }
    }

}