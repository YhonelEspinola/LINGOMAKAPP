package com.lingomak.lingomakapp.ui.mantenimiento

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.repository.ConsumoRepuesto

class InsumoMantenimientoAdapter(
    private var insumos: MutableList<ConsumoRepuesto>,
    private val onEliminarClick: (Int) -> Unit
) : RecyclerView.Adapter<InsumoMantenimientoAdapter.InsumoViewHolder>() {

    class InsumoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvInsumoNombre)
        val tvCantidad: TextView = view.findViewById(R.id.tvInsumoCantidad)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminarInsumo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsumoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_insumo_mantenimiento, parent, false)
        return InsumoViewHolder(view)
    }

    override fun onBindViewHolder(holder: InsumoViewHolder, position: Int) {
        val insumo = insumos[position]
        holder.tvNombre.text = insumo.nombre
        holder.tvCantidad.text = "Cantidad: ${insumo.cantidad}"
        holder.btnEliminar.setOnClickListener { onEliminarClick(position) }
    }

    override fun getItemCount(): Int = insumos.size

    fun actualizarLista(nuevaLista: List<ConsumoRepuesto>) {
        insumos = nuevaLista.toMutableList()
        notifyDataSetChanged()
    }
}
