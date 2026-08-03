package com.lingomak.lingomakapp.ui.alertas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.databinding.ItemAlertaBinding

class AlertasAdapter(
    private var listaAlertas: List<AlertaModel>,
    private val onTomarAccionClick: (AlertaModel) -> Unit
) : RecyclerView.Adapter<AlertasAdapter.AlertaViewHolder>() {

    inner class AlertaViewHolder(
        private val binding: ItemAlertaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alerta: AlertaModel, mostrarCategoria: Boolean) {

            binding.tvTituloAlerta.text = alerta.titulo
            binding.tvMensajeAlerta.text = alerta.mensaje
            binding.tvFechaAlerta.text =
                if (alerta.fecha.isNotEmpty()) "Fecha: ${alerta.fecha}" else alerta.categoria

            binding.tvPrioridadAlerta.text =
                "${alerta.categoria} • Prioridad: ${alerta.prioridad}"

            binding.tvCategoriaAlerta.visibility =
                if (mostrarCategoria) View.VISIBLE else View.GONE

            binding.tvCategoriaAlerta.text = when (alerta.categoria) {
                "MANTENIMIENTO" -> "MANTENIMIENTO"
                "INVENTARIO" -> "INVENTARIO"
                "MOVIMIENTOS" -> "MOVIMIENTOS"
                else -> alerta.categoria
            }
            aplicarEstilos(alerta.tipo)

            binding.btnTomarAccionAlerta.text = if (alerta.tipo == "ACTIVIDAD_OPERARIO") "Ver más" else "Tomar acción"

            binding.btnTomarAccionAlerta.setOnClickListener {
                onTomarAccionClick(alerta)
            }
        }

        private fun aplicarEstilos(tipo: String) {
            val context = binding.root.context
            val colorText = when (tipo) {
                "VENCIDO", "STOCK_AGOTADO" -> R.color.danger
                "STOCK_CRITICO", "STOCK_BAJO", "PROXIMO" -> R.color.warning
                "EN_PROCESO", "SOLICITUD_MANTENIMIENTO" -> R.color.primary
                "ALTO_CONSUMO" -> R.color.brand_yellow
                else -> R.color.text_secondary
            }

            binding.tvTituloAlerta.setTextColor(ContextCompat.getColor(context, colorText))
            binding.tvPrioridadAlerta.setTextColor(ContextCompat.getColor(context, colorText))

            when (tipo) {
                "ACTIVIDAD_OPERARIO" -> {
                    binding.ivIconoAlerta.setImageResource(R.drawable.person)
                }
                "VENCIDO" -> {
                    binding.ivIconoAlerta.setImageResource(R.drawable.alert)
                }
                "PROXIMO" -> {
                    binding.ivIconoAlerta.setImageResource(R.drawable.ic_notification)
                }
                "EN_PROCESO" -> {
                    binding.ivIconoAlerta.setImageResource(R.drawable.maintenance)
                }
                "STOCK_AGOTADO", "STOCK_CRITICO", "STOCK_BAJO" -> {
                    binding.ivIconoAlerta.setImageResource(R.drawable.inventario)
                }
                "ALTO_CONSUMO" -> {
                    binding.ivIconoAlerta.setImageResource(R.drawable.ic_analytics)
                }
                "SIN_ROTACION" -> {
                    binding.ivIconoAlerta.setImageResource(R.drawable.inventario)
                }
                "SOLICITUD_MANTENIMIENTO" -> {
                    binding.ivIconoAlerta.setImageResource(R.drawable.report)
                }
                else -> {
                    binding.ivIconoAlerta.setImageResource(R.drawable.alert)
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AlertaViewHolder {

        val binding = ItemAlertaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return AlertaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {

        val alertaActual = listaAlertas[position]

        val mostrarCategoria =
            position == 0 ||
                    listaAlertas[position - 1].categoria != alertaActual.categoria

        holder.bind(
            alerta = alertaActual,
            mostrarCategoria = mostrarCategoria
        )
    }

    override fun getItemCount(): Int {
        return listaAlertas.size
    }


    fun actualizarLista(nuevaLista: List<AlertaModel>) {
        listaAlertas = nuevaLista
        notifyDataSetChanged()
    }
}
