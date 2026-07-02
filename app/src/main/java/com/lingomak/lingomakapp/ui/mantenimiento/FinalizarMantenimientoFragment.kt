package com.lingomak.lingomakapp.ui.mantenimiento

import android.app.DatePickerDialog
import java.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.databinding.FragmentFinalizarMantenimientoBinding

class FinalizarMantenimientoFragment : Fragment() {

    private var _binding : FragmentFinalizarMantenimientoBinding? = null
    private val binding get() = _binding!!

    private var uidMaquinaria = ""
    private val viewModel : MantenimientoViewModel by viewModels()

    private var uidMantenimiento = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinalizarMantenimientoBinding.inflate(inflater, container, false)

        cargarDatos()
        configurarEventos()
        observarViewModel()

        return  binding.root
    }

    private fun cargarDatos(){
        uidMantenimiento = arguments?.getString("uid") ?: ""
        uidMaquinaria = arguments?.getString("uidMaquinaria") ?: ""

        val codigo = arguments?.getString("codigoMantenimiento") ?: ""
        val maquinaria = arguments?.getString("nombreMaquinaria") ?: ""
        val descripcion = arguments?.getString("descripcion") ?: ""

        binding.tvCodigoFinalizar.text = "Código: $codigo"
        binding.tvMaquinariaFinalizar.text = "Maquinaria: $maquinaria"
        binding.tvDescripcionFinalizar.text = "Descripción: $descripcion"

    }

    private fun configurarEventos(){
        binding.etFechaRealizada.setOnClickListener {
            mostrarDatePicker()
        }

        binding.btnFinalizarMantenimiento.setOnClickListener {
            validarFormulario()
        }
    }

    private fun mostrarDatePicker(){
        val calendario = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val fechaSeleccionada =  "$dayOfMonth/${month + 1}/$year"
                binding.etFechaRealizada.setText(fechaSeleccionada)

            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun validarFormulario(){
        val fechaRealizada = binding.etFechaRealizada.text.toString().trim()
        val horometroTexto = binding.etHorometroReal.text.toString().trim()
        val costoTexto = binding.etCostoReal.text.toString().trim()
        val observacionesFinales = binding.etObservacionesFinales.text.toString().trim()

        if(uidMantenimiento.isEmpty()){
            Toast.makeText(requireContext(), "No se encontro el mantenimiento", Toast.LENGTH_SHORT).show()
            return
        }

        if (fechaRealizada.isEmpty() || horometroTexto.isEmpty() || costoTexto.isEmpty()){
            Toast.makeText(requireContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val horometroReal = horometroTexto.toIntOrNull()
        if(horometroReal == null || horometroReal <= 0){
            Toast.makeText(requireContext(), "Ingrese un horómetro válido", Toast.LENGTH_SHORT).show()
            return
        }

        val costoReal = costoTexto.toDoubleOrNull()
        if(costoReal == null || costoReal < 0){
            Toast.makeText(requireContext(), "Ingrese un costo real válido", Toast.LENGTH_SHORT).show()
            return
        }
        if (uidMaquinaria.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No se encontró la maquinaria relacionada",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        mostrarCargando(true)

        viewModel.finalizarMantenimiento(
            uid = uidMantenimiento,
            uidMaquinaria = uidMaquinaria,
            fechaRealizada = fechaRealizada,
            horometroReal = horometroReal,
            costoReal = costoReal,
            observacionesFinales = observacionesFinales,
            onSuccess = {
                mostrarCargando(false)
                Toast.makeText(requireContext(),
                    "Mantenimiento finalizado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
                parentFragmentManager.popBackStack()
            }
        )



    }

    private fun observarViewModel(){

        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            mostrarCargando(false)
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarCargando(cargando : Boolean){

        binding.btnFinalizarMantenimiento.isEnabled = !cargando

        binding.btnFinalizarMantenimiento.text =
            if(cargando)
                "Finalizando..."
            else
                "Finalizar mantenimiento"

        binding.progressFinalizarMantenimiento.visibility =
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