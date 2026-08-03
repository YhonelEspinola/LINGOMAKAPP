package com.lingomak.lingomakapp.ui.repuestos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.databinding.ItemRepuestoBinding

import com.lingomak.lingomakapp.utils.ImageOptimizer

class RepuestosAdapter(
    private var listaRepuestos: List<RepuestoModel>,
    private val onItemClick: (RepuestoModel) -> Unit
) : RecyclerView.Adapter<RepuestosAdapter.RepuestoViewHolder>() {

    inner class RepuestoViewHolder(private val binding: ItemRepuestoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(repuesto: RepuestoModel) {
            binding.tvNombre.text = repuesto.nombre
            binding.tvCategoria.text = repuesto.categoria
            binding.tvStockActual.text = repuesto.stockActual.toString()
            // Manejo de visibilidad y overlay según estado
            if (repuesto.estado.equals("INACTIVO", ignoreCase = true)) {
                binding.overlayInactivo.visibility = android.view.View.VISIBLE
            } else {
                binding.overlayInactivo.visibility = android.view.View.GONE
            }

            // Cargar imagen optimizada (thumbnail)
            if (repuesto.imagenUrl.isNotEmpty()) {
                val optimizedUrl = ImageOptimizer.getOptimizedUrl(repuesto.imagenUrl, "512x512")
                Glide.with(binding.root.context)
                    .load(optimizedUrl)
                    .thumbnail(Glide.with(binding.root.context).load(repuesto.imagenUrl).override(100))
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .into(binding.ivRepuesto)
            } else {
                binding.ivRepuesto.setImageResource(R.drawable.bg_image_placeholder)
            }

            // Lógica de criticidad de color para el indicador lateral
            val colorRes = when {
                repuesto.stockActual == 0 -> R.color.danger
                repuesto.stockActual <= repuesto.stockMinimo -> R.color.warning
                else -> R.color.success
            }

            binding.viewIndicator.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, colorRes)
            )

            binding.root.setOnClickListener {
                onItemClick(repuesto)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepuestoViewHolder {
        val binding = ItemRepuestoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RepuestoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RepuestoViewHolder, position: Int) {
        holder.bind(listaRepuestos[position])
    }

    override fun getItemCount(): Int = listaRepuestos.size

    fun actualizarLista(nuevaLista: List<RepuestoModel>) {
        listaRepuestos = nuevaLista
        notifyDataSetChanged()
    }
}
