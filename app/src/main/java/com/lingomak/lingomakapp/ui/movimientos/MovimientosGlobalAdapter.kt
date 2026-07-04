package com.lingomak.lingomakapp.ui.movimientos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.databinding.ItemMovimientoGlobalBinding
import java.text.SimpleDateFormat
import java.util.*

class MovimientosGlobalAdapter(
    private var lista: List<Pair<MovimientoModel, String>>,
    private val onItemClick: (Pair<MovimientoModel, String>) -> Unit
) : RecyclerView.Adapter<MovimientosGlobalAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemMovimientoGlobalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pair: Pair<MovimientoModel, String>) {
            val movimiento = pair.first
            val nombreRepuesto = pair.second

            binding.tvNombreRepuesto.text = nombreRepuesto
            binding.tvTipo.text = movimiento.tipo
            binding.tvFecha.text = movimiento.fecha?.let { dateFormat.format(it) } ?: "--/--/--"

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

            binding.root.setOnClickListener { onItemClick(pair) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovimientoGlobalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<Pair<MovimientoModel, String>>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
