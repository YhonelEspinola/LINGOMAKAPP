package com.lingomak.lingomakapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.DashboardAdminBinding
import com.lingomak.lingomakapp.ui.alertas.AlertasFragment
import com.lingomak.lingomakapp.ui.dashboard.home.HomeAdminFragment
import com.lingomak.lingomakapp.ui.dashboard.perfil.PerfilFragment
import com.lingomak.lingomakapp.ui.mantenimiento.MantenimientoFragment
import com.lingomak.lingomakapp.ui.mantenimiento.SolicitudesMantenimientoFragment
import com.lingomak.lingomakapp.ui.maquinaria.MaquinariaFragment
import com.lingomak.lingomakapp.ui.movimientos.MovimientosGlobalFragment
import com.lingomak.lingomakapp.ui.repuestos.InventarioFragment
import com.lingomak.lingomakapp.ui.usuarios.UsuariosFragment
import com.lingomak.lingomakapp.workers.AlertasWorkerManager

class DashboardAdminActivity : AppCompatActivity() {

    private lateinit var binding: DashboardAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            DashboardAdminBinding.inflate(layoutInflater)

        setContentView(binding.root)

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

        /*
         * Este dispositivo recibirá las notificaciones
         * dirigidas a administradores.
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
     * Configura la navegación inferior del administrador.
     */


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
}
