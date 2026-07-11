package com.lingomak.lingomakapp.ui.mantenimiento

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.ItemMantenimientoBinding
import android.widget.PopupMenu
import com.lingomak.lingomakapp.R
class MantenimientoAdapter(
    private var listaMantenimientos: List<MantenimientoModel>,
    private val isOperario: Boolean = false,
    private val onMantenimientoClick: (MantenimientoModel) -> Unit,
    private val onEditarClick: (MantenimientoModel) -> Unit,
    private val onCambiarEstadoClick : (MantenimientoModel) -> Unit,
    private val onCancelarClick: (MantenimientoModel) -> Unit,
    private val onFinalizarClick : (MantenimientoModel) -> Unit

) : RecyclerView.Adapter<MantenimientoAdapter.MantenimientoViewHolder>() {

    inner class MantenimientoViewHolder(
        private val binding: ItemMantenimientoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mantenimiento: MantenimientoModel) {


            binding.tvTipoMantenimiento.text = mantenimiento.tipoMantenimiento

            binding.tvCodigoMantenimiento.text = mantenimiento.codigoMantenimiento

            binding.tvDescripcionMantenimiento.text = mantenimiento.descripcion

            binding.tvMaquinariaMantenimiento.text = mantenimiento.nombreMaquinaria

            binding.tvFechaProgramada.text = "📅 ${mantenimiento.fechaProgramada}"

            binding.tvHorometroMantenimiento.text =
                "⏱ ${mantenimiento.horometroProgramado} h"

            binding.tvResponsableMantenimiento.text =
                "👤 ${mantenimiento.responsable}"

            binding.tvEstadoMantenimiento.text = mantenimiento.estado

            binding.tvPrioridadMantenimiento.text = "⚑ Prioridad: Media"

            aplicarColorTipo(mantenimiento.tipoMantenimiento)

            aplicarColorEstado(mantenimiento.estado)

            binding.btnOpciones.setOnClickListener {

                val popupMenu = PopupMenu(binding.root.context, binding.btnOpciones)

                popupMenu.menuInflater.inflate(
                    R.menu.menu_mantenimiento_item,
                    popupMenu.menu
                )

                // Restricciones por Rol
                if (isOperario) {
                    popupMenu.menu.findItem(R.id.opcion_editar).isVisible = false
                    popupMenu.menu.findItem(R.id.opcion_cancelar).isVisible = false
                }

                when (mantenimiento.estado) {

                    "PENDIENTE" -> {
                        popupMenu.menu.findItem(R.id.opcion_finalizar).isVisible = false
                    }

                    "EN_PROCESO" -> {
                        popupMenu.menu.findItem(R.id.opcion_editar).isVisible = false
                        popupMenu.menu.findItem(R.id.opcion_cambiar_estado).isVisible = false
                        popupMenu.menu.findItem(R.id.opcion_cancelar).isVisible = false
                    }

                    "FINALIZADO", "VENCIDO", "CANCELADO" -> {
                        popupMenu.menu.findItem(R.id.opcion_editar).isVisible = false
                        popupMenu.menu.findItem(R.id.opcion_cambiar_estado).isVisible = false
                        popupMenu.menu.findItem(R.id.opcion_cancelar).isVisible = false
                        popupMenu.menu.findItem(R.id.opcion_finalizar).isVisible = false
                    }
                }

                popupMenu.setOnMenuItemClickListener { item ->

                    when (item.itemId) {

                        R.id.opcion_ver_detalle -> {
                            onMantenimientoClick(mantenimiento)
                            true
                        }

                        R.id.opcion_editar -> {
                            onEditarClick(mantenimiento)
                            true
                        }

                        R.id.opcion_cambiar_estado -> {
                            onCambiarEstadoClick(mantenimiento)
                            true
                        }

                        R.id.opcion_cancelar -> {
                            onCancelarClick(mantenimiento)
                            true
                        }

                        R.id.opcion_finalizar -> {
                            onFinalizarClick(mantenimiento)
                            true
                        }

                        else -> false
                    }
                }

                popupMenu.show()
            }

            binding.root.setOnClickListener {
                onMantenimientoClick(mantenimiento)
            }
        }

        private fun aplicarColorTipo(tipo: String) {

            when (tipo) {

                "PREVENTIVO" -> {
                    binding.tvTipoMantenimiento.setTextColor(Color.rgb(37, 99, 235))
                }

                "CORRECTIVO" -> {
                    binding.tvTipoMantenimiento.setTextColor(Color.rgb(234, 88, 12))
                }

                "PREDICTIVO" -> {
                    binding.tvTipoMantenimiento.setTextColor(Color.rgb(124, 58, 237))
                }

                else -> {
                    binding.tvTipoMantenimiento.setTextColor(Color.rgb(55, 65, 81))
                }
            }
        }

        private fun aplicarColorEstado(estado: String) {

            when (estado) {

                "PENDIENTE" -> {
                    binding.tvEstadoMantenimiento.setTextColor(Color.rgb(180, 83, 9))
                }

                "EN_PROCESO" -> {
                    binding.tvEstadoMantenimiento.text = "EN PROCESO"
                    binding.tvEstadoMantenimiento.setTextColor(Color.rgb(37, 99, 235))
                }

                "FINALIZADO" -> {
                    binding.tvEstadoMantenimiento.setTextColor(Color.rgb(22, 101, 52))
                }

                "VENCIDO" -> {
                    binding.tvEstadoMantenimiento.setTextColor(Color.rgb(185, 28, 28))
                }

                else -> {
                    binding.tvEstadoMantenimiento.setTextColor(Color.rgb(55, 65, 81))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MantenimientoViewHolder {

        val binding = ItemMantenimientoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MantenimientoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MantenimientoViewHolder, position: Int) {
        holder.bind(listaMantenimientos[position])
    }

    override fun getItemCount(): Int {
        return listaMantenimientos.size
    }

    fun actualizarLista(nuevaLista: List<MantenimientoModel>) {
        listaMantenimientos = nuevaLista
        notifyDataSetChanged()
    }
}