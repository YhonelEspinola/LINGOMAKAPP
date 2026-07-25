package com.lingomak.lingomakapp.ui.configuracion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.CategoriaModel

class CategoriaAdapter(
    private var lista: List<CategoriaModel>,
    private val onEliminarClick: (CategoriaModel) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreCategoria)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminarCategoria)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_categoria_config, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.tvNombre.text = item.nombre
        holder.btnEliminar.setOnClickListener { onEliminarClick(item) }
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nueva: List<CategoriaModel>) {
        lista = nueva
        notifyDataSetChanged()
    }
}
