package com.lingomak.lingomakapp.ui.mantenimiento

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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


            binding.tvNombreMaquinariaSolicitud.text =
                solicitud.nombreMaquinaria

            binding.tvCodigoMaquinariaSolicitud.text =
                "Código: ${solicitud.codigoMaquinaria}"

            binding.tvTipoMaquinariaSolicitud.text =
                "Tipo: ${solicitud.tipoMaquinaria}"

            binding.tvHorometroActualSolicitud.text =
                "Horómetro actual: ${solicitud.horometroActual} h"

            binding.tvIntervaloSolicitud.text =
                "Intervalo: cada ${solicitud.intervaloMantenimientoHoras} h"

            binding.tvHorasRestantesSolicitud.text =
                when {
                    solicitud.horasRestantes < 0 -> {
                        "Horas excedidas: ${kotlin.math.abs(solicitud.horasRestantes)} h"
                    }

                    solicitud.horasRestantes == 0 -> {
                        "Horas restantes: límite alcanzado"
                    }

                    else -> {
                        "Horas restantes: ${solicitud.horasRestantes} h"
                    }
                }

            binding.tvMotivoSolicitud.text =
                solicitud.motivo

            binding.tvOperarioSolicitud.text =
                "Registrado por: ${
                    solicitud.nombreOperario.ifBlank {
                        solicitud.correoOperario.ifBlank {
                            "Operario no identificado"
                        }
                    }
                }"

            binding.tvFechaSolicitud.text =
                "Fecha: ${solicitud.fechaRegistro}"

            aplicarUrgencia(solicitud.horasRestantes)

            binding.btnRevisarSolicitud.setOnClickListener {
                onRevisarClick(solicitud)
            }

            binding.btnRechazarSolicitud.setOnClickListener {
                onRechazarClick(solicitud)
            }
        }

        private fun aplicarUrgencia(horasRestantes: Int) {

            when {

                horasRestantes < 0 -> {
                    binding.tvUrgenciaSolicitud.text = "EXCEDIDO"

                    binding.tvUrgenciaSolicitud.setTextColor(
                        Color.rgb(185, 28, 28)
                    )

                    binding.tvHorasRestantesSolicitud.setTextColor(
                        Color.rgb(185, 28, 28)
                    )
                }

                horasRestantes == 0 -> {
                    binding.tvUrgenciaSolicitud.text =
                        "LÍMITE ALCANZADO"

                    binding.tvUrgenciaSolicitud.setTextColor(
                        Color.rgb(185, 28, 28)
                    )

                    binding.tvHorasRestantesSolicitud.setTextColor(
                        Color.rgb(185, 28, 28)
                    )
                }

                horasRestantes <= 10 -> {
                    binding.tvUrgenciaSolicitud.text = "CRÍTICO"

                    binding.tvUrgenciaSolicitud.setTextColor(
                        Color.rgb(185, 28, 28)
                    )

                    binding.tvHorasRestantesSolicitud.setTextColor(
                        Color.rgb(185, 28, 28)
                    )
                }

                else -> {
                    binding.tvUrgenciaSolicitud.text = "PRÓXIMO"

                    binding.tvUrgenciaSolicitud.setTextColor(
                        Color.rgb(180, 83, 9)
                    )

                    binding.tvHorasRestantesSolicitud.setTextColor(
                        Color.rgb(180, 83, 9)
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SolicitudViewHolder {

        val binding =
            ItemSolicitudMantenimientoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return SolicitudViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SolicitudViewHolder,
        position: Int
    ) {
        holder.bind(listaSolicitudes[position])
    }

    override fun getItemCount(): Int {
        return listaSolicitudes.size
    }

    fun actualizarLista(
        nuevaLista: List<SolicitudMantenimientoModel>
    ) {
        listaSolicitudes = nuevaLista
        notifyDataSetChanged()
    }
}