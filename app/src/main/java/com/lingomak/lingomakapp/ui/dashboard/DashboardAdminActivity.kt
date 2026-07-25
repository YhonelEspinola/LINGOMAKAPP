package com.lingomak.lingomakapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.lingomak.lingomakapp.BuildConfig
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.DashboardAdminBinding
import com.lingomak.lingomakapp.ui.alertas.AlertasFragment
import com.lingomak.lingomakapp.ui.alertas.AlertasViewModel
import com.lingomak.lingomakapp.ui.auth.LoginActivity
import com.lingomak.lingomakapp.ui.dashboard.home.HomeAdminFragment
import com.lingomak.lingomakapp.ui.dashboard.perfil.PerfilFragment
import com.lingomak.lingomakapp.ui.mantenimiento.MantenimientoFragment
import com.lingomak.lingomakapp.ui.mantenimiento.SolicitudesMantenimientoFragment
import com.lingomak.lingomakapp.ui.maquinaria.MaquinariaFragment
import com.lingomak.lingomakapp.ui.movimientos.MovimientosGlobalFragment
import com.lingomak.lingomakapp.ui.repuestos.InventarioFragment
import com.lingomak.lingomakapp.ui.usuarios.UsuariosFragment
import com.lingomak.lingomakapp.utils.Constants
import com.lingomak.lingomakapp.data.worker.AlertasWorkerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardAdminActivity : AppCompatActivity() {

    private lateinit var binding: DashboardAdminBinding
    private var userListener: ListenerRegistration? = null
    private val alertasViewModel: AlertasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            DashboardAdminBinding.inflate(layoutInflater)

        setContentView(binding.root)

        escucharEstadoUsuario()
        configurarBadgeAlertas()

        /*
         * Configuramos Remote Config para el nombre del modelo de IA
         */
        configurarRemoteConfig()

        /*
         * Primero configuramos todos los componentes
         * de navegación.
         */
        configurarToolbar()
        configurarNavigationDrawer()

        /*
         * Programa las revisiones locales mediante WorkManager.
         */
        AlertasWorkerManager.programarRevisionAlertas(this)

        dispararSincronizacionInicial()

        /*
         * Este dispositivo recibirá las notificaciones
         * dirigidas a administradores.
         *
         * Nos aseguramos de suscribirnos solo si somos admin.
         */
        FirebaseMessaging.getInstance()
            .subscribeToTopic("administradores")

        /*
         * Solo decidimos la pantalla inicial cuando
         * Android crea esta Activity por primera vez.
         */
        if (savedInstanceState == null) {

            /*
             * Primero revisamos si la Activity fue abierta
             * desde una notificación.
             */
            val destinoProcesado =
                procesarDestinoNotificacion(intent)

            /*
             * Si no vino desde una notificación,
             * mostramos el Home normalmente.
             */
            if (!destinoProcesado) {
                abrirHome()
            }
        }
    }

    private fun dispararSincronizacionInicial() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                com.lingomak.lingomakapp.data.repository.RepuestoRepository(this@DashboardAdminActivity).descargarCambiosDeFirestore()
                com.lingomak.lingomakapp.data.repository.MaquinariaRepository(this@DashboardAdminActivity).descargarMaquinariasDeFirestore()
                com.lingomak.lingomakapp.data.repository.MantenimientoRepository(this@DashboardAdminActivity).descargarCambiosDeFirestore()
                com.lingomak.lingomakapp.data.repository.UserRepository(this@DashboardAdminActivity).descargarUsuariosDeFirestore()
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    private fun configurarBadgeAlertas() {
        alertasViewModel.totalAlertas.observe(this) { total ->
            val menuItem = binding.navigationViewAdmin.menu.findItem(R.id.drawer_alertas)
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
        alertasViewModel.listarAlertas(esOperario = false)
    }

    /**
     * Escucha en tiempo real si el usuario es inactivado por otro administrador.
     */
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

    /**
     * Se ejecuta cuando el Dashboard ya estaba abierto
     * y se pulsa una nueva notificación.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        /*
         * Actualizamos el Intent almacenado por la Activity.
         */
        setIntent(intent)

        /*
         * Volvemos a procesar el destino.
         */
        procesarDestinoNotificacion(intent)
    }

    /**
     * Revisa los extras recibidos desde la notificación.
     *
     * Devuelve true si encontró y abrió un destino válido.
     */
    private fun procesarDestinoNotificacion(
        intentRecibido: Intent?
    ): Boolean {

        /*
         * Las notificaciones creadas por NotificationHelper
         * utilizan "tipoDestino".
         *
         * Las notificaciones automáticas de FCM pueden llegar
         * con la clave original "tipo".
         *
         * Por eso admitimos ambas.
         */
        val tipoDestino =
            intentRecibido
                ?.getStringExtra("tipoDestino")
                .orEmpty()
                .ifBlank {
                    intentRecibido
                        ?.getStringExtra("tipo")
                        .orEmpty()
                }

        val uidSolicitud =
            intentRecibido
                ?.getStringExtra("uidSolicitud")
                .orEmpty()

        return when (tipoDestino) {

            "SOLICITUD_MANTENIMIENTO" -> {

                val fragment =
                    SolicitudesMantenimientoFragment().apply {

                        /*
                         * Guardamos el UID en el Fragment.
                         *
                         * Todavía no lo usamos para filtrar,
                         * pero luego podremos resaltar la solicitud.
                         */
                        arguments = Bundle().apply {
                            putString(
                                "uidSolicitudDestacada",
                                uidSolicitud
                            )
                        }
                    }

                supportFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainerAdmin,
                        fragment
                    )
                    .commit()

                true
            }

            else -> false
        }
    }

    /**
     * Abre la pantalla principal del administrador.
     */
    private fun abrirHome() {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainerAdmin,
                HomeAdminFragment()
            )
            .commit()
    }

    /**
     * Configura el botón del Toolbar que abre
     * el NavigationDrawer.
     */
    private fun configurarToolbar() {

        binding.toolbarAdmin
            .setNavigationOnClickListener {

                binding.drawerLayoutAdmin.open()
            }
    }

    /**
     * Configura las opciones del menú lateral.
     */
    private fun configurarNavigationDrawer() {

        binding.navigationViewAdmin
            .setNavigationItemSelectedListener { item ->

                when (item.itemId) {

                    R.id.nav_home -> {

                        abrirHome()
                        binding.drawerLayoutAdmin.close()

                        true
                    }

                    R.id.nav_perfil -> {

                        abrirFragmentDrawer(
                            PerfilFragment()
                        )

                        true
                    }

                    R.id.drawer_usuarios -> {

                        abrirFragmentDrawer(
                            UsuariosFragment()
                        )

                        true
                    }

                    R.id.drawer_mantenimiento -> {

                        abrirFragmentDrawer(
                            MantenimientoFragment()
                        )

                        true
                    }

                    R.id.drawer_maquinaria -> {

                        abrirFragmentDrawer(
                            MaquinariaFragment()
                        )

                        true
                    }

                    R.id.drawer_inventario -> {

                        abrirFragmentDrawer(
                            InventarioFragment()
                        )

                        true
                    }

                    R.id.drawer_movimientos -> {

                        abrirFragmentDrawer(
                            MovimientosGlobalFragment()
                        )

                        true
                    }

                    R.id.drawer_alertas -> {

                        abrirFragmentDrawer(
                            AlertasFragment()
                        )

                        true
                    }

                    else -> false
                }
            }
    }

    /**
     * Método reutilizable para abrir una pantalla
     * desde el NavigationDrawer y cerrar el menú.
     */
    private fun abrirFragmentDrawer(
        fragment: androidx.fragment.app.Fragment
    ) {

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainerAdmin,
                fragment
            )
            .commit()

        binding.drawerLayoutAdmin.close()
    }

    /**
     * Configura y descarga los valores de Firebase Remote Config.
     */
    private fun configurarRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Valores por defecto
        val defaultValues = mapOf<String, Any>()
        remoteConfig.setDefaultsAsync(defaultValues)
        
        // Descargar y activar valores
        remoteConfig.fetchAndActivate()
    }
}
