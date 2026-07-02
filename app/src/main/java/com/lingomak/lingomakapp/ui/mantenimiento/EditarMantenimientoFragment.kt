package com.lingomak.lingomakapp.ui.mantenimiento

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.FragmentEditarMantenimientoBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditarMantenimientoFragment : Fragment() {

    private var _binding: FragmentEditarMantenimientoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MantenimientoViewModel by viewModels()

    private var uidMantenimiento = ""
    private var codigoMantenimiento = ""
    private var uidMaquinaria = ""
    private var codigoMaquinaria = ""
    private var nombreMaquinaria = ""
    private var tipoMaquinaria = ""
    private var responsable = ""
    private var estadoActual = ""
    private var fechaRegistro = ""
    private var registradoPor = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentEditarMantenimientoBinding.inflate(
                inflater,
                container,
                false
            )

        configurarSpinnerTipo()
        cargarDatos()
        configurarEventos()
        observarViewModel()

        return binding.root
    }

    private fun configurarSpinnerTipo() {

        val tipos = listOf(
            "PREVENTIVO",
            "CORRECTIVO",
            "PREDICTIVO"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            tipos
        )

        binding.spTipoMantenimientoEditar.adapter = adapter
    }

    private fun cargarDatos() {

        uidMantenimiento = arguments?.getString("uid") ?: ""
        codigoMantenimiento = arguments?.getString("codigoMantenimiento") ?: ""
        uidMaquinaria = arguments?.getString("uidMaquinaria") ?: ""
        codigoMaquinaria = arguments?.getString("codigoMaquinaria") ?: ""
        nombreMaquinaria = arguments?.getString("nombreMaquinaria") ?: ""
        tipoMaquinaria = arguments?.getString("tipoMaquinaria") ?: ""
        responsable = arguments?.getString("responsable") ?: ""
        estadoActual = arguments?.getString("estado") ?: ""
        fechaRegistro = arguments?.getString("fechaRegistro") ?: ""
        registradoPor = arguments?.getString("registradoPor") ?: ""

        val tipo = arguments?.getString("tipoMantenimiento") ?: ""
        val descripcion = arguments?.getString("descripcion") ?: ""
        val fecha = arguments?.getString("fechaProgramada") ?: ""
        val prioridad = arguments?.getString("prioridad") ?: ""
        val horometro = arguments?.getInt("horometroProgramado") ?: 0
        val costo = arguments?.getDouble("costoEstimado") ?: 0.0
        val observaciones = arguments?.getString("observaciones") ?: ""


        binding.tvCodigoMantenimientoEditar.text =
            "Código: $codigoMantenimiento"

        binding.tvMaquinariaEditar.text =
            "Maquinaria: $nombreMaquinaria"

        binding.tvCodigoMaquinariaEditar.text =
            "Código maquinaria: $codigoMaquinaria"

        binding.tvResponsableEditar.text =
            "Responsable: $responsable"


        binding.etDescripcionEditar.setText(descripcion)
        binding.etFechaProgramadaEditar.setText(fecha)
        binding.etHorometroProgramadoEditar.setText(horometro.toString())
        binding.etCostoEstimadoEditar.setText(costo.toString())
        binding.etObservacionesEditar.setText(observaciones)

        val posicionTipo =
            (binding.spTipoMantenimientoEditar.adapter as ArrayAdapter<String>)
                .getPosition(tipo)

        binding.spTipoMantenimientoEditar.setSelection(posicionTipo)

        when (prioridad) {
            "ALTA" ->
                binding.rbPrioridadAltaEditar.isChecked = true

            "MEDIA" ->
                binding.rbPrioridadMediaEditar.isChecked = true

            "BAJA" ->
                binding.rbPrioridadBajaEditar.isChecked = true
        }
    }

    private fun configurarEventos() {

        binding.etFechaProgramadaEditar.setOnClickListener {
            mostrarDatePicker()
        }

        binding.btnGuardarCambiosMantenimiento.setOnClickListener {
            actualizarMantenimiento()
        }
    }

    private fun mostrarDatePicker() {

        val calendario = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->

                val fecha =
                    "$dayOfMonth/${month + 1}/$year"

                binding.etFechaProgramadaEditar.setText(fecha)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }

    private fun obtenerPrioridad(): String {

        return when (binding.rgPrioridadEditar.checkedRadioButtonId) {

            binding.rbPrioridadAltaEditar.id -> "ALTA"

            binding.rbPrioridadMediaEditar.id -> "MEDIA"

            binding.rbPrioridadBajaEditar.id -> "BAJA"

            else -> ""
        }
    }

    private fun actualizarMantenimiento() {

        val tipo =
            binding.spTipoMantenimientoEditar.selectedItem.toString()

        val prioridad = obtenerPrioridad()

        val descripcion =
            binding.etDescripcionEditar.text.toString().trim()

        val fecha =
            binding.etFechaProgramadaEditar.text.toString().trim()

        val horometro =
            binding.etHorometroProgramadoEditar.text.toString().trim()

        val costo =
            binding.etCostoEstimadoEditar.text.toString().trim()

        val observaciones =
            binding.etObservacionesEditar.text.toString().trim()

        if (
            descripcion.isEmpty() ||
            fecha.isEmpty() ||
            prioridad.isEmpty() ||
            horometro.isEmpty()
        ) {

            Toast.makeText(
                requireContext(),
                "Complete los campos obligatorios",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val mantenimiento = MantenimientoModel(

            uid = uidMantenimiento,
            codigoMantenimiento = codigoMantenimiento,

            uidMaquinaria = uidMaquinaria,
            codigoMaquinaria = codigoMaquinaria,
            nombreMaquinaria = nombreMaquinaria,
            tipoMaquinaria = tipoMaquinaria,

            tipoMantenimiento = tipo,
            descripcion = descripcion,
            fechaProgramada = fecha,
            estado = estadoActual,
            responsable = responsable,
            observaciones = observaciones,

            costoEstimado = costo.toDoubleOrNull() ?: 0.0,

            horometroProgramado =
                horometro.toIntOrNull() ?: 0,

            fechaRealizada = "",
            costoReal = 0.0,
            horometroReal = 0,

            fechaRegistro = fechaRegistro,
            fechaActualizacion = obtenerFechaActual(),

            registradoPor = registradoPor,
            actualizadoPor = registradoPor,

            prioridad = prioridad
        )
        mostrarCargando(true)

        viewModel.actualizarMantenimiento(
            mantenimiento = mantenimiento,
            onSuccess = {
                mostrarCargando(false)
                Toast.makeText(
                    requireContext(),
                    "Mantenimiento actualizado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(
                        com.lingomak.lingomakapp.R.id.fragmentContainerAdmin,
                        MantenimientoFragment()
                    )
                    .commit()
            }
        )
    }

    private fun obtenerFechaActual(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())
    }

    private fun observarViewModel() {

        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            mostrarCargando(false)
            Toast.makeText(
                requireContext(),
                mensaje,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarCargando(cargando : Boolean){

        binding.btnGuardarCambiosMantenimiento.isEnabled = !cargando

        binding.btnGuardarCambiosMantenimiento.text =
            if(cargando)
                "Guardando..."
            else
                "Guardar cambios"

        binding.progressEditarMantenimiento.visibility =
            if(cargando)
                View.VISIBLE
            else
                View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}