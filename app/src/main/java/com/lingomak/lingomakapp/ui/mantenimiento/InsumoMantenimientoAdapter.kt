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
        val tvNombre: TextView = view.findViewById(android.R.id.text1)
        val tvCantidad: TextView = view.findViewById(android.R.id.text2)
        val btnEliminar: ImageButton = ImageButton(view.context).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        
        init {
            (view as ViewGroup).addView(btnEliminar, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsumoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
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
