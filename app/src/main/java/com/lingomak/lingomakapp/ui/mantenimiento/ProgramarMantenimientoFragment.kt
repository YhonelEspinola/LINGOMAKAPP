package com.lingomak.lingomakapp.ui.mantenimiento

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lingomak.lingomakapp.databinding.FragmentProgramarMantenimientoBinding
import java.util.Calendar

class ProgramarMantenimientoFragment : Fragment() {

    private var _binding: FragmentProgramarMantenimientoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        _binding = FragmentProgramarMantenimientoBinding.inflate(
            inflater,
            container,
            false
        )

        configurarSpinners()

        configurarEventos()

        return binding.root
    }

    private fun configurarSpinners() {

        val tiposMantenimiento = listOf(
            "PREVENTIVO",
            "CORRECTIVO",
            "PREDICTIVO"
        )

        val adapterTipos = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            tiposMantenimiento
        )

        binding.spTipoMantenimiento.adapter = adapterTipos

        val maquinarias = listOf(
            "Excavadora CAT 320",
            "Volquete Volvo FMX",
            "Cargador Frontal CAT",
            "Retroexcavadora JCB"
        )


        val adapterMaquinaria = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            maquinarias
        )

        binding.spMaquinaria.adapter = adapterMaquinaria
    }

    private fun configurarEventos() {


        binding.etFechaProgramada.setOnClickListener {
            mostrarDatePicker()
        }


        binding.btnGuardarMantenimiento.setOnClickListener {
            validarFormulario()
        }
    }

    private fun mostrarDatePicker() {


        val calendario = Calendar.getInstance()

        val anio = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)


        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->


                val fechaSeleccionada =
                    "$dayOfMonth/${month + 1}/$year"


                binding.etFechaProgramada.setText(fechaSeleccionada)
            },
            anio,
            mes,
            dia
        )

        datePicker.show()
    }

    private fun obtenerPrioridadSeleccionada(): String {

        return when (binding.rgPrioridad.checkedRadioButtonId) {

            binding.rbPrioridadAlta.id -> "ALTA"

            binding.rbPrioridadMedia.id -> "MEDIA"

            binding.rbPrioridadBaja.id -> "BAJA"

            else -> ""
        }
    }

    private fun validarFormulario() {

        val tipoMantenimiento =
            binding.spTipoMantenimiento.selectedItem?.toString() ?: ""

        val maquinaria =
            binding.spMaquinaria.selectedItem?.toString() ?: ""

        val prioridad =
            obtenerPrioridadSeleccionada()

        val descripcion =
            binding.etDescripcion.text.toString().trim()

        val fechaProgramada =
            binding.etFechaProgramada.text.toString().trim()

        val responsable =
            binding.etResponsable.text.toString().trim()

        val horometro =
            binding.etHorometroProgramado.text.toString().trim()

        val costoEstimado =
            binding.etCostoEstimado.text.toString().trim()

        val observaciones =
            binding.etObservaciones.text.toString().trim()

        if (
            tipoMantenimiento.isEmpty() ||
            maquinaria.isEmpty() ||
            prioridad.isEmpty() ||
            descripcion.isEmpty() ||
            fechaProgramada.isEmpty() ||
            responsable.isEmpty() ||
            horometro.isEmpty()
        ) {
            Toast.makeText(
                requireContext(),
                "Complete los campos obligatorios",
                Toast.LENGTH_SHORT
            ).show()
            return
        }


        val horometroInt = horometro.toIntOrNull()

        if (horometroInt == null || horometroInt <= 0) {
            Toast.makeText(
                requireContext(),
                "Ingrese un horómetro válido",
                Toast.LENGTH_SHORT
            ).show()
            return
        }


        val costoDouble =
            if (costoEstimado.isEmpty()) {
                0.0
            } else {
                costoEstimado.toDoubleOrNull()
            }

        if (costoDouble == null) {
            Toast.makeText(
                requireContext(),
                "Ingrese un costo estimado válido",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Toast.makeText(
            requireContext(),
            "Mantenimiento guardado",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}