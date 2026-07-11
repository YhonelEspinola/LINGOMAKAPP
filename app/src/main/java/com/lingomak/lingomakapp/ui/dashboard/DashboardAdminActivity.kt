package com.lingomak.lingomakapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.DashboardAdminBinding
import com.lingomak.lingomakapp.ui.alertas.AlertasFragment
import com.lingomak.lingomakapp.ui.dashboard.home.HomeAdminFragment
import com.lingomak.lingomakapp.ui.dashboard.perfil.PerfilFragment
import com.lingomak.lingomakapp.ui.mantenimiento.MantenimientoFragment
import com.lingomak.lingomakapp.ui.maquinaria.MaquinariaFragment
import com.lingomak.lingomakapp.ui.movimientos.MovimientosGlobalFragment
import com.lingomak.lingomakapp.ui.repuestos.InventarioFragment
import com.lingomak.lingomakapp.ui.usuarios.UsuariosFragment
import com.lingomak.lingomakapp.workers.AlertasWorkerManager

class DashboardAdminActivity : AppCompatActivity() {

    private lateinit var binding: DashboardAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DashboardAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(savedInstanceState == null){
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainerAdmin,
                    HomeAdminFragment() )
                .commit()
        }

        configurarTollbar()

        configurarNavigationDrawer()

        AlertasWorkerManager.programarRevisionAlertas(this)
    }

    private fun configurarTollbar(){
        binding.toolbarAdmin.setNavigationOnClickListener {
            binding.drawerLayoutAdmin.open()
        }
    }

    private fun configurarNavigationDrawer(){
        binding.navigationViewAdmin.setNavigationItemSelectedListener { item ->
            when (item.itemId){
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerAdmin, HomeAdminFragment())
                        .commit()
                    binding.drawerLayoutAdmin.close()
                    true
                }

                R.id.drawer_usuarios ->{
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainerAdmin,
                            UsuariosFragment()
                        )
                        .commit()

                    binding.drawerLayoutAdmin.close()
                    true
                }

                R.id.drawer_mantenimiento ->{
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainerAdmin,
                            MantenimientoFragment()
                        )
                        .commit()

                    binding.drawerLayoutAdmin.close()
                    true
                }

                R.id.drawer_maquinaria ->{
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainerAdmin,
                            MaquinariaFragment()
                        )
                        .commit()

                    binding.drawerLayoutAdmin.close()
                    true
                }

                R.id.drawer_inventario -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainerAdmin,
                            InventarioFragment()
                        )
                        .commit()
                    binding.drawerLayoutAdmin.close()
                    true
                }

                R.id.drawer_movimientos -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainerAdmin,
                            MovimientosGlobalFragment()
                        )
                        .commit()
                    binding.drawerLayoutAdmin.close()
                    true
                }

                R.id.drawer_alertas -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainerAdmin,
                            AlertasFragment()
                        )
                        .commit()
                    binding.drawerLayoutAdmin.close()
                    true
                }

                R.id.nav_perfil -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerAdmin, PerfilFragment())
                        .commit()
                    binding.drawerLayoutAdmin.close()
                    true
                }

                else -> false
            }
        }
    }
}
