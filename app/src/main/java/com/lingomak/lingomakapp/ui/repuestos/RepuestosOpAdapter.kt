package com.lingomak.lingomakapp.ui.repuestos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.databinding.ItemRepuestoBinding
import com.lingomak.lingomakapp.utils.ImageOptimizer

class RepuestosOpAdapter(
    private var listaRepuestos: List<RepuestoModel>,
    private val onItemClick: (RepuestoModel) -> Unit
) : RecyclerView.Adapter<RepuestosOpAdapter.RepuestoOpViewHolder>() {

    inner class RepuestoOpViewHolder(private val binding: ItemRepuestoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(repuesto: RepuestoModel) {
            binding.tvNombre.text = repuesto.nombre
            binding.tvCategoria.text = repuesto.categoria
            binding.tvStockActual.text = repuesto.stockActual.toString()

            // Manejo de visibilidad y overlay según estado
            if (repuesto.estado.equals("INACTIVO", ignoreCase = true)) {
                binding.overlayInactivo.visibility = View.VISIBLE
            } else {
                binding.overlayInactivo.visibility = View.GONE
            }

            if (repuesto.imagenUrl.isNotEmpty()) {
                val optimizedUrl = ImageOptimizer.getOptimizedUrl(repuesto.imagenUrl, "512x512")
                Glide.with(binding.root.context)
                    .load(optimizedUrl)
                    .thumbnail(Glide.with(binding.root.context).load(repuesto.imagenUrl).override(100))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivRepuesto)
            } else {
                binding.ivRepuesto.setImageResource(android.R.drawable.ic_menu_gallery)
            }

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepuestoOpViewHolder {
        val binding = ItemRepuestoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RepuestoOpViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RepuestoOpViewHolder, position: Int) {
        holder.bind(listaRepuestos[position])
    }

    override fun getItemCount(): Int = listaRepuestos.size

    fun actualizarLista(nuevaLista: List<RepuestoModel>) {
        listaRepuestos = nuevaLista
        notifyDataSetChanged()
    }
}