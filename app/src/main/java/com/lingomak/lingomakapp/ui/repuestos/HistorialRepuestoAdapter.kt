package com.lingomak.lingomakapp.ui.repuestos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.databinding.ItemMovimientoBinding
import java.text.SimpleDateFormat
import java.util.Locale

class HistorialRepuestoAdapter(
    private var listaMovimientos: List<MovimientoModel>
) : RecyclerView.Adapter<HistorialRepuestoAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemMovimientoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(movimiento: MovimientoModel) {
            binding.tvTipo.text = movimiento.tipo
            binding.tvFecha.text = movimiento.fecha?.let { dateFormat.format(it) } ?: "--/--/--"
            
            val idProvisional = if (movimiento.registradoPor.length >= 6) {
                movimiento.registradoPor.substring(0, 6)
            } else {
                movimiento.registradoPor
            }
            // TODO: Cruzar con colección "usuarios" para mostrar nombre real
            binding.tvUsuario.text = "Responsable: $idProvisional"

            if (movimiento.tipo == "ENTRADA") {
                binding.tvCantidad.text = "+${movimiento.cantidad}"
                binding.tvCantidad.setTextColor(ContextCompat.getColor(binding.root.context, R.color.success))
            } else {
                binding.tvCantidad.text = "-${movimiento.cantidad}"
                binding.tvCantidad.setTextColor(ContextCompat.getColor(binding.root.context, R.color.danger))
            }

            if (movimiento.observacion.isNotEmpty()) {
                binding.tvObservacion.visibility = View.VISIBLE
                binding.tvObservacion.text = movimiento.observacion
            } else {
                binding.tvObservacion.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovimientoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listaMovimientos[position])
    }

    override fun getItemCount(): Int = listaMovimientos.size

    fun actualizarLista(nuevaLista: List<MovimientoModel>) {
        listaMovimientos = nuevaLista
        notifyDataSetChanged()
    }
}
