package com.lingomak.lingomakapp.ui.maquinaria

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.BitacoraUsoModel
import com.lingomak.lingomakapp.databinding.FragmentDetalleBitacoraBinding

import com.lingomak.lingomakapp.utils.formatoHoras

class DetalleBitacoraFragment : Fragment() {

    private var _binding: FragmentDetalleBitacoraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BitacoraUsoViewModel by viewModels()
    private var registroUid: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleBitacoraBinding.inflate(inflater, container, false)
        
        registroUid = arguments?.getString("registro_uid") ?: ""
        
        observarViewModel()
        
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (registroUid.isNotEmpty()) {
            viewModel.cargarRegistroPorUid(registroUid)
        }
    }

    private fun observarViewModel() {
        viewModel.registroSeleccionado.observe(viewLifecycleOwner) { model ->
            if (model != null) {
                pintarDatos(model)
            }
        }
    }

    private fun pintarDatos(m: BitacoraUsoModel) {
        binding.tvFechaDetalle.text = m.fecha
        binding.tvNombreMaquinaDetalle.text = m.nombreMaquinaria
        binding.tvDetalleMaquina.text = "${m.marcaMaquinaria} · ${m.modeloMaquinaria} · ${m.codigoMaquinaria}"
        binding.tvTrabajoRealizadoDetalle.text = m.trabajoRealizado
        binding.tvHorometroInicialDetalle.text = "${m.horometroAnterior.formatoHoras()} h"
        binding.tvHorometroFinalDetalle.text = "${m.horometroFinal.formatoHoras()} h"
        binding.tvHorasTotalDetalle.text = "Total: ${m.horasUso.formatoHoras()} horas de uso"
        
        binding.tvOperarioDetalle.text = "Registrado por: ${m.operarioNombre}"
        
        if (m.registroUso.modificadoPorUid != null) {
            binding.tvAuditoriaDetalle.visibility = View.VISIBLE
            binding.tvAuditoriaDetalle.text = "Última modificación: ${m.registroUso.fechaUltimaModificacion} — Modificado por: ${m.registroUso.modificadoPorNombre}"
        } else {
            binding.tvAuditoriaDetalle.visibility = View.GONE
        }

        // Proyecto
        val tieneProyecto = !m.obra.isNullOrEmpty() || !m.contratista.isNullOrEmpty() || !m.ubicacion.isNullOrEmpty()
        binding.cardProyectoDetalle.visibility = if (tieneProyecto) View.VISIBLE else View.GONE
        binding.tvObraDetalle.text = "Obra: ${m.obra ?: "--"}"
        binding.tvContratistaDetalle.text = "Contratista: ${m.contratista ?: "--"}"
        binding.tvUbicacionDetalle.text = "Ubicación: ${m.ubicacion ?: "--"}"

        // Repostaje
        val tieneRepostaje = m.galonesCombustible > 0 || m.galonesAceite > 0
        binding.cardRepostajeDetalle.visibility = if (tieneRepostaje) View.VISIBLE else View.GONE
        binding.tvCombustibleDetalle.text = "Combustible: ${m.galonesCombustible.formatoHoras()} Gls (${m.tipoCombustible.ifEmpty { "Diesel" }})"
        binding.tvAceiteDetalle.text = "Aceite: ${m.galonesAceite.formatoHoras()} Gls"
        binding.tvTipoCargaDetalle.text = "Tipo de Carga: ${m.tipoCarga.ifEmpty { "Parcial" }}"

        binding.btnEditarActividad.setOnClickListener {
            abrirEdicion(m)
        }
    }

    private fun abrirEdicion(m: BitacoraUsoModel) {
        val fragment = RegistrarUsoMaquinariaFragment()
        val bundle = Bundle().apply {
            putString("uidRegistroUso", m.uid)
            putString("uidMaquinaria", m.registroUso.uidMaquinaria)
            putString("codigoMaquinaria", m.codigoMaquinaria)
            putString("nombreMaquinaria", m.nombreMaquinaria)
            putString("tipoMaquinaria", m.registroUso.tipoMaquinaria)
            
            putDouble("horometroAnterior", m.horometroAnterior)
            putDouble("horometroFinal", m.horometroFinal)
            putString("trabajoRealizado", m.trabajoRealizado)
            putString("tipoMovimiento", m.tipoMovimiento)
            putString("obra", m.obra)
            putString("contratista", m.contratista)
            putString("ubicacion", m.ubicacion)
            putString("fechaRegistro", m.registroUso.fechaRegistro)
            
            // Datos de Repostaje Unificados
            putDouble("galonesCombustible", m.galonesCombustible)
            putDouble("galonesAceite", m.galonesAceite)
            putString("tipoCombustible", m.tipoCombustible)
            putString("tipoCarga", m.tipoCarga)
            
            // Datos de la máquina actual para validaciones
            putDouble("horometroActual", m.maquinaria?.horometroActual ?: m.horometroFinal)
            putDouble("horometroUltimoMantenimiento", m.maquinaria?.horometroUltimoMantenimiento ?: 0.0)
            putDouble("intervaloMantenimientoHoras", m.maquinaria?.intervaloMantenimientoHoras ?: 250.0)
            if (m.maquinaria?.capacidadTanqueGls != null) {
                putDouble("capacidadTanqueGls", m.maquinaria.capacidadTanqueGls!!)
            }
        }
        fragment.arguments = bundle
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
