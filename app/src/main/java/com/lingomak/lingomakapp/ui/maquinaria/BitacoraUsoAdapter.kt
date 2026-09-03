package com.lingomak.lingomakapp.ui.maquinaria

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.data.model.BitacoraUsoModel
import com.lingomak.lingomakapp.databinding.ItemBitacoraUsoBinding

import com.lingomak.lingomakapp.utils.formatoHoras

class BitacoraUsoAdapter(
    private var lista: List<BitacoraUsoModel>,
    private val onClick: (BitacoraUsoModel) -> Unit
) : RecyclerView.Adapter<BitacoraUsoAdapter.ViewHolder>() {

    fun actualizarLista(nuevaLista: List<BitacoraUsoModel>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBitacoraUsoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int = lista.size

    inner class ViewHolder(private val binding: ItemBitacoraUsoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(m: BitacoraUsoModel) {
            binding.tvFechaBitacora.text = m.fecha
            binding.chipTipoMovimiento.text = m.tipoMovimiento.uppercase()
            binding.tvNombreMaquina.text = "${m.nombreMaquinaria} · ${m.marcaMaquinaria} · ${m.modeloMaquinaria}"
            binding.tvTrabajoRealizado.text = m.trabajoRealizado
            binding.tvHorometrosRango.text = "${m.horometroAnterior.formatoHoras()} - ${m.horometroFinal.formatoHoras()}"
            binding.tvTotalHoras.text = "${m.horasUso.formatoHoras()} h"
            binding.tvOperario.text = m.operarioNombre
            
            if (m.galonesCombustible > 0) {
                binding.tvRepostajeInfo.text = "Repostaje: ${m.galonesCombustible.formatoHoras()} Gls"
            } else {
                binding.tvRepostajeInfo.text = "Repostaje: NO"
            }

            binding.root.setOnClickListener { onClick(m) }
        }
    }
}
