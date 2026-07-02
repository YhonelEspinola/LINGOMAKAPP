package com.lingomak.lingomakapp.ui.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.DashboardAdminBinding
import com.lingomak.lingomakapp.ui.dashboard.home.HomeAdminFragment
import com.lingomak.lingomakapp.ui.dashboard.perfil.PerfilFragment
import com.lingomak.lingomakapp.ui.mantenimiento.MantenimientoFragment
import com.lingomak.lingomakapp.ui.maquinaria.MaquinariaFragment
import com.lingomak.lingomakapp.ui.usuarios.UsuariosFragment

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

        configurarBottomNavigation()

        configurarTollbar()

        configurarNavigationDrawer()
    }

    private fun configurarBottomNavigation(){
        binding.bottomNavigationAdmin.setOnItemSelectedListener { item ->
            when(item.itemId){
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerAdmin, HomeAdminFragment())
                        .commit()
                    true
                }
                R.id.nav_inventario -> {
                    true
                }
                R.id.nav_mantenimiento -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerAdmin, MantenimientoFragment())
                        .commit()
                    true
                }
                R.id.nav_perfil -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerAdmin,
                    PerfilFragment())
                    .commit()
                    true
                }
                else -> false
            }

        }
    }

    private fun configurarTollbar(){
        binding.toolbarAdmin.setNavigationOnClickListener {
            binding.drawerLayoutAdmin.open()
        }
    }

    private fun configurarNavigationDrawer(){
        binding.navigationViewAdmin.setNavigationItemSelectedListener { item ->
            when (item.itemId){
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

                else -> false
            }
        }
    }
}