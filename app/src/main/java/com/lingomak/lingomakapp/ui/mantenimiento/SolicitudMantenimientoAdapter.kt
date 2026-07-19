package com.lingomak.lingomakapp.ui.mantenimiento

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.SolicitudMantenimientoModel
import com.lingomak.lingomakapp.databinding.ItemSolicitudMantenimientoBinding

class SolicitudMantenimientoAdapter(
    private var listaSolicitudes: List<SolicitudMantenimientoModel>,
    private val onRevisarClick: (SolicitudMantenimientoModel) -> Unit,
    private val onRechazarClick: (SolicitudMantenimientoModel) -> Unit
) : RecyclerView.Adapter<SolicitudMantenimientoAdapter.SolicitudViewHolder>() {

    inner class SolicitudViewHolder(
        private val binding: ItemSolicitudMantenimientoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(solicitud: SolicitudMantenimientoModel) {
            binding.tvNombreMaquinariaSolicitud.text = solicitud.nombreMaquinaria
            binding.tvCodigoMaquinariaSolicitud.text = "Código: ${solicitud.codigoMaquinaria}"
            
            binding.tvHorometroActualSolicitud.text = "Horómetro actual: ${solicitud.horometroActual} h"

            binding.tvHorasRestantesSolicitud.text = when {
                solicitud.horasRestantes < 0 -> {
                    "Horas excedidas: ${kotlin.math.abs(solicitud.horasRestantes)} h"
                }
                solicitud.horasRestantes == 0 -> {
                    "Límite de horómetro alcanzado"
                }
                else -> {
                    "Horas restantes: ${solicitud.horasRestantes} h"
                }
            }

            binding.tvMotivoSolicitud.text = solicitud.motivo

            aplicarEstiloUrgencia(solicitud.horasRestantes)

            binding.btnRevisarSolicitud.setOnClickListener {
                onRevisarClick(solicitud)
            }

            binding.btnRechazarSolicitud.setOnClickListener {
                onRechazarClick(solicitud)
            }
        }

        private fun aplicarEstiloUrgencia(horasRestantes: Int) {
            val context = binding.root.context
            when {
                horasRestantes <= 5 -> {
                    binding.tvUrgenciaSolicitud.text = "URGENTE"
                    binding.tvUrgenciaSolicitud.setTextColor(context.getColor(R.color.danger))
                    binding.tvHorasRestantesSolicitud.setTextColor(context.getColor(R.color.danger))
                }
                horasRestantes <= 20 -> {
                    binding.tvUrgenciaSolicitud.text = "PRÓXIMO"
                    binding.tvUrgenciaSolicitud.setTextColor(context.getColor(R.color.primary))
                    binding.tvHorasRestantesSolicitud.setTextColor(context.getColor(R.color.primary))
                }
                else -> {
                    binding.tvUrgenciaSolicitud.text = "PREVENTIVO"
                    binding.tvUrgenciaSolicitud.setTextColor(context.getColor(R.color.success))
                    binding.tvHorasRestantesSolicitud.setTextColor(context.getColor(R.color.success))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SolicitudViewHolder {
        val binding = ItemSolicitudMantenimientoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SolicitudViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SolicitudViewHolder, position: Int) {
        holder.bind(listaSolicitudes[position])
    }

    override fun getItemCount(): Int = listaSolicitudes.size

    fun actualizarLista(nuevaLista: List<SolicitudMantenimientoModel>) {
        listaSolicitudes = nuevaLista
        notifyDataSetChanged()
    }
}