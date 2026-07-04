package com.lingomak.lingomakapp.ui.alertas

import android.graphics.Color
import android.view.LayoutInflater
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

        fun bind(alerta: AlertaModel) {

            binding.tvTituloAlerta.text = alerta.titulo
            binding.tvMensajeAlerta.text = alerta.mensaje
            binding.tvFechaAlerta.text = "Fecha: ${alerta.fecha}"
            binding.tvPrioridadAlerta.text =
                "Prioridad: ${alerta.prioridad}"

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

    override fun onBindViewHolder(
        holder: AlertaViewHolder,
        position: Int
    ) {
        holder.bind(listaAlertas[position])
    }

    override fun getItemCount(): Int {
        return listaAlertas.size
    }


    fun actualizarLista(nuevaLista: List<AlertaModel>) {
        listaAlertas = nuevaLista
        notifyDataSetChanged()
    }
}