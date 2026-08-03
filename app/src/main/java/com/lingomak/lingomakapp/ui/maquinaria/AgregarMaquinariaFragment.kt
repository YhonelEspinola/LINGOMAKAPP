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
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.databinding.FragmentAgregarMaquinariaBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AgregarMaquinariaFragment : Fragment() {

    private var _binding: FragmentAgregarMaquinariaBinding? = null
    private val binding get() = _binding!!

    private val viewModel : MaquinariaViewModel by viewModels()

    private var imagenSeleccionadaUri : Uri? = null
    private var listaCategorias: List<String> = emptyList()

    private val seleccionadarImagenLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()){ uri ->
            if (uri != null){
                imagenSeleccionadaUri = uri
                binding.imgVistaPreviaMaquinaria.visibility = View.VISIBLE
                binding.layoutPlaceholderMaquinaria.visibility = View.GONE
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
        _binding = FragmentAgregarMaquinariaBinding.inflate(inflater, container,false)

        configurarSpinnerEstado()
        configurarEventos()
        observarViewModel()

        return binding.root
    }

    private fun configurarSpinnerEstado(){
        val estados = listOf(
            "OPERATIVA",
            "EN_MANTENIMIENTO",
            "INACTIVA"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            estados
        )
        binding.spEstadoMaquinaria.setAdapter(adapter)
    }

    private fun configurarEventos(){
        binding.etHorometroActual.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))
        binding.etHorometroUltimoMantenimiento.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))
        binding.etCapacidadTanque.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))

        binding.cardSubirImagenMaquinaria.setOnClickListener {
            seleccionadarImagenLauncher.launch("image/*")
        }

        binding.btnGuardarMaquinaria.setOnClickListener {
            validarFormulario()
        }
    }

    private fun observarViewModel(){
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
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            listaCategorias
        )
        binding.spTipoMaquinaria.setAdapter(adapter)
    }

    private fun validarFormulario(){
        val codigo = generarCodigoMaquinaria(binding.etNombreMaquinaria.text.toString().trim())
        val nombre = binding.etNombreMaquinaria.text.toString().trim()
        val tipo = binding.spTipoMaquinaria.text.toString()
        val marca = binding.etMarcaMaquinaria.text.toString().trim()
        val modelo = binding.etModeloMaquinaria.text.toString().trim()
        val placaSerie = binding.etPlacaSerie.text.toString().trim()
        val anioTexto = binding.etAnioMaquinaria.text.toString().trim()
        val estado = binding.spEstadoMaquinaria.text.toString()
        val horometroActualTexto = binding.etHorometroActual.text.toString().trim()
        val horometroUltimoTexto = binding.etHorometroUltimoMantenimiento.text.toString().trim()
        val ubicacion = binding.etUbicacionActual.text.toString().trim()
        val intervaloTexto = binding.etIntervaloMantenimiento.text.toString().trim()
        val observaciones = binding.etObservacionesMaquinaria.text.toString().trim()
        
        val capacidadTanqueTexto = binding.etCapacidadTanque.text.toString().trim()


        if (
            nombre.isEmpty() ||
            tipo.isEmpty() ||
            marca.isEmpty() ||
            modelo.isEmpty() ||
            placaSerie.isEmpty() ||
            anioTexto.isEmpty() ||
            horometroActualTexto.isEmpty() ||
            ubicacion.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (tipo == "Seleccione un tipo" || tipo.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Seleccione un tipo de maquinaria",
                Toast.LENGTH_SHORT
            ).show()

            mostrarCargando(false)
            return
        }

        val anio = anioTexto.toIntOrNull()
        val horometroActual = horometroActualTexto.toDoubleOrNull()
        val horometroUltimo = horometroUltimoTexto.toDoubleOrNull() ?: 0.0
        val intervaloMantenimiento = intervaloTexto.toIntOrNull() ?: 250
        
        val capacidadTanque = capacidadTanqueTexto.toDoubleOrNull()

        if (anio == null || anio <= 0) {
            Toast.makeText(requireContext(), "Ingrese un año válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (horometroActual == null || horometroActual < 0) {
            Toast.makeText(requireContext(), "Ingrese un horómetro válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (intervaloMantenimiento <= 0) {
            Toast.makeText(requireContext(), "El intervalo de mantenimiento debe ser un número positivo", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCargando(true)

        guardarMaquinaria(
            codigo = codigo,
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
            intervalo = intervaloMantenimiento,
            observaciones = observaciones,
            capacidadTanque = capacidadTanque
        )
    }

    private fun guardarMaquinaria(
        codigo: String,
        nombre: String,
        tipo: String,
        marca: String,
        modelo: String,
        placaSerie: String,
        anio: Int,
        estado: String,
        horometroActual: Double,
        horometroUltimo: Double,
        ubicacion: String,
        intervalo: Int,
        observaciones: String,
        capacidadTanque: Double?
    ){
        val uid = UUID.randomUUID().toString()
        val fechaActual = obtenerFechaActual()
        val registradoPor = FirebaseAuth.getInstance().currentUser?.email ?: "ADMIN"

        val imagenUri = imagenSeleccionadaUri

        if (imagenUri != null) {
            viewModel.subirImagenMaquinaria(
                imagenUri = imagenUri,
                uid = uid,
                onSuccess = { imagenUrl ->
                    registrarMaquinariaEnFirestore(
                        uid = uid,
                        codigo = codigo,
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
                        observaciones = observaciones,
                        imagenUrl = imagenUrl,
                        fechaActual = fechaActual,
                        registradoPor = registradoPor,
                        capacidadTanque = capacidadTanque
                    )
                }
            )
        } else {
            registrarMaquinariaEnFirestore(
                uid = uid,
                codigo = codigo,
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
                observaciones = observaciones,
                imagenUrl = "",
                fechaActual = fechaActual,
                registradoPor = registradoPor,
                capacidadTanque = capacidadTanque
            )
        }
    }

    private fun registrarMaquinariaEnFirestore(
        uid: String,
        codigo: String,
        nombre: String,
        tipo: String,
        marca: String,
        modelo: String,
        placaSerie: String,
        anio: Int,
        estado: String,
        horometroActual: Double,
        horometroUltimo: Double,
        ubicacion: String,
        intervalo: Int,
        observaciones: String,
        imagenUrl: String,
        fechaActual: String,
        registradoPor: String,
        capacidadTanque: Double?
    ) {
        val maquinaria = MaquinariaModel(
            uid = uid,
            codigoMaquinaria = codigo,
            nombre = nombre,
            tipo = tipo,
            marca = marca,
            modelo = modelo,
            placaSerie = placaSerie,
            anio = anio,
            estado = estado,
            horometroActual = horometroActual,
            horometroUltimoMantenimiento = horometroUltimo,
            capacidadTanqueGls = capacidadTanque,
            intervaloMantenimientoHoras = intervalo,
            ubicacionActual = ubicacion,
            imagenUrl = imagenUrl,
            observaciones = observaciones,
            fechaRegistro = fechaActual,
            fechaActualizacion = fechaActual,
            registradoPor = registradoPor
        )

        viewModel.agregarMaquinaria(
            maquinaria = maquinaria,
            onSuccess = {
                mostrarCargando(false)
                Toast.makeText(
                    requireContext(),
                    "Maquinaria registrada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()
            }
        )
    }

    private fun obtenerFechaActual(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())
    }

    private fun mostrarCargando(cargando : Boolean){
        binding.btnGuardarMaquinaria.isEnabled = !cargando

        binding.btnGuardarMaquinaria.text =
            if(cargando) "Guardando..." else "Guardar maquinaria"

        binding.progressGuardarMaquinaria.visibility =
            if(cargando) View.VISIBLE else View.GONE
    }

    private fun generarCodigoMaquinaria(nombre: String): String {

        val prefijo = nombre
            .trim()
            .replace(" ", "")
            .uppercase()
            .take(3)
            .padEnd(3, 'X')
        val fecha = SimpleDateFormat(
            "yyyyMMdd",
            Locale.getDefault()
        ).format(Date())

        val numeroAleatorio = (1000..9999).random()

        return "$prefijo-$fecha-$numeroAleatorio"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
