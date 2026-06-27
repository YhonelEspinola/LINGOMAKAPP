package com.lingomak.lingomakapp.ui.repuestos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.databinding.ItemRepuestoBinding

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
            binding.tvEstado.text = repuesto.estado

            // Lógica de criticidad de color
            val colorRes = when {
                repuesto.stockActual == 0 -> R.color.danger
                repuesto.stockActual <= repuesto.stockMinimo -> R.color.warning
                else -> R.color.success
            }

            binding.viewEstadoColor.backgroundTintList = 
                ContextCompat.getColorStateList(binding.root.context, colorRes)

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
