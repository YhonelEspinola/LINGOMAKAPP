package com.lingomak.lingomakapp.ui.mantenimiento

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.R
import java.io.File

class EvidenciasAdapter(
    private val images: MutableList<String>,
    private val onQuitarClick: (Int) -> Unit
) : RecyclerView.Adapter<EvidenciasAdapter.EvidenciaViewHolder>() {

    class EvidenciaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivEvidencia: ImageView = view.findViewById(R.id.ivEvidencia)
        val btnQuitar: ImageButton = view.findViewById(R.id.btnQuitar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvidenciaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_evidencia_mantenimiento, parent, false)
        return EvidenciaViewHolder(view)
    }

    override fun onBindViewHolder(holder: EvidenciaViewHolder, position: Int) {
        val path = images[position]
        Glide.with(holder.itemView.context)
            .load(File(path))
            .centerCrop()
            .placeholder(R.drawable.bg_image_placeholder)
            .into(holder.ivEvidencia)

        holder.btnQuitar.setOnClickListener { onQuitarClick(holder.adapterPosition) }
    }

    override fun getItemCount(): Int = images.size

    fun actualizarLista(nuevasImagenes: List<String>) {
        // Evitamos limpiar si es la misma lista física para no perder datos
        if (images !== nuevasImagenes) {
            images.clear()
            images.addAll(nuevasImagenes)
        }
        notifyDataSetChanged()
    }
}
