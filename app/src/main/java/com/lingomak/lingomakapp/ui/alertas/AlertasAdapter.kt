package com.lingomak.lingomakapp.ui.alertas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.data.model.AlertaModel
import com.lingomak.lingomakapp.databinding.ItemAlertaBinding

class AlertasAdapter(
    private var listaAlertas: List<AlertaModel>
) : RecyclerView.Adapter<AlertasAdapter.AlertaViewHolder>() {

    inner class AlertaViewHolder(
        private val binding: ItemAlertaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alerta: AlertaModel,mostrarCategoria: Boolean) {

            binding.tvIconoAlerta.text =
                if (alerta.icono.isNotEmpty()) alerta.icono else "⚠"

            binding.tvTituloAlerta.text = alerta.titulo
            binding.tvMensajeAlerta.text = alerta.mensaje
            binding.tvFechaAlerta.text =
                if (alerta.fecha.isNotEmpty()) "Fecha: ${alerta.fecha}" else alerta.categoria

            binding.tvPrioridadAlerta.text =
                "${alerta.categoria} • Prioridad: ${alerta.prioridad}"

            binding.tvCategoriaAlerta.visibility =
                if (mostrarCategoria) View.VISIBLE else View.GONE

            binding.tvCategoriaAlerta.text = when (alerta.categoria) {
                "MANTENIMIENTO" -> "🔧 MANTENIMIENTO"
                "INVENTARIO" -> "📦 INVENTARIO"
                "MOVIMIENTOS" -> "📈 MOVIMIENTOS"
                else -> alerta.categoria
            }
            aplicarEstilos(alerta.tipo)
        }

        private fun aplicarEstilos(tipo: String) {

            when (tipo) {

                "VENCIDO" -> {
                    binding.tvIconoAlerta.text = "🚨"

                    binding.tvTituloAlerta.setTextColor(
                        Color.rgb(185, 28, 28)
                    )

                    binding.tvPrioridadAlerta.setTextColor(
                        Color.rgb(185, 28, 28)
                    )
                }

                "PROXIMO" -> {
                    binding.tvIconoAlerta.text = "⏳"

                    binding.tvTituloAlerta.setTextColor(
                        Color.rgb(180, 83, 9)
                    )

                    binding.tvPrioridadAlerta.setTextColor(
                        Color.rgb(180, 83, 9)
                    )
                }

                "EN_PROCESO" -> {
                    binding.tvIconoAlerta.text = "🛠"

                    binding.tvTituloAlerta.setTextColor(
                        Color.rgb(37, 99, 235)
                    )

                    binding.tvPrioridadAlerta.setTextColor(
                        Color.rgb(37, 99, 235)
                    )
                }

                "STOCK_AGOTADO" -> {
                    binding.tvIconoAlerta.text = "📦"
                    binding.tvTituloAlerta.setTextColor(Color.rgb(185, 28, 28))
                    binding.tvPrioridadAlerta.setTextColor(Color.rgb(185, 28, 28))
                }

                "STOCK_CRITICO" -> {
                    binding.tvIconoAlerta.text = "📦"
                    binding.tvTituloAlerta.setTextColor(Color.rgb(234, 88, 12))
                    binding.tvPrioridadAlerta.setTextColor(Color.rgb(234, 88, 12))
                }

                "STOCK_BAJO" -> {
                    binding.tvIconoAlerta.text = "📦"
                    binding.tvTituloAlerta.setTextColor(Color.rgb(180, 83, 9))
                    binding.tvPrioridadAlerta.setTextColor(Color.rgb(180, 83, 9))
                }

                "ALTO_CONSUMO" -> {
                    binding.tvIconoAlerta.text = "📈"
                    binding.tvTituloAlerta.setTextColor(Color.rgb(124, 58, 237))
                    binding.tvPrioridadAlerta.setTextColor(Color.rgb(124, 58, 237))
                }

                "SIN_ROTACION" -> {
                    binding.tvIconoAlerta.text = "📦"
                    binding.tvTituloAlerta.setTextColor(Color.rgb(75, 85, 99))
                    binding.tvPrioridadAlerta.setTextColor(Color.rgb(75, 85, 99))
                }

                else -> {
                    binding.tvIconoAlerta.text = "⚠"
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