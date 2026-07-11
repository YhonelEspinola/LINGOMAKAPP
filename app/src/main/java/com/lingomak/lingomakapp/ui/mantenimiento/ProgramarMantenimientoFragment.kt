package com.lingomak.lingomakapp.ui.mantenimiento

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lingomak.lingomakapp.databinding.FragmentProgramarMantenimientoBinding
import java.util.Calendar
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.ui.maquinaria.MaquinariaViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.data.repository.UserRepository
import com.lingomak.lingomakapp.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ProgramarMantenimientoFragment : Fragment() {

    private var _binding: FragmentProgramarMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val maquinariaViewModel: MaquinariaViewModel by viewModels()
    private val solicitudViewModel: SolicitudMantenimientoViewModel by viewModels()
    private val mantenimientoViewModel: MantenimientoViewModel by viewModels()
    
    private val userRepository = UserRepository()

    private var listaMaquinarias = listOf<MaquinariaModel>()
    private var listaOperarios = listOf<UserModel>()

    private var maquinariaSeleccionada: MaquinariaModel? = null
    private var operarioSeleccionado: UserModel? = null

    private var vieneDeSolicitud = false
    private var uidSolicitud = ""
    private var uidMaquinariaSolicitud = ""
    private var motivoSolicitud = ""
    private var fechaSugerida = ""
    private var horometroSugerido = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgramarMantenimientoBinding.inflate(inflater, container, false)

        leerArgumentosSolicitud()
        configurarSpinners()
        aplicarDatosInicialesSolicitud()
        cargarMaquinaria()
        cargarOperarios()
        configurarEventos()
        observarMantenimientoViewModel()

        return binding.root
    }

    private fun configurarSpinners() {
        val tiposMantenimiento = listOf("PREVENTIVO", "CORRECTIVO", "PREDICTIVO")
        val adapterTipos = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tiposMantenimiento)
        binding.spTipoMantenimiento.adapter = adapterTipos
    }

    private fun cargarOperarios() {
        userRepository.listarOperarios(
            onSuccess = { users ->
                // Añadimos opción "TODOS" al inicio
                val todosOption = UserModel(uid = "TODOS", nombre = "TODOS LOS OPERARIOS")
                listaOperarios = listOf(todosOption) + users
                
                val nombres = listaOperarios.map { it.nombre }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, nombres)
                binding.spResponsable.adapter = adapter
            },
            onError = { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun configurarEventos() {
        binding.etFechaProgramada.setOnClickListener { mostrarDatePicker() }

        binding.btnGuardarMantenimiento.setOnClickListener { validarFormulario() }

        binding.spResponsable.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                operarioSeleccionado = listaOperarios.getOrNull(position)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spMaquinaria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    maquinariaSeleccionada = listaMaquinarias.getOrNull(position)
                    maquinariaSeleccionada?.let { maquinaria ->
                        val horometroAmostrar = if (vieneDeSolicitud && horometroSugerido > 0) horometroSugerido else maquinaria.horometroActual
                        binding.etHorometroProgramado.setText(horometroAmostrar.toString())
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val fechaSeleccionada = "$dayOfMonth/${month + 1}/$year"
                binding.etFechaProgramada.setText(fechaSeleccionada)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = calendario.timeInMillis
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
        val tipoMantenimiento = binding.spTipoMantenimiento.selectedItem?.toString() ?: ""
        val prioridad = obtenerPrioridadSeleccionada()
        val descripcion = binding.etDescripcion.text.toString().trim()
        val fechaProgramada = binding.etFechaProgramada.text.toString().trim()
        val horometro = binding.etHorometroProgramado.text.toString().trim()
        val costoEstimado = binding.etCostoEstimado.text.toString().trim()
        val observaciones = binding.etObservaciones.text.toString().trim()
        
        val operario = operarioSeleccionado

        if (tipoMantenimiento.isEmpty() || prioridad.isEmpty() || descripcion.isEmpty() || 
            fechaProgramada.isEmpty() || horometro.isEmpty() || operario == null) {
            Toast.makeText(requireContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val horometroInt = horometro.toIntOrNull() ?: 0
        val costoDouble = costoEstimado.toDoubleOrNull() ?: 0.0
        val maquinaria = maquinariaSeleccionada

        if (maquinaria == null) {
            Toast.makeText(requireContext(), "Seleccione una maquinaria válida", Toast.LENGTH_SHORT).show()
            return
        }

        val mantenimiento = MantenimientoModel(
            uid = UUID.randomUUID().toString(),
            codigoMantenimiento = generarCodigoMantenimiento(),
            uidMaquinaria = maquinaria.uid,
            codigoMaquinaria = maquinaria.codigoMaquinaria,
            nombreMaquinaria = maquinaria.nombre,
            tipoMaquinaria = maquinaria.tipo,
            tipoMantenimiento = tipoMantenimiento,
            descripcion = descripcion,
            fechaProgramada = fechaProgramada,
            estado = "PENDIENTE",
            responsable = operario.nombre,
            responsableUid = operario.uid,
            observaciones = observaciones,
            costoEstimado = costoDouble,
            horometroProgramado = horometroInt,
            fechaRegistro = obtenerFechaActual(),
            fechaActualizacion = obtenerFechaActual(),
            registradoPor = FirebaseAuth.getInstance().currentUser?.email ?: "ADMIN",
            prioridad = prioridad
        )

        mostrarCargando(true)

        mantenimientoViewModel.validarMantenimientoActivo(
            uidMaquinaria = maquinaria.uid,
            onExiste = {
                mostrarCargando(false)
                Toast.makeText(requireContext(), "La maquinaria ya tiene un mantenimiento pendiente o en proceso", Toast.LENGTH_LONG).show()
            },
            onNoExiste = {
                mantenimientoViewModel.agregarMantenimiento(
                    mantenimiento = mantenimiento,
                    onSuccess = {
                        if (!vieneDeSolicitud) {
                            finalizarExito("Mantenimiento programado correctamente")
                            return@agregarMantenimiento
                        }
                        solicitudViewModel.marcarComoConvertida(
                            uidSolicitud = uidSolicitud,
                            uidAdministrador = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                            uidMantenimientoGenerado = mantenimiento.uid,
                            onSuccess = { finalizarExito("Solicitud aprobada y mantenimiento programado") }
                        )
                    }
                )
            }
        )
    }

    private fun finalizarExito(mensaje: String) {
        mostrarCargando(false)
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun cargarMaquinaria(){
        maquinariaViewModel.listaMaquinarias.observe(viewLifecycleOwner) { lista ->
            listaMaquinarias = lista
            if(lista.isEmpty()) return@observe
            val nombres = lista.map { "${it.codigoMaquinaria} - ${it.nombre}" }
            binding.spMaquinaria.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, nombres)

            if (vieneDeSolicitud) {
                val pos = lista.indexOfFirst { it.uid == uidMaquinariaSolicitud }
                if (pos >= 0) {
                    binding.spMaquinaria.setSelection(pos)
                    maquinariaSeleccionada = lista[pos]
                    binding.spMaquinaria.isEnabled = false
                    if (horometroSugerido > 0) binding.etHorometroProgramado.setText(horometroSugerido.toString())
                }
            } else {
                maquinariaSeleccionada = lista.firstOrNull()
            }
        }
        maquinariaViewModel.listarMaquinarias()
    }

    private fun generarCodigoMantenimiento(): String {
        val fecha = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return "MAN-$fecha-${(1000..9999).random()}"
    }

    private fun obtenerFechaActual(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun observarMantenimientoViewModel() {
        mantenimientoViewModel.mensajeError.observe(viewLifecycleOwner) { msg ->
            mostrarCargando(false)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
        solicitudViewModel.mensajeError.observe(viewLifecycleOwner) { msg ->
            mostrarCargando(false)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarCargando(cargando : Boolean){
        binding.btnGuardarMantenimiento.isEnabled = !cargando
        binding.btnGuardarMantenimiento.text = if(cargando) "Guardando..." else "Programar mantenimiento"
        binding.progressGuardarMantenimiento.visibility = if(cargando) View.VISIBLE else View.GONE
    }

    private fun leerArgumentosSolicitud() {
        vieneDeSolicitud = arguments?.getString("origen") == "SOLICITUD_MANTENIMIENTO"
        uidSolicitud = arguments?.getString("uidSolicitud").orEmpty()
        uidMaquinariaSolicitud = arguments?.getString("uidMaquinaria").orEmpty()
        motivoSolicitud = arguments?.getString("motivoSolicitud").orEmpty()
        fechaSugerida = arguments?.getString("fechaSugerida").orEmpty()
        horometroSugerido = arguments?.getInt("horometroProgramado") ?: 0
    }

    private fun aplicarDatosInicialesSolicitud() {
        if (!vieneDeSolicitud) return
        if (motivoSolicitud.isNotBlank()) binding.etDescripcion.setText(motivoSolicitud)
        if (fechaSugerida.isNotBlank()) binding.etFechaProgramada.setText(fechaSugerida)
        if (horometroSugerido > 0) binding.etHorometroProgramado.setText(horometroSugerido.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}