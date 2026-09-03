package com.lingomak.lingomakapp.ui.maquinaria

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.RegistroUsoMaquinariaModel
import com.lingomak.lingomakapp.data.model.SuministroModel
import com.lingomak.lingomakapp.databinding.FragmentRegistrarUsoMaquinariaBinding
import com.lingomak.lingomakapp.utils.DateUtils
import com.lingomak.lingomakapp.utils.formatoHoras
import java.util.UUID

class RegistrarUsoMaquinariaFragment : Fragment() {

    private var _binding: FragmentRegistrarUsoMaquinariaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegistrarUsoMaquinariaViewModel by viewModels()

    // Datos Maquinaria
    private var uidMaquinaria = ""
    private var codigoMaquinaria = ""
    private var nombreMaquinaria = ""
    private var tipoMaquinaria = ""
    private var horometroActualMaquina = 0.0
    private var horometroUltimoMantenimiento = 0.0
    private var intervaloMantenimientoHoras = 250.0
    private var capacidadTanqueGls: Double? = null

    // Datos Edición (si aplica)
    private var esEdicion = false
    private var uidRegistroUso = ""
    private var horometroAnteriorTramo = 0.0
    private var horometroFinalOriginal = 0.0
    private var uidSuministroOriginal: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrarUsoMaquinariaBinding.inflate(inflater, container, false)

        cargarArgumentos()
        configurarSpinners()
        configurarColapsables()
        configurarEventos()
        observarViewModel()
        
        if (esEdicion) {
            cargarDatosEdicion()
        } else {
            binding.tvHorometroActual.text = "${horometroActualMaquina.formatoHoras()} h"
        }

        return binding.root
    }

    private fun cargarArgumentos() {
        val args = arguments ?: return
        uidMaquinaria = args.getString("uidMaquinaria") ?: ""
        codigoMaquinaria = args.getString("codigoMaquinaria") ?: ""
        nombreMaquinaria = args.getString("nombreMaquinaria") ?: ""
        tipoMaquinaria = args.getString("tipoMaquinaria") ?: ""
        
        horometroActualMaquina = args.getDouble("horometroActual", 0.0)
        horometroUltimoMantenimiento = args.getDouble("horometroUltimoMantenimiento", 0.0)
        intervaloMantenimientoHoras = args.getDouble("intervaloMantenimientoHoras", 250.0)
        
        if (args.containsKey("capacidadTanqueGls")) {
            capacidadTanqueGls = args.getDouble("capacidadTanqueGls")
        }

        uidRegistroUso = args.getString("uidRegistroUso") ?: ""
        esEdicion = uidRegistroUso.isNotEmpty()

        binding.tvNombreMaquinaria.text = nombreMaquinaria
        binding.tvCodigoMaquinaria.text = "Código: $codigoMaquinaria"
        binding.tvTipoMaquinaria.text = "Tipo: $tipoMaquinaria"
        binding.tvUltimoMantenimiento.text = "${horometroUltimoMantenimiento.formatoHoras()} h"
        binding.tvIntervaloMantenimiento.text = "Frecuencia: cada ${intervaloMantenimientoHoras.formatoHoras()} h"
        
        if (esEdicion) {
            binding.tvTituloFormulario.text = "Editar Actividad"
            binding.btnGuardarUso.text = "ACTUALIZAR REGISTRO"
        }
    }

    private fun configurarSpinners() {
        val tiposMov = listOf("Trabajo", "Traslado")
        binding.spTipoMovimiento.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tiposMov))

        val tiposCarga = listOf("Parcial", "Completa")
        binding.spTipoCarga.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tiposCarga))
    }

    private fun configurarColapsables() {
        binding.layoutHeaderRepostaje.setOnClickListener {
            val visible = binding.layoutContentRepostaje.visibility == View.VISIBLE
            binding.layoutContentRepostaje.visibility = if (visible) View.GONE else View.VISIBLE
            binding.ivChevronRepostaje.rotation = if (visible) 0f else 180f
        }

        binding.layoutHeaderProyecto.setOnClickListener {
            val visible = binding.layoutContentProyecto.visibility == View.VISIBLE
            binding.layoutContentProyecto.visibility = if (visible) View.GONE else View.VISIBLE
            binding.ivChevronProyecto.rotation = if (visible) 0f else 180f
        }
    }

    private fun configurarEventos() {
        binding.etHorometroFinal.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))
        binding.etGalonesCombustible.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))
        binding.etGalonesAceite.filters = arrayOf(com.lingomak.lingomakapp.utils.DecimalDigitsInputFilter(2))

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calcularVistaPrevia()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etHorometroFinal.addTextChangedListener(watcher)
        binding.etGalonesCombustible.addTextChangedListener(watcher)

        binding.btnGuardarUso.setOnClickListener {
            validarYGuardar()
        }
    }

    private fun cargarDatosEdicion() {
        val args = arguments ?: return
        horometroAnteriorTramo = args.getDouble("horometroAnterior", 0.0)
        horometroFinalOriginal = args.getDouble("horometroFinal", 0.0)
        
        binding.tvHorometroActual.text = "${horometroAnteriorTramo.formatoHoras()} h"
        binding.etHorometroFinal.setText(horometroFinalOriginal.toString())
        binding.etTrabajoRealizado.setText(args.getString("trabajoRealizado"))
        
        val tipoMov = args.getString("tipoMovimiento") ?: "Trabajo"
        binding.spTipoMovimiento.setText(tipoMov, false)

        binding.etObra.setText(args.getString("obra"))
        binding.etContratista.setText(args.getString("contratista"))
        binding.etUbicacion.setText(args.getString("ubicacion"))

        // Cargar Datos de Repostaje Unificados
        val galonesComb = args.getDouble("galonesCombustible", 0.0)
        val galonesAceite = args.getDouble("galonesAceite", 0.0)
        val tipoComb = args.getString("tipoCombustible") ?: ""
        val tipoCarga = args.getString("tipoCarga") ?: ""

        if (galonesComb > 0 || galonesAceite > 0) {
            binding.layoutContentRepostaje.visibility = View.VISIBLE
            binding.ivChevronRepostaje.rotation = 180f
            
            binding.etGalonesCombustible.setText(galonesComb.toString())
            binding.etGalonesAceite.setText(galonesAceite.toString())
            binding.spTipoCarga.setText(tipoCarga, false)
            binding.spTipoCombustible.setText(tipoComb, false)
        }
    }

    private fun observarViewModel() {
        viewModel.cargando.observe(viewLifecycleOwner) { binding.progressUsoMaquinaria.visibility = if (it) View.VISIBLE else View.GONE }
        
        viewModel.mensajeError.observe(viewLifecycleOwner) { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }

        viewModel.registroExitoso.observe(viewLifecycleOwner) { if (it) { 
            Toast.makeText(requireContext(), "Registro guardado correctamente", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }}

        viewModel.tiposCombustible.observe(viewLifecycleOwner) { tipos ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos)
            binding.spTipoCombustible.setAdapter(adapter)
            
            viewModel.suministroExistente.value?.let { s ->
                binding.spTipoCombustible.setText(s.tipoCombustible, false)
            }
        }

        viewModel.suministroExistente.observe(viewLifecycleOwner) { suministro ->
            if (suministro != null) {
                uidSuministroOriginal = suministro.uid
                binding.layoutContentRepostaje.visibility = View.VISIBLE
                binding.ivChevronRepostaje.rotation = 180f
                
                binding.etGalonesCombustible.setText(suministro.galonesCombustible.toString())
                binding.etGalonesAceite.setText(suministro.galonesAceite.toString())
                
                binding.spTipoCarga.setText(suministro.tipoCarga, false)
                
                val tipos = viewModel.tiposCombustible.value
                if (tipos != null) {
                    binding.spTipoCombustible.setText(suministro.tipoCombustible, false)
                }
            }
        }
    }

    private fun calcularVistaPrevia() {
        val horometroAnterior = if (esEdicion) horometroAnteriorTramo else horometroActualMaquina
        val horometroFinal = binding.etHorometroFinal.text.toString().toDoubleOrNull() ?: 0.0
        
        val horasUso = if (horometroFinal > horometroAnterior) horometroFinal - horometroAnterior else 0.0
        binding.tvHorasCalculadas.text = "${horasUso.formatoHoras()} h"

        if (horometroFinal > 0) {
            val horasDesdeUltimo = horometroFinal - horometroUltimoMantenimiento
            val horasRestantes = intervaloMantenimientoHoras - horasDesdeUltimo
            binding.tvHorasRestantes.text = "${horasRestantes.formatoHoras()} h"

            when {
                horasRestantes <= 0 -> {
                    binding.tvMensajeMantenimiento.text = "🔴 Se superó el límite. Se enviará solicitud de mantenimiento."
                    binding.tvMensajeMantenimiento.setTextColor(Color.RED)
                }
                horasRestantes <= 20 -> {
                    binding.tvMensajeMantenimiento.text = "🟡 Próxima a mantenimiento (faltan ${horasRestantes.formatoHoras()} h)"
                    binding.tvMensajeMantenimiento.setTextColor(Color.parseColor("#FFA500"))
                }
                else -> {
                    binding.tvMensajeMantenimiento.text = "🟢 Maquinaria en estado normal."
                    binding.tvMensajeMantenimiento.setTextColor(Color.parseColor("#2E7D32"))
                }
            }
        }

        val combustible = binding.etGalonesCombustible.text.toString().toDoubleOrNull() ?: 0.0
        if (capacidadTanqueGls != null && capacidadTanqueGls!! > 0) {
            val porc = (combustible / capacidadTanqueGls!!) * 100
            binding.tvPorcentajeTanque.text = "${porc.formatoHoras()}%"
            if (porc > 100 && binding.spTipoCarga.text.toString() == "Completa") {
                binding.tvPorcentajeTanque.setTextColor(Color.RED)
            } else {
                binding.tvPorcentajeTanque.setTextColor(Color.GRAY)
            }
        } else {
            binding.tvPorcentajeTanque.text = "N/A"
            if (combustible > 0) {
                binding.tvMensajeMantenimiento.text = "Aviso: Capacidad del tanque no configurada."
                binding.tvMensajeMantenimiento.setTextColor(Color.GRAY)
            }
        }
    }

    private fun validarYGuardar() {
        val horometroAnterior = if (esEdicion) horometroAnteriorTramo else horometroActualMaquina
        val horometroFinal = binding.etHorometroFinal.text.toString().toDoubleOrNull()
        val trabajo = binding.etTrabajoRealizado.text.toString().trim()
        
        if (horometroFinal == null || horometroFinal <= horometroAnterior) {
            Toast.makeText(requireContext(), "El horómetro final debe ser mayor a $horometroAnterior", Toast.LENGTH_SHORT).show()
            return
        }

        if (trabajo.isEmpty()) {
            Toast.makeText(requireContext(), "El trabajo realizado es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        val galonesComb = binding.etGalonesCombustible.text.toString().toDoubleOrNull() ?: 0.0
        val galonesAceite = binding.etGalonesAceite.text.toString().toDoubleOrNull() ?: 0.0
        val tipoCarga = binding.spTipoCarga.text.toString()
        
        if (tipoCarga == "Completa" && capacidadTanqueGls != null && galonesComb > capacidadTanqueGls!!) {
            Toast.makeText(requireContext(), "El combustible excede la capacidad del tanque (${capacidadTanqueGls} Gls)", Toast.LENGTH_SHORT).show()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        val horasBrutas = horometroFinal - horometroAnterior
        val horasUso = if (horometroFinal > horometroAnterior) {
            // Redondear a 2 decimales antes de guardar
            (Math.round(horasBrutas * 100.0) / 100.0)
        } else 0.0

        val registroUso = RegistroUsoMaquinariaModel(
            uid = if (esEdicion) uidRegistroUso else "",
            uidMaquinaria = uidMaquinaria,
            codigoMaquinaria = codigoMaquinaria,
            nombreMaquinaria = nombreMaquinaria,
            tipoMaquinaria = tipoMaquinaria,
            uidOperario = user?.uid ?: "",
            nombreOperario = user?.displayName ?: "Operario",
            correoOperario = user?.email ?: "",
            fechaUso = DateUtils.obtenerFechaActual(),
            horometroAnterior = horometroAnterior,
            horometroFinal = horometroFinal,
            horasUso = horasUso,
            trabajoRealizado = trabajo,
            fechaRegistro = if (esEdicion) (arguments?.getString("fechaRegistro") ?: DateUtils.obtenerFechaActual()) else DateUtils.obtenerFechaActual(),
            tipoMovimiento = binding.spTipoMovimiento.text.toString(),
            obra = binding.etObra.text.toString().trim().takeIf { it.isNotEmpty() },
            contratista = binding.etContratista.text.toString().trim().takeIf { it.isNotEmpty() },
            ubicacion = binding.etUbicacion.text.toString().trim().takeIf { it.isNotEmpty() },
            galonesCombustible = galonesComb,
            galonesAceite = galonesAceite,
            tipoCombustible = binding.spTipoCombustible.text.toString().ifEmpty { "Diesel" },
            tipoCarga = tipoCarga
        )

        viewModel.registrarUsoMaquinaria(
            registroUso = registroUso,
            suministro = null, // Ya no necesitamos enviarlo aparte, va en el modelo
            esEdicion = esEdicion,
            horometroFinalOriginal = if (esEdicion) horometroFinalOriginal else null
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
