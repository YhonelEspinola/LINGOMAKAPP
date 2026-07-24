package com.lingomak.lingomakapp.ui.maquinaria

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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.databinding.FragmentEditarMaquinariaBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditarMaquinariaFragment : Fragment() {

    private var _binding: FragmentEditarMaquinariaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MaquinariaViewModel by viewModels()
    private var uid: String = ""
    private var codigoMaquinaria: String = ""
    private var imagenUrlActual: String = ""
    private var fechaRegistro: String = ""
    private var registradoPor: String = ""

    private var nuevaImagenUri: Uri? = null

    private val seleccionarImagenLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            if (uri != null) {
                nuevaImagenUri = uri

                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(binding.imgVistaPreviaMaquinaria)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEditarMaquinariaBinding.inflate(inflater, container, false)
        uid = arguments?.getString("uid") ?: ""

        configurarSpinnerEstado()
        configurarSpinnerTipoMaquinaria()
        configurarSpinnerMarca()
        configurarEventos()
        observarViewModel()

        if (uid.isNotEmpty()) {
            cargarDatosDesdeViewModel()
        }

        return binding.root
    }

    private fun cargarDatosDesdeViewModel() {
        viewModel.obtenerMaquinariaPorUid(uid).observe(viewLifecycleOwner) { maquinaria ->
            maquinaria?.let { 
                pintarDatos(it)
            }
        }
    }

    private fun pintarDatos(maquinaria: MaquinariaModel) {
        codigoMaquinaria = maquinaria.codigoMaquinaria
        imagenUrlActual = maquinaria.imagenUrl
        fechaRegistro = maquinaria.fechaRegistro
        registradoPor = maquinaria.registradoPor

        binding.etNombreMaquinaria.setText(maquinaria.nombre)
        binding.etModeloMaquinaria.setText(maquinaria.modelo)
        binding.etPlacaSerie.setText(maquinaria.placaSerie)
        binding.etAnioMaquinaria.setText(maquinaria.anio.toString())
        binding.etHorometroActual.setText(maquinaria.horometroActual.toString())
        binding.etHorometroUltimoMantenimiento.setText(maquinaria.horometroUltimoMantenimiento.toString())
        binding.etUbicacionActual.setText(maquinaria.ubicacionActual)
        binding.etIntervaloMantenimiento.setText(maquinaria.intervaloMantenimientoHoras.toString())
        binding.etObservacionesMaquinaria.setText(maquinaria.observaciones)

        val tiposAdapter = binding.spTipoMaquinaria.adapter
        for (i in 0 until tiposAdapter.count) {
            if (tiposAdapter.getItem(i).toString() == maquinaria.tipo) {
                binding.spTipoMaquinaria.setSelection(i)
                break
            }
        }

        val estadosAdapter = binding.spEstadoMaquinaria.adapter
        for (i in 0 until estadosAdapter.count) {
            if (estadosAdapter.getItem(i).toString() == maquinaria.estado) {
                binding.spEstadoMaquinaria.setSelection(i)
                break
            }
        }

        actualizarSpinnerMarcaParaTipo(maquinaria.tipo, maquinaria.marca)

        if (imagenUrlActual.isNotEmpty()) {
            Glide.with(this)
                .load(imagenUrlActual)
                .centerCrop()
                .placeholder(R.drawable.ic_maquinaria_placeholder)
                .error(R.drawable.ic_maquinaria_placeholder)
                .into(binding.imgVistaPreviaMaquinaria)
        } else {
            binding.imgVistaPreviaMaquinaria.setImageResource(R.drawable.ic_maquinaria_placeholder)
        }
    }

    private fun configurarSpinnerEstado() {
        val estados = listOf(
            "OPERATIVA",
            "EN_MANTENIMIENTO",
            "INACTIVA"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            estados
        )

        binding.spEstadoMaquinaria.adapter = adapter
    }

    private fun configurarSpinnerTipoMaquinaria() {
        val tiposMaquinaria = listOf(
            "Seleccione un tipo",
            "Excavadora",
            "Retroexcavadora",
            "Volquete",
            "Cargador Frontal",
            "Motoniveladora",
            "Rodillo Compactador",
            "Tractor Oruga",
            "Camión Cisterna",
            "Camión Grúa",
            "Minicargador",
            "Compresora",
            "Generador Eléctrico",
            "Otro"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            tiposMaquinaria
        )

        binding.spTipoMaquinaria.adapter = adapter
    }

    private fun actualizarSpinnerMarcaParaTipo(tipo: String, marcaASeleccionar: String) {
        val marcas = obtenerMarcasPorTipo(tipo)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, marcas)
        binding.spMarcaMaquinaria.adapter = adapter
        
        val pos = marcas.indexOf(marcaASeleccionar)
        if (pos >= 0) binding.spMarcaMaquinaria.setSelection(pos)
    }

    private fun obtenerMarcasPorTipo(tipo: String): List<String> {
        return when (tipo) {
            "Excavadora" -> listOf("Seleccione una marca", "CAT", "Komatsu", "Hitachi", "Volvo", "Hyundai", "Doosan")
            "Retroexcavadora" -> listOf("Seleccione una marca", "JCB", "CAT", "Case", "John Deere")
            "Volquete" -> listOf("Seleccione una marca", "Volvo", "Scania", "Mercedes-Benz", "MAN", "Iveco")
            "Cargador Frontal" -> listOf("Seleccione una marca", "CAT", "Komatsu", "Volvo", "John Deere")
            "Motoniveladora" -> listOf("Seleccione una marca", "CAT", "Komatsu", "John Deere")
            "Rodillo Compactador" -> listOf("Seleccione una marca", "Bomag", "Dynapac", "CAT")
            "Tractor Oruga" -> listOf("Seleccione una marca", "CAT", "Komatsu", "John Deere")
            "Camión Cisterna" -> listOf("Seleccione una marca", "Volvo", "Scania", "Mercedes-Benz")
            "Camión Grúa" -> listOf("Seleccione una marca", "Volvo", "Scania", "Mercedes-Benz")
            "Minicargador" -> listOf("Seleccione una marca", "Bobcat", "CAT", "JCB")
            "Compresora" -> listOf("Seleccione una marca", "Atlas Copco", "Sullair", "Kaeser")
            "Generador Eléctrico" -> listOf("Seleccione una marca", "Caterpillar", "Cummins", "Perkins")
            else -> listOf("Seleccione una marca")
        }
    }

    private fun configurarSpinnerMarca() {
        binding.spTipoMaquinaria.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val tipoSeleccionado = binding.spTipoMaquinaria.selectedItem.toString()
                    val marcas = obtenerMarcasPorTipo(tipoSeleccionado)

                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        marcas
                    )

                    binding.spMarcaMaquinaria.adapter = adapter
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun configurarEventos() {
        binding.btnSeleccionarImagen.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        binding.btnGuardarMaquinaria.setOnClickListener {
            validarFormulario()
        }
    }

    private fun observarViewModel() {
        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            mostrarCargando(false)
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun validarFormulario() {
        val nombre = binding.etNombreMaquinaria.text.toString().trim()
        val tipo = binding.spTipoMaquinaria.selectedItem.toString()
        val marca = binding.spMarcaMaquinaria.selectedItem.toString()
        val modelo = binding.etModeloMaquinaria.text.toString().trim()
        val placaSerie = binding.etPlacaSerie.text.toString().trim()
        val anioTexto = binding.etAnioMaquinaria.text.toString().trim()
        val estado = binding.spEstadoMaquinaria.selectedItem.toString()
        val horometroActualTexto = binding.etHorometroActual.text.toString().trim()
        val horometroUltimoTexto = binding.etHorometroUltimoMantenimiento.text.toString().trim()
        val ubicacion = binding.etUbicacionActual.text.toString().trim()
        val intervaloTexto = binding.etIntervaloMantenimiento.text.toString().trim()
        val observaciones = binding.etObservacionesMaquinaria.text.toString().trim()

        if (
            uid.isEmpty() ||
            nombre.isEmpty() ||
            modelo.isEmpty() ||
            placaSerie.isEmpty() ||
            anioTexto.isEmpty() ||
            horometroActualTexto.isEmpty() ||
            ubicacion.isEmpty() ||
            intervaloTexto.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (tipo == "Seleccione un tipo") {
            Toast.makeText(requireContext(), "Seleccione un tipo de maquinaria", Toast.LENGTH_SHORT).show()
            return
        }

        if (marca == "Seleccione una marca") {
            Toast.makeText(requireContext(), "Seleccione una marca", Toast.LENGTH_SHORT).show()
            return
        }

        val anio = anioTexto.toIntOrNull()
        val horometroActual = horometroActualTexto.toIntOrNull()
        val horometroUltimo = horometroUltimoTexto.toIntOrNull() ?: 0
        val intervalo = intervaloTexto.toIntOrNull()

        if (anio == null || anio <= 0) {
            Toast.makeText(requireContext(), "Ingrese un año válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (horometroActual == null || horometroActual < 0) {
            Toast.makeText(requireContext(), "Ingrese un horómetro válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (intervalo == null || intervalo <= 0) {
            Toast.makeText(requireContext(), "Ingrese un intervalo de mantenimiento válido", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCargando(true)

        actualizarMaquinaria(
            nombre = nombre,
            tipo = tipo,
            marca = marca,
            modelo = modelo,
            placaSerie = placaSerie,
            anio = anio,
            estado = estado,
            horometroActual = horometroActual,
            horometroUltimo = horometroUltimo,
            ubicacion = ubicacion,
            intervalo = intervalo,
            observaciones = observaciones
        )
    }

    private fun actualizarMaquinaria(
        nombre: String,
        tipo: String,
        marca: String,
        modelo: String,
        placaSerie: String,
        anio: Int,
        estado: String,
        horometroActual: Int,
        horometroUltimo: Int,
        ubicacion: String,
        intervalo: Int,
        observaciones: String
    ) {
        val nuevaImagen = nuevaImagenUri

        if (nuevaImagen != null) {

            viewModel.subirImagenMaquinaria(
                imagenUri = nuevaImagen,
                uid = uid,
                onSuccess = { nuevaImagenUrl ->
                    actualizarFirestore(
                        nombre, tipo, marca, modelo, placaSerie, anio, estado,
                        horometroActual, horometroUltimo, ubicacion, intervalo, observaciones,
                        nuevaImagenUrl
                    )
                }
            )
        } else {

            actualizarFirestore(
                nombre, tipo, marca, modelo, placaSerie, anio, estado,
                horometroActual, horometroUltimo, ubicacion, intervalo, observaciones,
                imagenUrlActual
            )
        }
    }

    private fun actualizarFirestore(
        nombre: String,
        tipo: String,
        marca: String,
        modelo: String,
        placaSerie: String,
        anio: Int,
        estado: String,
        horometroActual: Int,
        horometroUltimo: Int,
        ubicacion: String,
        intervalo: Int,
        observaciones: String,
        imagenUrl: String
    ) {
        val maquinariaActualizada = MaquinariaModel(
            uid = uid,
            codigoMaquinaria = codigoMaquinaria,
            nombre = nombre,
            tipo = tipo,
            marca = marca,
            modelo = modelo,
            placaSerie = placaSerie,
            anio = anio,
            estado = estado,
            horometroActual = horometroActual,
            horometroUltimoMantenimiento = horometroUltimo,
            intervaloMantenimientoHoras = intervalo,
            ubicacionActual = ubicacion,
            imagenUrl = imagenUrl,
            observaciones = observaciones,
            fechaRegistro = fechaRegistro,
            fechaActualizacion = obtenerFechaActual(),
            registradoPor = registradoPor
        )

        viewModel.actualizarMaquinaria(
            maquinaria = maquinariaActualizada,
            onSuccess = {
                mostrarCargando(false)

                Toast.makeText(
                    requireContext(),
                    "Maquinaria actualizada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()
            }
        )
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.btnGuardarMaquinaria.isEnabled = !cargando
        binding.btnGuardarMaquinaria.text =
            if(cargando)
                "Guardando..."
            else
                "Guardar cambios"

        binding.progressEditarMaquinaria.visibility =
            if(cargando)
                View.VISIBLE
            else
                View.GONE
    }

    private fun obtenerFechaActual(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
