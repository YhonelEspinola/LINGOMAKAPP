package com.lingomak.lingomakapp.ui.dashboard.operario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentHomeOperarioBinding
import com.lingomak.lingomakapp.ui.alertas.AlertasFragment
import com.lingomak.lingomakapp.ui.movimientos.MovimientosGlobalFragment
import com.lingomak.lingomakapp.ui.operario.OperarioMaquinariaFragment

class HomeOperarioFragment : Fragment() {

    private var _binding: FragmentHomeOperarioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeOperarioBinding.inflate(
            inflater,
            container,
            false
        )

        configurarSaludo()
        configurarEventos()

        return binding.root
    }


    private fun configurarSaludo() {
        val usuarioActual = FirebaseAuth.getInstance().currentUser

        val nombre =
            usuarioActual?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: "Operario"

        binding.tvSaludoOperario.text = "Hola, $nombre"
    }


    private fun configurarEventos() {

        binding.cardRegistrarUso.setOnClickListener {
            abrirFragment(OperarioMaquinariaFragment())
        }

        binding.cardMovimientos.setOnClickListener {

        }

        binding.cardEscanearQr.setOnClickListener {

        }

        binding.cardAlertas.setOnClickListener {

        }
    }


    private fun abrirFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.containerOperario,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}