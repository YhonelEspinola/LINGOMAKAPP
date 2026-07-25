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

    private var listaCategorias: List<String> = emptyList()
    private var nuevaImagenUri: Uri? = null

    private val seleccionarImagenLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                nuevaImagenUri = uri
                Glide.with(this).load(uri).centerCrop().into(binding.imgVistaPreviaMaquinaria)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditarMaquinariaBinding.inflate(inflater, container, false)
        uid = arguments?.getString("uid") ?: ""

        configurarSpinnerEstado()
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
            maquinaria?.let { pintarDatos(it) }
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

        val indexCat = listaCategorias.indexOf(maquinaria.tipo)
        if (indexCat >= 0) binding.spTipoMaquinaria.setSelection(indexCat)

        val estadosAdapter = binding.spEstadoMaquinaria.adapter
        for (i in 0 until estadosAdapter.count) {
            if (estadosAdapter.getItem(i).toString() == maquinaria.estado) {
                binding.spEstadoMaquinaria.setSelection(i)
                break
            }
        }

        actualizarSpinnerMarcaParaTipo(maquinaria.tipo, maquinaria.marca)

        if (imagenUrlActual.isNotEmpty()) {
            Glide.with(this).load(imagenUrlActual).centerCrop()
                .placeholder(R.drawable.ic_maquinaria_placeholder)
                .error(R.drawable.ic_maquinaria_placeholder)
                .into(binding.imgVistaPreviaMaquinaria)
        }
    }

    private fun observarViewModel() {
        viewModel.categorias.observe(viewLifecycleOwner) { lista ->
            configurarSpinnerTipoMaquinaria(lista.map { it.nombre })
        }
        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            mostrarCargando(false)
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarSpinnerTipoMaquinaria(categorias: List<String>) {
        listaCategorias = listOf("Seleccione un tipo") + categorias
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listaCategorias)
        binding.spTipoMaquinaria.adapter = adapter
    }

    private fun configurarSpinnerEstado() {
        val estados = listOf("OPERATIVA", "EN_MANTENIMIENTO", "INACTIVA")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, estados)
        binding.spEstadoMaquinaria.adapter = adapter
    }

    private fun configurarSpinnerMarca() {
        binding.spTipoMaquinaria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val tipo = binding.spTipoMaquinaria.selectedItem.toString()
                actualizarSpinnerMarcaParaTipo(tipo, "")
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun actualizarSpinnerMarcaParaTipo(tipo: String, seleccionada: String) {
        val marcas = when (tipo) {
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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, marcas)
        binding.spMarcaMaquinaria.adapter = adapter
        if (seleccionada.isNotEmpty()) {
            val pos = marcas.indexOf(seleccionada)
            if (pos >= 0) binding.spMarcaMaquinaria.setSelection(pos)
        }
    }

    private fun configurarEventos() {
        binding.btnSeleccionarImagen.setOnClickListener { seleccionarImagenLauncher.launch("image/*") }
        binding.btnGuardarMaquinaria.setOnClickListener { validarFormulario() }
    }

    private fun validarFormulario() {
        val nombre = binding.etNombreMaquinaria.text.toString().trim()
        val tipo = binding.spTipoMaquinaria.selectedItem.toString()
        val marca = binding.spMarcaMaquinaria.selectedItem.toString()
        val modelo = binding.etModeloMaquinaria.text.toString().trim()
        val placaSerie = binding.etPlacaSerie.text.toString().trim()
        val anio = binding.etAnioMaquinaria.text.toString().toIntOrNull() ?: 0
        val horometroActual = binding.etHorometroActual.text.toString().toIntOrNull() ?: 0
        val horometroUltimo = binding.etHorometroUltimoMantenimiento.text.toString().toIntOrNull() ?: 0
        val ubicacion = binding.etUbicacionActual.text.toString().trim()
        val intervalo = binding.etIntervaloMantenimiento.text.toString().toIntOrNull() ?: 250
        val observaciones = binding.etObservacionesMaquinaria.text.toString().trim()

        if (nombre.isEmpty() || tipo == "Seleccione un tipo" || marca == "Seleccione una marca" || anio <= 0) {
            Toast.makeText(requireContext(), "Complete los campos obligatorios correctamente", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCargando(true)
        if (nuevaImagenUri != null) {
            viewModel.subirImagenMaquinaria(nuevaImagenUri!!, uid) { url ->
                enviarAFirestore(nombre, tipo, marca, modelo, placaSerie, anio, horometroActual, horometroUltimo, ubicacion, intervalo, observaciones, url)
            }
        } else {
            enviarAFirestore(nombre, tipo, marca, modelo, placaSerie, anio, horometroActual, horometroUltimo, ubicacion, intervalo, observaciones, imagenUrlActual)
        }
    }

    private fun enviarAFirestore(nombre: String, tipo: String, marca: String, modelo: String, placa: String, anio: Int, ha: Int, hu: Int, ubi: String, i: Int, obs: String, url: String) {
        val maquinaria = MaquinariaModel(
            uid = uid, codigoMaquinaria = codigoMaquinaria, nombre = nombre, tipo = tipo, marca = marca,
            modelo = modelo, placaSerie = placa, anio = anio, estado = binding.spEstadoMaquinaria.selectedItem.toString(),
            horometroActual = ha, horometroUltimoMantenimiento = hu, intervaloMantenimientoHoras = i,
            ubicacionActual = ubi, imagenUrl = url, observaciones = obs, fechaRegistro = fechaRegistro,
            fechaActualizacion = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            registradoPor = registradoPor
        )
        viewModel.actualizarMaquinaria(maquinaria) {
            mostrarCargando(false)
            Toast.makeText(requireContext(), "Maquinaria actualizada", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.btnGuardarMaquinaria.isEnabled = !cargando
        binding.btnGuardarMaquinaria.text = if (cargando) "Guardando..." else "Guardar cambios"
        binding.progressEditarMaquinaria.visibility = if (cargando) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
