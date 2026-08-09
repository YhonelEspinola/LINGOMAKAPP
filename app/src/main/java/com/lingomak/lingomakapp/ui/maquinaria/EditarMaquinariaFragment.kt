package com.lingomak.lingomakapp.ui.maquinaria

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
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
    private var maquinariaCargada: MaquinariaModel? = null

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
                maquinariaCargada = it
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
        binding.etMarcaMaquinaria.setText(maquinaria.marca)
        binding.etModeloMaquinaria.setText(maquinaria.modelo)
        binding.etPlacaSerie.setText(maquinaria.placaSerie)
        binding.etAnioMaquinaria.setText(maquinaria.anio.toString())
        binding.etHorometroActual.setText(maquinaria.horometroActual.toString())
        binding.etHorometroUltimoMantenimiento.setText(maquinaria.horometroUltimoMantenimiento.toString())
        binding.etCapacidadTanque.setText(maquinaria.capacidadTanqueGls?.toString() ?: "")
        binding.etUbicacionActual.setText(maquinaria.ubicacionActual)
        binding.etIntervaloMantenimiento.setText(maquinaria.intervaloMantenimientoHoras.toString())
        binding.etObservacionesMaquinaria.setText(maquinaria.observaciones)

        // Preseleccionar tipo si las categorías ya cargaron
        actualizarSeleccionTipo()

        binding.spEstadoMaquinaria.setText(maquinaria.estado, false)

        if (imagenUrlActual.isNotEmpty()) {
            Glide.with(this).load(imagenUrlActual).centerCrop()
                .placeholder(R.drawable.bg_image_placeholder)
                .error(R.drawable.bg_image_placeholder)
                .into(binding.imgVistaPreviaMaquinaria)
        }
    }

    private fun observarViewModel() {
        viewModel.categorias.observe(viewLifecycleOwner) { lista ->
            listaCategorias = listOf("Seleccione un tipo") + lista.map { it.nombre }
            configurarSpinnerTipoMaquinaria(listaCategorias)
            // Re-chequear selección cuando lleguen las categorías
            actualizarSeleccionTipo()
        }
        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            mostrarCargando(false)
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarSeleccionTipo() {
        val tipo = maquinariaCargada?.tipo ?: return
        val index = listaCategorias.indexOf(tipo)
        if (index >= 0) {
            binding.spTipoMaquinaria.setText(tipo, false)
        }
    }

    private fun configurarSpinnerTipoMaquinaria(categorias: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categorias)
        binding.spTipoMaquinaria.setAdapter(adapter)
    }

    private fun configurarSpinnerEstado() {
        val estados = listOf("OPERATIVA", "EN_MANTENIMIENTO", "INACTIVA")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estados)
        binding.spEstadoMaquinaria.setAdapter(adapter)
    }

    private fun configurarEventos() {
        binding.etHorometroActual.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))
        binding.etHorometroUltimoMantenimiento.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))
        binding.etCapacidadTanque.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))
        binding.etIntervaloMantenimiento.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))

        binding.btnSeleccionarImagen.setOnClickListener { seleccionarImagenLauncher.launch("image/*") }
        binding.btnGuardarMaquinaria.setOnClickListener { validarFormulario() }
    }

    private fun validarFormulario() {
        val nombre = binding.etNombreMaquinaria.text.toString().trim()
        val tipo = binding.spTipoMaquinaria.text.toString()
        val marca = binding.etMarcaMaquinaria.text.toString().trim()
        val modelo = binding.etModeloMaquinaria.text.toString().trim()
        val placaSerie = binding.etPlacaSerie.text.toString().trim()
        val anio = binding.etAnioMaquinaria.text.toString().toIntOrNull() ?: 0
        val horometroActual = binding.etHorometroActual.text.toString().toDoubleOrNull() ?: 0.0
        val horometroUltimo = binding.etHorometroUltimoMantenimiento.text.toString().toDoubleOrNull() ?: 0.0
        val capacidadTanque = binding.etCapacidadTanque.text.toString().toDoubleOrNull()
        val ubicacion = binding.etUbicacionActual.text.toString().trim()
        val intervalo = binding.etIntervaloMantenimiento.text.toString().toDoubleOrNull() ?: 250.0
        val observaciones = binding.etObservacionesMaquinaria.text.toString().trim()

        if (nombre.isEmpty() || tipo == "Seleccione un tipo" || tipo.isEmpty() || marca.isEmpty() || anio <= 0) {
            Toast.makeText(requireContext(), "Complete los campos obligatorios correctamente", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCargando(true)
        if (nuevaImagenUri != null) {
            viewModel.subirImagenMaquinaria(nuevaImagenUri!!, uid) { url ->
                enviarAFirestore(nombre, tipo, marca, modelo, placaSerie, anio, horometroActual, horometroUltimo, ubicacion, intervalo, observaciones, url, capacidadTanque)
            }
        } else {
            enviarAFirestore(nombre, tipo, marca, modelo, placaSerie, anio, horometroActual, horometroUltimo, ubicacion, intervalo, observaciones, imagenUrlActual, capacidadTanque)
        }
    }

    private fun enviarAFirestore(nombre: String, tipo: String, marca: String, modelo: String, placa: String, anio: Int, ha: Double, hu: Double, ubi: String, i: Double, obs: String, url: String, capacidad: Double?) {
        val maquinaria = MaquinariaModel(
            uid = uid, codigoMaquinaria = codigoMaquinaria, nombre = nombre, tipo = tipo, marca = marca,
            modelo = modelo, placaSerie = placa, anio = anio, estado = binding.spEstadoMaquinaria.text.toString(),
            horometroActual = ha, horometroUltimoMantenimiento = hu, capacidadTanqueGls = capacidad, intervaloMantenimientoHoras = i,
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
