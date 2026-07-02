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

        configurarSpinnerEstado()
        configurarSpinnerTipoMaquinaria()
        configurarSpinnerMarca()
        cargarDatosRecibidos()
        configurarEventos()
        observarViewModel()

        return binding.root
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

                    val marcas = when (tipoSeleccionado) {
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

                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        marcas
                    )

                    binding.spMarcaMaquinaria.adapter = adapter

                    val marcaActual = arguments?.getString("marca") ?: ""
                    val posicionMarca = marcas.indexOf(marcaActual)

                    if (posicionMarca >= 0) {
                        binding.spMarcaMaquinaria.setSelection(posicionMarca)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun cargarDatosRecibidos() {
        uid = arguments?.getString("uid") ?: ""
        codigoMaquinaria = arguments?.getString("codigoMaquinaria") ?: ""
        imagenUrlActual = arguments?.getString("imagenUrl") ?: ""
        fechaRegistro = arguments?.getString("fechaRegistro") ?: obtenerFechaActual()
        registradoPor = arguments?.getString("registradoPor") ?: "ADMIN"

        val nombre = arguments?.getString("nombre") ?: ""
        val tipo = arguments?.getString("tipo") ?: ""
        val modelo = arguments?.getString("modelo") ?: ""
        val placaSerie = arguments?.getString("placaSerie") ?: ""
        val anio = arguments?.getInt("anio") ?: 0
        val estado = arguments?.getString("estado") ?: ""
        val horometroActual = arguments?.getInt("horometroActual") ?: 0
        val horometroUltimo = arguments?.getInt("horometroUltimoMantenimiento") ?: 0
        val ubicacion = arguments?.getString("ubicacionActual") ?: ""
        val observaciones = arguments?.getString("observaciones") ?: ""

        binding.etNombreMaquinaria.setText(nombre)
        binding.etModeloMaquinaria.setText(modelo)
        binding.etPlacaSerie.setText(placaSerie)
        binding.etAnioMaquinaria.setText(anio.toString())
        binding.etHorometroActual.setText(horometroActual.toString())
        binding.etHorometroUltimoMantenimiento.setText(horometroUltimo.toString())
        binding.etUbicacionActual.setText(ubicacion)
        binding.etObservacionesMaquinaria.setText(observaciones)


        val tiposAdapter = binding.spTipoMaquinaria.adapter
        for (i in 0 until tiposAdapter.count) {
            if (tiposAdapter.getItem(i).toString() == tipo) {
                binding.spTipoMaquinaria.setSelection(i)
                break
            }
        }


        val estadosAdapter = binding.spEstadoMaquinaria.adapter
        for (i in 0 until estadosAdapter.count) {
            if (estadosAdapter.getItem(i).toString() == estado) {
                binding.spEstadoMaquinaria.setSelection(i)
                break
            }
        }

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
        val observaciones = binding.etObservacionesMaquinaria.text.toString().trim()

        if (
            uid.isEmpty() ||
            nombre.isEmpty() ||
            modelo.isEmpty() ||
            placaSerie.isEmpty() ||
            anioTexto.isEmpty() ||
            horometroActualTexto.isEmpty() ||
            ubicacion.isEmpty()
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

        if (anio == null || anio <= 0) {
            Toast.makeText(requireContext(), "Ingrese un año válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (horometroActual == null || horometroActual < 0) {
            Toast.makeText(requireContext(), "Ingrese un horómetro válido", Toast.LENGTH_SHORT).show()
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
                        horometroActual, horometroUltimo, ubicacion, observaciones,
                        nuevaImagenUrl
                    )
                }
            )
        } else {

            actualizarFirestore(
                nombre, tipo, marca, modelo, placaSerie, anio, estado,
                horometroActual, horometroUltimo, ubicacion, observaciones,
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
        observaciones: String,
        imagenUrl: String
    ) {
        val actualizadoPor = FirebaseAuth.getInstance().currentUser?.email ?: "ADMIN"

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