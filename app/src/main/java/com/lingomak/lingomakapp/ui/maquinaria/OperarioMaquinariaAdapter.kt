package com.lingomak.lingomakapp.ui.maquinaria

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.databinding.ItemMaquinariaOperarioBinding
import com.lingomak.lingomakapp.utils.formatoHoras

class OperarioMaquinariaAdapter(
    private var listaMaquinarias: List<MaquinariaModel>,
    private val onRegistrarUsoClick: (MaquinariaModel) -> Unit
) : RecyclerView.Adapter<OperarioMaquinariaAdapter.MaquinariaViewHolder>() {

    inner class MaquinariaViewHolder(
        private val binding: ItemMaquinariaOperarioBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(maquinaria: MaquinariaModel) {

            binding.tvNombreMaquinaria.text = maquinaria.nombre
            binding.tvCodigoMaquinaria.text = maquinaria.codigoMaquinaria

            binding.tvTipoMarcaModelo.text =
                "${maquinaria.tipo} | ${maquinaria.marca} | ${maquinaria.modelo}"

            binding.tvEstadoMaquinaria.text = maquinaria.estado

            binding.tvHorometroMaquinaria.text = "Horómetro actual: ${maquinaria.horometroActual.formatoHoras()} h"

            binding.tvUbicacionMaquinaria.text =
                maquinaria.ubicacionActual.ifEmpty { "Sin ubicación" }

            val horasDesdeUltimo =
                maquinaria.horometroActual - maquinaria.horometroUltimoMantenimiento

            val horasRestantes =
                maquinaria.intervaloMantenimientoHoras - horasDesdeUltimo

            when {
                horasRestantes <= 20 -> {
                    binding.tvHorasRestantes.text = "Faltan ${horasRestantes.formatoHoras()} h para mantenimiento"
                    binding.tvHorasRestantes.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.danger))
                }
                horasRestantes <= 50 -> {
                    binding.tvHorasRestantes.text = "Faltan ${horasRestantes.formatoHoras()} h para mantenimiento"
                    binding.tvHorasRestantes.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.warning))
                }
                else -> {
                    binding.tvHorasRestantes.text = "Faltan ${horasRestantes.formatoHoras()} h para mantenimiento"
                    binding.tvHorasRestantes.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.success))
                }
            }

            if (maquinaria.imagenUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(maquinaria.imagenUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .into(binding.imgMaquinaria)
            } else {
                binding.imgMaquinaria.setImageResource(
                    R.drawable.bg_image_placeholder
                )
            }

            binding.btnRegistrarUso.setOnClickListener {
                onRegistrarUsoClick(maquinaria)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MaquinariaViewHolder {

        val binding = ItemMaquinariaOperarioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MaquinariaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MaquinariaViewHolder,
        position: Int
    ) {
        holder.bind(listaMaquinarias[position])
    }

    override fun getItemCount(): Int {
        return listaMaquinarias.size
    }

    fun actualizarLista(nuevaLista: List<MaquinariaModel>) {
        listaMaquinarias = nuevaLista
        notifyDataSetChanged()
    }
}
