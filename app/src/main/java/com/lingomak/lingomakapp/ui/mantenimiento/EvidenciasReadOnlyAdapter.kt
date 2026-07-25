package com.lingomak.lingomakapp.ui.mantenimiento

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.R
import java.io.File

class EvidenciasReadOnlyAdapter(
    private val images: List<String>,
    private val onImagenClick: (String) -> Unit
) : RecyclerView.Adapter<EvidenciasReadOnlyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iv: ImageView = view.findViewById(R.id.ivEvidencia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_evidencia_mantenimiento, parent, false)
        view.findViewById<View>(R.id.btnQuitar).visibility = View.GONE
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val path = images[position]
        
        // Cargamos de forma inteligente: URLs remotas, URIs de contenido/archivo o File local
        val imageSource = when {
            path.startsWith("http") || path.startsWith("content") || path.startsWith("file") -> path
            else -> File(path)
        }

        Glide.with(holder.itemView.context)
            .load(imageSource)
            .centerCrop()
            .placeholder(R.drawable.bg_image_placeholder)
            .error(R.drawable.bg_image_placeholder)
            .into(holder.iv)

        holder.itemView.setOnClickListener { onImagenClick(path) }
    }

    override fun getItemCount(): Int = images.size
}
