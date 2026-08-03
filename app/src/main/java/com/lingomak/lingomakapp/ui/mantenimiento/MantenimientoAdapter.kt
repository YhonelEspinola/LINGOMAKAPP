package com.lingomak.lingomakapp.ui.mantenimiento

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.ItemMantenimientoBinding
import android.widget.PopupMenu
import com.lingomak.lingomakapp.R

class MantenimientoAdapter(
    private var listaMantenimientos: List<MantenimientoModel>,
    private val onMantenimientoClick: (MantenimientoModel) -> Unit
) : RecyclerView.Adapter<MantenimientoAdapter.MantenimientoViewHolder>() {

    inner class MantenimientoViewHolder(
        private val binding: ItemMantenimientoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mantenimiento: MantenimientoModel) {
            binding.tvTipoMantenimiento.text = mantenimiento.tipoMantenimiento
            binding.tvCodigoMantenimiento.text = mantenimiento.codigoMantenimiento
            binding.tvDescripcionMantenimiento.text = mantenimiento.descripcion
            binding.tvMaquinariaMantenimiento.text = mantenimiento.nombreMaquinaria
            binding.tvFechaProgramada.text = mantenimiento.fechaProgramada
            binding.tvHorometroMantenimiento.text = "${mantenimiento.horometroProgramado} h"
            binding.tvResponsableMantenimiento.text = mantenimiento.responsable
            binding.tvPrioridadMantenimiento.text = "Prioridad: ${mantenimiento.prioridad}"
            
            aplicarColorTipo(mantenimiento.tipoMantenimiento)
            aplicarColorEstado(mantenimiento.estado)

            binding.root.setOnClickListener {
                onMantenimientoClick(mantenimiento)
            }
        }

        private fun aplicarColorTipo(tipo: String) {
            when (tipo) {
                "PREVENTIVO" -> binding.tvTipoMantenimiento.setTextColor(Color.rgb(37, 99, 235))
                "CORRECTIVO" -> binding.tvTipoMantenimiento.setTextColor(Color.rgb(234, 88, 12))
                else -> binding.tvTipoMantenimiento.setTextColor(Color.rgb(55, 65, 81))
            }
        }

        private fun aplicarColorEstado(estado: String) {
            val context = binding.root.context
            val color: Int
            val background: Int

            when (estado) {
                "PENDIENTE" -> { color = R.color.warning; background = R.drawable.bg_chip_estado_pendiente }
                "EN_PROCESO" -> { color = R.color.primary; background = R.drawable.bg_chip_estado_proceso }
                "FINALIZADO" -> { color = R.color.success; background = R.drawable.bg_chip_estado_operativa }
                "VENCIDO" -> { color = R.color.danger; background = R.drawable.bg_chip_estado_inactiva }
                "CANCELADO" -> { color = R.color.text_secondary; background = R.drawable.bg_chip_estado_cancelado }
                else -> { color = R.color.text_primary; background = R.drawable.bg_chip_estado_cancelado }
            }
            
            binding.tvEstadoMantenimiento.setTextColor(context.getColor(color))
            binding.tvEstadoMantenimiento.setBackgroundResource(background)
            
            if (estado == "EN_PROCESO") {
                binding.tvEstadoMantenimiento.text = "EN PROCESO"
            } else {
                binding.tvEstadoMantenimiento.text = estado
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MantenimientoViewHolder {
        val binding = ItemMantenimientoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MantenimientoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MantenimientoViewHolder, position: Int) {
        holder.bind(listaMantenimientos[position])
    }

    override fun getItemCount(): Int = listaMantenimientos.size

    fun actualizarLista(nuevaLista: List<MantenimientoModel>) {
        listaMantenimientos = nuevaLista
        notifyDataSetChanged()
    }
}
