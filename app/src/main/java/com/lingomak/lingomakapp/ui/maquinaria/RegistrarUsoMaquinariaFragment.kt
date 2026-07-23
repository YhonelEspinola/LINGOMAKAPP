package com.lingomak.lingomakapp.ui.maquinaria

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.databinding.FragmentRegistrarUsoMaquinariaBinding

class RegistrarUsoMaquinariaFragment : Fragment() {

    private var _binding: FragmentRegistrarUsoMaquinariaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegistrarUsoMaquinariaViewModel by viewModels()

    private var uidMaquinaria = ""
    private var codigoMaquinaria = ""
    private var nombreMaquinaria = ""
    private var tipoMaquinaria = ""

    private var horometroActual = 0
    private var horometroUltimoMantenimiento = 0
    private var intervaloMantenimientoHoras = 250

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrarUsoMaquinariaBinding.inflate(inflater, container, false)

        cargarDatos()
        configurarEventos()
        observarViewModel()
        calcularVistaPrevia()

        return binding.root
    }

    private fun cargarDatos() {
        uidMaquinaria = arguments?.getString("uidMaquinaria") ?: ""
        codigoMaquinaria = arguments?.getString("codigoMaquinaria") ?: ""
        nombreMaquinaria = arguments?.getString("nombreMaquinaria") ?: ""
        tipoMaquinaria = arguments?.getString("tipoMaquinaria") ?: ""

        horometroActual = arguments?.getInt("horometroActual") ?: 0
        horometroUltimoMantenimiento =
            arguments?.getInt("horometroUltimoMantenimiento") ?: 0
        intervaloMantenimientoHoras =
            arguments?.getInt("intervaloMantenimientoHoras") ?: 250

        binding.tvNombreMaquinaria.text = nombreMaquinaria
        binding.tvCodigoMaquinaria.text = "Código: $codigoMaquinaria"
        binding.tvTipoMaquinaria.text = "Tipo: $tipoMaquinaria"
        binding.tvHorometroActual.text = "Horómetro actual: $horometroActual h"
        binding.tvUltimoMantenimiento.text =
            "Último mantenimiento: $horometroUltimoMantenimiento h"
        binding.tvIntervaloMantenimiento.text =
            "Intervalo: cada $intervaloMantenimientoHoras h"
    }

    private fun configurarEventos() {
        binding.etHorasUso.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calcularVistaPrevia()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnGuardarUso.setOnClickListener {
            validarYRegistrarUso()
        }
    }

    private fun calcularVistaPrevia() {
        val horasUso =
            binding.etHorasUso.text.toString().trim().toIntOrNull()

        if (horasUso == null || horasUso <= 0) {
            binding.tvNuevoHorometro.text = "Nuevo horómetro: --"
            binding.tvHorasRestantes.text = "Horas restantes: --"
            binding.tvMensajeMantenimiento.text =
                "Ingrese las horas trabajadas para calcular el estado del mantenimiento."
            binding.tvMensajeMantenimiento.setTextColor(Color.rgb(107, 114, 128))
            return
        }

        val nuevoHorometro =
            horometroActual + horasUso

        val horasDesdeUltimo =
            nuevoHorometro - horometroUltimoMantenimiento

        val horasRestantes =
            intervaloMantenimientoHoras - horasDesdeUltimo

        binding.tvNuevoHorometro.text =
            "Nuevo horómetro: $nuevoHorometro h"

        binding.tvHorasRestantes.text =
            "Horas restantes: $horasRestantes h"

        when {
            horasRestantes <= 0 -> {
                binding.tvMensajeMantenimiento.text =
                    "🔴 Con este registro se superará el límite de mantenimiento. Se enviará una solicitud al administrador."
                binding.tvMensajeMantenimiento.setTextColor(Color.rgb(185, 28, 28))
            }

            horasRestantes <= 20 -> {
                binding.tvMensajeMantenimiento.text =
                    "🔴 La maquinaria quedará muy cerca del mantenimiento. Se enviará una solicitud al administrador."
                binding.tvMensajeMantenimiento.setTextColor(Color.rgb(185, 28, 28))
            }

            horasRestantes <= 50 -> {
                binding.tvMensajeMantenimiento.text =
                    "🟡 Atención: la maquinaria se está acercando al mantenimiento preventivo."
                binding.tvMensajeMantenimiento.setTextColor(Color.rgb(180, 83, 9))
            }

            else -> {
                binding.tvMensajeMantenimiento.text =
                    "🟢 La maquinaria aún se encuentra dentro del rango normal de uso."
                binding.tvMensajeMantenimiento.setTextColor(Color.rgb(22, 101, 52))
            }
        }
    }

    private fun validarYRegistrarUso() {
        val horasTexto = binding.etHorasUso.text.toString().trim()
        val observacion = binding.etObservacion.text.toString().trim()

        val horasUso = horasTexto.toIntOrNull()

        if (uidMaquinaria.isEmpty()) {
            Toast.makeText(requireContext(), "No se encontró la maquinaria", Toast.LENGTH_SHORT).show()
            return
        }

        if (horasUso == null || horasUso <= 0) {
            Toast.makeText(requireContext(), "Ingrese horas válidas", Toast.LENGTH_SHORT).show()
            return
        }

        if (horasUso > 24) {
            Toast.makeText(requireContext(), "No puede registrar más de 24 horas por día", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioActual = FirebaseAuth.getInstance().currentUser

        viewModel.registrarUsoMaquinaria(
            uidMaquinaria = uidMaquinaria,
            uidOperario = usuarioActual?.uid ?: "",
            nombreOperario = usuarioActual?.displayName ?: "",
            correoOperario = usuarioActual?.email ?: "",
            horasUso = horasUso,
            observacion = observacion
        )
    }

    private fun observarViewModel() {
        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.progressUsoMaquinaria.visibility =
                if (cargando) View.VISIBLE else View.GONE

            binding.btnGuardarUso.isEnabled = !cargando
        }

        viewModel.registroExitoso.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(
                    requireContext(),
                    "Uso de maquinaria registrado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()
            }
        }

        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}