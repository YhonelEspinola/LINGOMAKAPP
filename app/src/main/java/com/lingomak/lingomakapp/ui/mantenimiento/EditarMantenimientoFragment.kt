package com.lingomak.lingomakapp.ui.mantenimiento

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.datepicker.MaterialDatePicker
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.FragmentEditarMantenimientoBinding
import java.text.SimpleDateFormat
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
        _binding = FragmentEditarMantenimientoBinding.inflate(inflater, container, false)
        uidMantenimiento = arguments?.getString("uid") ?: ""

        configurarSpinnerTipo()
        cargarDatos()
        configurarEventos()
        observarViewModel()

        return binding.root
    }

    private fun configurarSpinnerTipo() {
        val tipos = listOf("PREVENTIVO", "CORRECTIVO", "PREDICTIVO")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tipos)
        binding.spTipoMantenimientoEditar.adapter = adapter
    }

    private fun cargarDatos() {
        viewModel.obtenerMantenimientoPorUid(uidMantenimiento) { m ->
            codigoMantenimiento = m.codigoMantenimiento
            uidMaquinaria = m.uidMaquinaria
            codigoMaquinaria = m.codigoMaquinaria
            nombreMaquinaria = m.nombreMaquinaria
            tipoMaquinaria = m.tipoMaquinaria
            responsable = m.responsable
            estadoActual = m.estado
            fechaRegistro = m.fechaRegistro
            registradoPor = m.registradoPor

            binding.tvCodigoMantenimientoEditar.text = "Código: $codigoMantenimiento"
            binding.tvMaquinariaEditar.text = "Maquinaria: $nombreMaquinaria"
            binding.tvCodigoMaquinariaEditar.text = "Código maquinaria: $codigoMaquinaria"
            binding.tvResponsableEditar.text = "Responsable: $responsable"
            
            binding.etDescripcionEditar.setText(m.descripcion)
            binding.etFechaProgramadaEditar.setText(m.fechaProgramada)
            binding.etHorometroProgramadoEditar.setText(m.horometroProgramado.toString())
            binding.etCostoEstimadoEditar.setText(m.costoEstimado.toString())
            binding.etObservacionesEditar.setText(m.observaciones)

            val tipoPos = when (m.tipoMantenimiento) {
                "PREVENTIVO" -> 0
                "CORRECTIVO" -> 1
                "PREDICTIVO" -> 2
                else -> 0
            }
            binding.spTipoMantenimientoEditar.setSelection(tipoPos)

            when (m.prioridad) {
                "ALTA" -> binding.rbPrioridadAltaEditar.isChecked = true
                "MEDIA" -> binding.rbPrioridadMediaEditar.isChecked = true
                "BAJA" -> binding.rbPrioridadBajaEditar.isChecked = true
            }
        }
    }

    private fun configurarEventos() {
        binding.etFechaProgramadaEditar.setOnClickListener { mostrarDatePicker() }
        binding.btnGuardarCambiosMantenimiento.setOnClickListener { actualizarMantenimiento() }
        // Nota: ivBotonRegresar no existe en este layout, se asume popBackStack por back button o toolbar si hubiera
    }

    private fun mostrarDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Seleccionar fecha")
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            val date = Date(selection)
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.etFechaProgramadaEditar.setText(format.format(date))
        }
        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun obtenerPrioridad(): String {
        return when (binding.rgPrioridadEditar.checkedRadioButtonId) {
            R.id.rbPrioridadAltaEditar -> "ALTA"
            R.id.rbPrioridadMediaEditar -> "MEDIA"
            R.id.rbPrioridadBajaEditar -> "BAJA"
            else -> "MEDIA"
        }
    }

    private fun actualizarMantenimiento() {
        val desc = binding.etDescripcionEditar.text.toString().trim()
        val fecha = binding.etFechaProgramadaEditar.text.toString().trim()
        val horometroStr = binding.etHorometroProgramadoEditar.text.toString().trim()
        val costoStr = binding.etCostoEstimadoEditar.text.toString().trim()

        if (desc.isEmpty() || fecha.isEmpty() || horometroStr.isEmpty()) {
            Toast.makeText(requireContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val m = MantenimientoModel(
            uid = uidMantenimiento,
            codigoMantenimiento = codigoMantenimiento,
            uidMaquinaria = uidMaquinaria,
            codigoMaquinaria = codigoMaquinaria,
            nombreMaquinaria = nombreMaquinaria,
            tipoMaquinaria = tipoMaquinaria,
            tipoMantenimiento = binding.spTipoMantenimientoEditar.selectedItem.toString(),
            descripcion = desc,
            fechaProgramada = fecha,
            estado = estadoActual,
            prioridad = obtenerPrioridad(),
            responsable = responsable,
            horometroProgramado = horometroStr.toIntOrNull() ?: 0,
            costoEstimado = costoStr.toDoubleOrNull() ?: 0.0,
            observaciones = binding.etObservacionesEditar.text.toString().trim(),
            fechaRegistro = fechaRegistro,
            fechaActualizacion = obtenerFechaActual(),
            registradoPor = registradoPor
        )

        mostrarCargando(true)
        viewModel.actualizarMantenimiento(m) {
            Toast.makeText(requireContext(), "Mantenimiento actualizado", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun obtenerFechaActual(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun observarViewModel() {
        viewModel.mensajeError.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                mostrarCargando(false)
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarCargando(show: Boolean) {
        binding.progressEditarMantenimiento.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnGuardarCambiosMantenimiento.isEnabled = !show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
