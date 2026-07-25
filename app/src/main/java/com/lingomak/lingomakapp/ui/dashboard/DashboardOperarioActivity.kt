package com.lingomak.lingomakapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.DashboardOperarioBinding
import com.lingomak.lingomakapp.ui.alertas.AlertasFragment
import com.lingomak.lingomakapp.ui.alertas.AlertasViewModel
import com.lingomak.lingomakapp.ui.auth.LoginActivity
import com.lingomak.lingomakapp.ui.dashboard.home.HomeOperarioFragment
import com.lingomak.lingomakapp.ui.dashboard.perfil.PerfilOperarioFragment
import com.lingomak.lingomakapp.ui.mantenimiento.MantenimientoFragment
import com.lingomak.lingomakapp.ui.movimientos.EscaneoQRFragment
import com.lingomak.lingomakapp.ui.maquinaria.OperarioMaquinariaFragment
import com.lingomak.lingomakapp.ui.repuestos.InventarioOpFragment
import com.lingomak.lingomakapp.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardOperarioActivity : AppCompatActivity() {

    private lateinit var binding: DashboardOperarioBinding
    private var userListener: ListenerRegistration? = null
    private val alertasViewModel: AlertasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DashboardOperarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        escucharEstadoUsuario()
        configurarBadgeAlertas()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.containerOperario, HomeOperarioFragment())
                .commit()
        }

        configurarToolbar()
        configurarNavigationDrawer()
        
        dispararSincronizacionInicial()
    }

    private fun dispararSincronizacionInicial() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                com.lingomak.lingomakapp.data.repository.RepuestoRepository(this@DashboardOperarioActivity).descargarCambiosDeFirestore()
                com.lingomak.lingomakapp.data.repository.MaquinariaRepository(this@DashboardOperarioActivity).descargarMaquinariasDeFirestore()
                com.lingomak.lingomakapp.data.repository.MantenimientoRepository(this@DashboardOperarioActivity).descargarCambiosDeFirestore()
                com.lingomak.lingomakapp.data.repository.UserRepository(this@DashboardOperarioActivity).descargarUsuariosDeFirestore()
                com.lingomak.lingomakapp.data.repository.ConfiguracionRepository(this@DashboardOperarioActivity).descargarCategoriasDeFirestore()
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    private fun configurarBadgeAlertas() {
        alertasViewModel.totalAlertas.observe(this) { total ->
            val menuItem = binding.navigationViewOperario.menu.findItem(R.id.nav_op_alertas)
            val actionView = (menuItem.actionView as? android.widget.FrameLayout) 
                ?: layoutInflater.inflate(R.layout.menu_badge, null) as android.widget.FrameLayout
            
            val badge = actionView.findViewById<TextView>(R.id.tvBadgeCount)
            if (total > 0) {
                badge.visibility = View.VISIBLE
                badge.text = total.toString()
            } else {
                badge.visibility = View.GONE
            }
            menuItem.actionView = actionView
        }
    }

    override fun onResume() {
        super.onResume()
        alertasViewModel.listarAlertas(esOperario = true)
    }

    private fun escucharEstadoUsuario() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        userListener = FirebaseFirestore.getInstance()
            .collection(Constants.USUARIOS)
            .document(uid)
            .addSnapshotListener { snapshot, _ ->
                val estado = snapshot?.getString("estado")
                if (estado != null && estado.equals("INACTIVO", ignoreCase = true)) {
                    forzarLogout()
                }
            }
    }

    private fun forzarLogout() {
        userListener?.remove()
        FirebaseAuth.getInstance().signOut()
        
        Toast.makeText(this, "Tu cuenta ha sido desactivada", Toast.LENGTH_LONG).show()
        
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        userListener?.remove()
        super.onDestroy()
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

                else -> false
            }
        }
    }
}
