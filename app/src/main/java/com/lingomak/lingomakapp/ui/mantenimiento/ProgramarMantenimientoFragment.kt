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
    private var listaMaquinarias = listOf<MaquinariaModel>()

    private var maquinariaSeleccionada: MaquinariaModel? = null

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseFirestore.getInstance()
    private var responsableActual = ""
    private var vieneDeSolicitud = false
    private var uidSolicitud = ""

    private var uidMaquinariaSolicitud = ""
    private var motivoSolicitud = ""
    private var fechaSugerida = ""
    private var horometroSugerido = 0

    private val mantenimientoViewModel: MantenimientoViewModel by viewModels()

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

        leerArgumentosSolicitud()
        configurarSpinners()
        aplicarDatosInicialesSolicitud()
        cargarMaquinaria()
        configurarEventos()
        cargarResponsableActual()
        observarMantenimientoViewModel()

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



    }

    private fun configurarEventos() {


        binding.etFechaProgramada.setOnClickListener {
            mostrarDatePicker()
        }


        binding.btnGuardarMantenimiento.setOnClickListener {
            validarFormulario()

        }

        binding.spMaquinaria.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    maquinariaSeleccionada = listaMaquinarias.getOrNull(position)

                    maquinariaSeleccionada?.let { maquinaria ->

                        val tipoSeleccionado =
                            binding.spTipoMantenimiento.selectedItem?.toString()

                        val proximoHorometro = when (tipoSeleccionado) {

                            "PREVENTIVO" ->
                                maquinaria.horometroActual

                            "PREDICTIVO" ->
                                maquinaria.horometroActual

                            "CORRECTIVO" ->
                                maquinaria.horometroActual

                            else ->
                                maquinaria.horometroActual
                        }

                        val horometroAmostrar =
                            if (vieneDeSolicitud && horometroSugerido > 0) {
                                horometroSugerido
                            } else {
                                proximoHorometro
                            }

                        binding.etHorometroProgramado.setText(
                            horometroAmostrar.toString()
                        )

                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
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

        val tipoMantenimiento =
            binding.spTipoMantenimiento.selectedItem?.toString() ?: ""

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

        if (DateUtils.fechaEsAnteriorAHoy(fechaProgramada)) {
            Toast.makeText(
                requireContext(),
                "La fecha programada no puede ser anterior a la fecha actual",
                Toast.LENGTH_LONG
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
        val maquinaria = maquinariaSeleccionada

        if (maquinaria != null &&
            horometroInt < maquinaria.horometroActual) {

            Toast.makeText(
                requireContext(),
                "El horómetro programado no puede ser menor al horómetro actual (${maquinaria.horometroActual} h).",
                Toast.LENGTH_LONG
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


        if (maquinaria?.estado == "INACTIVA") {

            Toast.makeText(
                requireContext(),
                "No se puede programar mantenimiento para una maquinaria inactiva",
                Toast.LENGTH_LONG
            ).show()

            return
        }
        if(maquinaria == null){
            Toast.makeText(
                requireContext(),
                "Seleccione una maquinaria válida",
                Toast.LENGTH_SHORT
            ).show()
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
            responsable = responsable,
            observaciones = observaciones,
            costoEstimado = costoDouble,
            horometroProgramado = horometroInt,

            fechaRealizada = "",
            costoReal = 0.0,
            horometroReal = 0,

            fechaRegistro = obtenerFechaActual(),
            fechaActualizacion = obtenerFechaActual(),
            registradoPor = FirebaseAuth.getInstance().currentUser?.email ?: "ADMIN",
            actualizadoPor = FirebaseAuth.getInstance().currentUser?.email ?: "ADMIN",

            prioridad = prioridad
        )

        mostrarCargando(true)

        mantenimientoViewModel.validarMantenimientoActivo(
            uidMaquinaria = maquinaria.uid,

            onExiste = {
                mostrarCargando(false)

                Toast.makeText(
                    requireContext(),
                    "La maquinaria ya tiene un mantenimiento pendiente o en proceso",
                    Toast.LENGTH_LONG
                ).show()
            },

            onNoExiste = {
                mantenimientoViewModel.agregarMantenimiento(
                    mantenimiento = mantenimiento,
                    onSuccess = {

                        if (!vieneDeSolicitud) {

                            mostrarCargando(false)

                            Toast.makeText(
                                requireContext(),
                                "Mantenimiento programado correctamente",
                                Toast.LENGTH_SHORT
                            ).show()

                            parentFragmentManager.popBackStack()
                            return@agregarMantenimiento
                        }

                        val uidAdministrador =
                            FirebaseAuth.getInstance()
                                .currentUser
                                ?.uid
                                .orEmpty()

                        if (
                            uidSolicitud.isBlank() ||
                            uidAdministrador.isBlank()
                        ) {
                            mostrarCargando(false)

                            Toast.makeText(
                                requireContext(),
                                "El mantenimiento se creó, pero no se pudo actualizar la solicitud",
                                Toast.LENGTH_LONG
                            ).show()

                            parentFragmentManager.popBackStack()
                            return@agregarMantenimiento
                        }

                        solicitudViewModel.marcarComoConvertida(
                            uidSolicitud = uidSolicitud,
                            uidAdministrador = uidAdministrador,

                            uidMantenimientoGenerado = mantenimiento.uid,

                            onSuccess = {
                                mostrarCargando(false)

                                Toast.makeText(
                                    requireContext(),
                                    "Solicitud aprobada y mantenimiento programado correctamente",
                                    Toast.LENGTH_LONG
                                ).show()

                                parentFragmentManager.popBackStack()
                            }
                        )
                    }
                )
            }
        )

    }

    private fun cargarMaquinaria(){
        maquinariaViewModel.listaMaquinarias.observe(viewLifecycleOwner) { lista ->
            listaMaquinarias = lista

            if(lista.isEmpty()){
                Toast.makeText(
                    requireContext(),
                    "No hay maquinarias registradas",
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }

            val nombresMaquinaria = lista.map { maquinaria ->
                "${maquinaria.codigoMaquinaria} - ${maquinaria.nombre}"
            }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                nombresMaquinaria
            )

            binding.spMaquinaria.adapter = adapter

            if (vieneDeSolicitud) {

                val posicionMaquinaria =
                    lista.indexOfFirst { maquinaria ->
                        maquinaria.uid == uidMaquinariaSolicitud
                    }

                if (posicionMaquinaria >= 0) {

                    binding.spMaquinaria.setSelection(posicionMaquinaria)

                    maquinariaSeleccionada =
                        lista[posicionMaquinaria]

                    binding.spMaquinaria.isEnabled = false
                    binding.spMaquinaria.isClickable = false

                    if (horometroSugerido > 0) {
                        binding.etHorometroProgramado.setText(
                            horometroSugerido.toString()
                        )
                    }

                } else {

                    Toast.makeText(
                        requireContext(),
                        "No se encontró la maquinaria asociada a la solicitud",
                        Toast.LENGTH_LONG
                    ).show()

                    binding.btnGuardarMantenimiento.isEnabled = false
                }

            } else {

                /*
                 * Flujo manual normal.
                 */
                maquinariaSeleccionada = lista.first()
            }

        }
        maquinariaViewModel.mensajeError.observe(viewLifecycleOwner){ mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }

        maquinariaViewModel.listarMaquinarias()
    }
    private fun cargarResponsableActual() {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            Toast.makeText(requireContext(), "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        database.collection("usuarios")
            .document(usuarioActual.uid)
            .get()
            .addOnSuccessListener { document ->

                val nombre = document.getString("nombre") ?: ""
                val correo = document.getString("correo") ?: usuarioActual.email.orEmpty()

                responsableActual = if (nombre.isNotEmpty()) nombre else correo

                binding.etResponsable.setText(responsableActual)

                binding.etResponsable.isFocusable = false
                binding.etResponsable.isClickable = false
                binding.etResponsable.isCursorVisible = false
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Error al cargar responsable",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun generarCodigoMantenimiento(): String {
        val fecha = SimpleDateFormat(
            "yyyyMMdd",
            Locale.getDefault()
        ).format(Date())

        val numeroAleatorio = (1000..9999).random()

        return "MAN-$fecha-$numeroAleatorio"
    }

    private fun obtenerFechaActual(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())
    }

    private fun observarMantenimientoViewModel() {

        mantenimientoViewModel.mensajeError.observe(
            viewLifecycleOwner
        ) { mensaje ->

            mostrarCargando(false)

            Toast.makeText(
                requireContext(),
                mensaje,
                Toast.LENGTH_SHORT
            ).show()
        }

        solicitudViewModel.mensajeError.observe(
            viewLifecycleOwner
        ) { mensaje ->

            mostrarCargando(false)

            Toast.makeText(
                requireContext(),
                mensaje,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarCargando(cargando : Boolean){

        binding.btnGuardarMantenimiento.isEnabled = !cargando

        binding.btnGuardarMantenimiento.text =
            if(cargando)
                "Guardando..."
            else
                "Programar mantenimiento"

        binding.progressGuardarMantenimiento.visibility =
            if(cargando)
                View.VISIBLE
            else
                View.GONE
    }


    private fun leerArgumentosSolicitud() {

        vieneDeSolicitud =
            arguments?.getString("origen") ==
                    "SOLICITUD_MANTENIMIENTO"

        uidSolicitud =
            arguments?.getString("uidSolicitud").orEmpty()

        uidMaquinariaSolicitud =
            arguments?.getString("uidMaquinaria").orEmpty()

        motivoSolicitud =
            arguments?.getString("motivoSolicitud").orEmpty()

        fechaSugerida =
            arguments?.getString("fechaSugerida").orEmpty()

        horometroSugerido =
            arguments?.getInt("horometroProgramado") ?: 0
    }

    private fun aplicarDatosInicialesSolicitud() {

        if (!vieneDeSolicitud) {
            return
        }

        binding.spTipoMantenimiento.setSelection(0)

        if (motivoSolicitud.isNotBlank()) {
            binding.etDescripcion.setText(motivoSolicitud)
        }

        if (fechaSugerida.isNotBlank()) {
            binding.etFechaProgramada.setText(fechaSugerida)
        }

        if (horometroSugerido > 0) {
            binding.etHorometroProgramado.setText(
                horometroSugerido.toString()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}