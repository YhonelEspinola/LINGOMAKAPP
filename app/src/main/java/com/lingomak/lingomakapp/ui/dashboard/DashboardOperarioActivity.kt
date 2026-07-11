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
import com.lingomak.lingomakapp.ui.mantenimiento.MantenimientoFragment
import com.lingomak.lingomakapp.ui.movimientos.EscaneoQRFragment
import com.lingomak.lingomakapp.ui.movimientos.MovimientosOpFragment
import com.lingomak.lingomakapp.ui.operario.OperarioMaquinariaFragment
import com.lingomak.lingomakapp.ui.repuestos.InventarioOpFragment

class DashboardOperarioActivity : AppCompatActivity() {

    private lateinit var binding: DashboardOperarioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DashboardOperarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.containerOperario, HomeOperarioFragment())
                .commit()
        }

        configurarToolbar()
        configurarNavigationDrawer()
    }

    private fun configurarToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayoutOperario.open()
        }
    }

    private fun configurarNavigationDrawer() {
        binding.navigationViewOperario.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_op_inicio -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.containerOperario, HomeOperarioFragment())
                        .commit()
                    binding.drawerLayoutOperario.close()
                    true
                }

                R.id.nav_op_inventario -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.containerOperario, InventarioOpFragment())
                        .commit()
                    binding.drawerLayoutOperario.close()
                    true
                }

                R.id.nav_op_movimientos -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.containerOperario, MovimientosOpFragment())
                        .commit()
                    binding.drawerLayoutOperario.close()
                    true
                }

                R.id.nav_op_maquinaria -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.containerOperario, OperarioMaquinariaFragment())
                        .commit()
                    binding.drawerLayoutOperario.close()
                    true
                }

                R.id.nav_op_qr -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.containerOperario, EscaneoQRFragment())
                        .commit()
                    binding.drawerLayoutOperario.close()
                    true
                }

                R.id.nav_op_mantenimiento -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.containerOperario, MantenimientoFragment())
                        .commit()
                    binding.drawerLayoutOperario.close()
                    true
                }

                R.id.nav_op_alertas -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.containerOperario, AlertasFragment())
                        .commit()
                    binding.drawerLayoutOperario.close()
                    true
                }

                R.id.menu_perfil_operario -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.containerOperario, PerfilOperarioFragment())
                        .commit()
                    binding.drawerLayoutOperario.close()
                    true
                }

                R.id.menu_cerrar_sesion_operario -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                    true
                }

                else -> false
            }
        }
    }
}