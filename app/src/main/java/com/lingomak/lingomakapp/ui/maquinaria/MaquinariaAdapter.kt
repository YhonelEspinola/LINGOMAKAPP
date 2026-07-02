package com.lingomak.lingomakapp.ui.maquinaria

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.databinding.ItemMaquinariaBinding

class MaquinariaAdapter(
    private var listaMaquinarias: List<MaquinariaModel>,
    private val onMaquinariaClick: (MaquinariaModel) -> Unit,
    private val onEditarClick: (MaquinariaModel) -> Unit,
    private val onCambiarEstadoClick: (MaquinariaModel) -> Unit
) : RecyclerView.Adapter<MaquinariaAdapter.MaquinariaViewHolder>() {

    inner class MaquinariaViewHolder(
        private val binding: ItemMaquinariaBinding
    ) : RecyclerView.ViewHolder(binding.root){

        fun bind(maquinaria: MaquinariaModel){

            binding.tvNombreMaquinaria.text = maquinaria.nombre
            binding.tvCodigoMaquinaria.text = maquinaria.codigoMaquinaria
            binding.tvTipoMarcaModelo.text =
                "${maquinaria.tipo} | ${maquinaria.marca} | ${maquinaria.modelo}"

            binding.tvHorometroMaquinaria.text =
                "${maquinaria.horometroActual} h"

            binding.tvUbicacionMaquinaria.text =
                maquinaria.ubicacionActual.ifEmpty { "sin ubicacion" }

            if (maquinaria.imagenUrl.isNotEmpty()){
                Glide.with(binding.root.context)
                    .load(maquinaria.imagenUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_maquinaria_placeholder)
                    .error(R.drawable.ic_maquinaria_placeholder)
                    .into(binding.imgMaquinaria)
            }else{
                binding.imgMaquinaria.setImageResource(R.drawable.ic_maquinaria_placeholder)
            }

            aplicarEstado(maquinaria.estado)

            binding.root.setOnClickListener {
                onMaquinariaClick(maquinaria)
            }

            binding.btnOpcionesMaquinaria.setOnClickListener {
                mostrarMenuOpciones(maquinaria)
            }
        }

        private fun aplicarEstado(estado: String) {
            when(estado){
                "OPERATIVA" -> {
                    binding.tvEstadoMaquinaria.text = "🟢 OPERATIVA"
                    binding.tvEstadoMaquinaria.setTextColor(Color.rgb(22, 101, 52))
                    binding.tvEstadoMaquinaria.setBackgroundResource(R.drawable.bg_chip_estado_operativa)
                }

                "EN_MANTENIMIENTO" -> {
                    binding.tvEstadoMaquinaria.text = "🟠 EN MANTENIMIENTO"
                binding.tvEstadoMaquinaria.setTextColor(Color.rgb(180, 83, 9))
                        binding.tvEstadoMaquinaria.setBackgroundResource(R.drawable.bg_chip_estado_pendiente)
                }

                "INACTIVA" -> {
                binding.tvEstadoMaquinaria.text = "🔴 INACTIVA"
                binding.tvEstadoMaquinaria.setTextColor(Color.rgb(185, 28, 28))
                binding.tvEstadoMaquinaria.setBackgroundResource(R.drawable.bg_chip_estado_inactiva)
                }
                else ->{
                    binding.tvEstadoMaquinaria.text = estado
                    binding.tvEstadoMaquinaria.setTextColor(R.drawable.bg_chip_estado_inactiva)
                }
            }

        }

        private fun mostrarMenuOpciones(maquinaria: MaquinariaModel){
            val popupMemu = PopupMenu(binding.root.context,binding.btnOpcionesMaquinaria)

            popupMemu.menu.add("Ver detalle")
            popupMemu.menu.add("Editar")
            popupMemu.menu.add("Cambiar estado")

            popupMemu.setOnMenuItemClickListener { item ->
                when (item.title.toString()){
                    "Ver detalle" -> {
                        onMaquinariaClick(maquinaria)
                        true
                    }
                    "Editar" -> {
                        onEditarClick(maquinaria)
                        true
                    }
                    "Cambiar estado" -> {
                        onCambiarEstadoClick(maquinaria)
                        true
                    }
                    else -> false
                }
            }
            popupMemu.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaquinariaViewHolder {
        val binding = ItemMaquinariaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MaquinariaViewHolder(binding)
    }
    override fun onBindViewHolder(holder: MaquinariaViewHolder, position: Int) {
        holder.bind(listaMaquinarias[position])
    }

    override fun getItemCount(): Int {
        return listaMaquinarias.size
    }

    fun actualizarLista(nuevaLista: List<MaquinariaModel>) {
        listaMaquinarias = nuevaLista
        notifyDataSetChanged()
    }

}