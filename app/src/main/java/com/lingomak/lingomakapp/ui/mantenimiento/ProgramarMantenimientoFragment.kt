package com.lingomak.lingomakapp.ui.mantenimiento

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.data.repository.UserRepository
import com.lingomak.lingomakapp.databinding.FragmentProgramarMantenimientoBinding
import com.lingomak.lingomakapp.ui.maquinaria.MaquinariaViewModel
import java.text.SimpleDateFormat
import java.util.*

class ProgramarMantenimientoFragment : Fragment() {

    private var _binding: FragmentProgramarMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val maquinariaViewModel: MaquinariaViewModel by viewModels()
    private val mantenimientoViewModel: MantenimientoViewModel by viewModels()
    private val userRepository = UserRepository()

    private var listaMaquinarias: List<MaquinariaModel> = emptyList()
    private var listaOperarios: List<UserModel> = emptyList()

    private var maquinariaSeleccionada: MaquinariaModel? = null
    private var operarioSeleccionado: UserModel? = null

    private val imagenesReporteLocal: MutableList<String> = mutableListOf()
    private lateinit var adapterImagenes: EvidenciasAdapter

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { agregarImagenALista(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgramarMantenimientoBinding.inflate(inflater, container, false)

        configurarRecyclerView()
        configurarSpinners()
        configurarEventos()
        observarMantenimientoViewModel()
        cargarMaquinaria()
        cargarOperarios()

        return binding.root
    }

    private fun configurarRecyclerView() {
        adapterImagenes = EvidenciasAdapter(imagenesReporteLocal) { posicion ->
            imagenesReporteLocal.removeAt(posicion)
            adapterImagenes.notifyDataSetChanged()
            actualizarVisibilidadBotonAgregar()
        }
        binding.rvImagenesReporte.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvImagenesReporte.adapter = adapterImagenes
    }

    private fun agregarImagenALista(uri: Uri) {
        imagenesReporteLocal.add(uri.toString())
        adapterImagenes.notifyDataSetChanged()
        actualizarVisibilidadBotonAgregar()
    }

    private fun actualizarVisibilidadBotonAgregar() {
        binding.cardSubirImagenReferencia.visibility =
            if (imagenesReporteLocal.size >= 3) View.GONE else View.VISIBLE
    }

    private fun configurarSpinners() {
        val tipos = listOf("PREVENTIVO", "CORRECTIVO", "PREDICTIVO")
        binding.spTipoMantenimiento.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            tipos
        )
    }

    private fun cargarOperarios() {
        userRepository.listarOperarios({ usuarios ->
            listaOperarios = usuarios.filter { it.estado == "ACTIVO" }
            val nombres = mutableListOf("Seleccione responsable")
            nombres.addAll(listaOperarios.map { it.nombre })
            
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, nombres)
            binding.spResponsable.adapter = adapter
        }, { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        })
    }

    private fun configurarEventos() {
        binding.etFechaProgramada.setOnClickListener { mostrarDatePicker() }
        binding.cardSubirImagenReferencia.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnGuardarMantenimiento.setOnClickListener { validarFormulario() }

        binding.spMaquinaria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (position > 0) {
                    maquinariaSeleccionada = listaMaquinarias[position - 1]
                    binding.etHorometroProgramado.setText(maquinariaSeleccionada?.horometroActual.toString())
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spResponsable.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (position > 0) {
                    operarioSeleccionado = listaOperarios[position - 1]
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun mostrarDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Seleccionar fecha")
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            val date = Date(selection)
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.etFechaProgramada.setText(format.format(date))
        }
        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun obtenerPrioridadSeleccionada(): String {
        return when (binding.rgPrioridad.checkedRadioButtonId) {
            R.id.rbPrioridadAlta -> "ALTA"
            R.id.rbPrioridadMedia -> "MEDIA"
            R.id.rbPrioridadBaja -> "BAJA"
            else -> "MEDIA"
        }
    }

    private fun validarFormulario() {
        val desc = binding.etDescripcion.text.toString().trim()
        val fecha = binding.etFechaProgramada.text.toString().trim()
        val horometroStr = binding.etHorometroProgramado.text.toString().trim()
        
        if (maquinariaSeleccionada == null || operarioSeleccionado == null || desc.isEmpty() || fecha.isEmpty() || horometroStr.isEmpty()) {
            Toast.makeText(requireContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val m = MantenimientoModel(
            uid = UUID.randomUUID().toString(),
            codigoMantenimiento = generarCodigoMantenimiento(),
            uidMaquinaria = maquinariaSeleccionada!!.uid,
            codigoMaquinaria = maquinariaSeleccionada!!.codigoMaquinaria,
            nombreMaquinaria = maquinariaSeleccionada!!.nombre,
            tipoMaquinaria = maquinariaSeleccionada!!.tipo,
            tipoMantenimiento = binding.spTipoMantenimiento.selectedItem.toString(),
            descripcion = desc,
            fechaProgramada = fecha,
            estado = "PENDIENTE",
            prioridad = obtenerPrioridadSeleccionada(),
            responsable = operarioSeleccionado!!.nombre,
            responsableUid = operarioSeleccionado!!.uid,
            horometroProgramado = horometroStr.toIntOrNull() ?: 0,
            costoEstimado = binding.etCostoEstimado.text.toString().toDoubleOrNull() ?: 0.0,
            observaciones = binding.etObservaciones.text.toString().trim(),
            fechaRegistro = obtenerFechaActual(),
            imagenesReporteLocal = imagenesReporteLocal
        )

        mostrarCargando(true)
        mantenimientoViewModel.agregarMantenimiento(m) {
            Toast.makeText(requireContext(), "Mantenimiento programado", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun generarCodigoMantenimiento(): String {
        val ts = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "MAN-$ts-$random"
    }

    private fun obtenerFechaActual(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun observarMantenimientoViewModel() {
        mantenimientoViewModel.mensajeError.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                mostrarCargando(false)
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarCargando(show: Boolean) {
        binding.progressGuardarMantenimiento.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnGuardarMantenimiento.isEnabled = !show
    }

    private fun cargarMaquinaria() {
        maquinariaViewModel.listaMaquinarias.observe(viewLifecycleOwner) { maquinarias ->
            listaMaquinarias = maquinarias
            val nombres = mutableListOf("Seleccione maquinaria")
            nombres.addAll(maquinarias.map { "${it.nombre} (${it.codigoMaquinaria})" })
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, nombres)
            binding.spMaquinaria.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
