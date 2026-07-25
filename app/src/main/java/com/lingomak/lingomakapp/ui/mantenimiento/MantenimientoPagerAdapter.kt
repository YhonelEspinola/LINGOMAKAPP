package com.lingomak.lingomakapp.ui.mantenimiento

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.ItemMantenimientoPageBinding
import com.lingomak.lingomakapp.utils.DateUtils
import java.util.*

class MantenimientoPagerAdapter(
    private val isOperario: Boolean,
    private val onMantenimientoClick: (MantenimientoModel) -> Unit,
    private val onEditarClick: (MantenimientoModel) -> Unit,
    private val onCambiarEstadoClick: (MantenimientoModel) -> Unit,
    private val onCancelarClick: (MantenimientoModel) -> Unit,
    private val onFinalizarClick: (MantenimientoModel) -> Unit,
    private val onHistoryFiltersChanged: (List<MantenimientoModel>) -> Unit
) : RecyclerView.Adapter<MantenimientoPagerAdapter.PageViewHolder>() {

    private var allMantenimientos: List<MantenimientoModel> = emptyList()
    private var searchQuery: String = ""
    
    // Page states
    private var filterTipo0: String = "TODOS"
    private var filterEstado0: String = "TODOS"
    
    private var filterTipo1: String = "TODOS"
    private var filterEstado1: String = "TODOS"
    var historyFechaInicio: Long = 0L
    var historyFechaFin: Long = Long.MAX_VALUE

    fun updateData(newList: List<MantenimientoModel>) {
        allMantenimientos = newList
        notifyDataSetChanged()
    }

    fun updateSearch(query: String) {
        searchQuery = query
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemMantenimientoPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = 2

    inner class PageViewHolder(val binding: ItemMantenimientoPageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val adapter = MantenimientoAdapter(
            listaMantenimientos = emptyList(),
            isOperario = isOperario,
            onMantenimientoClick = onMantenimientoClick,
            onEditarClick = onEditarClick,
            onCambiarEstadoClick = onCambiarEstadoClick,
            onCancelarClick = onCancelarClick,
            onFinalizarClick = onFinalizarClick
        )

        init {
            binding.rvMantenimientos.layoutManager = LinearLayoutManager(itemView.context)
            binding.rvMantenimientos.adapter = adapter
        }

        fun bind(position: Int) {
            setupChips(position)
            
            if (position == 0) {
                binding.layoutFiltroFecha.visibility = View.GONE
                setupPage0()
            } else {
                binding.layoutFiltroFecha.visibility = View.VISIBLE
                setupPage1()
            }
        }

        private fun setupChips(position: Int) {
            binding.chipGroupEstado.removeAllViews()
            
            val chipGroupTipo = binding.chipGroupTipo
            chipGroupTipo.setOnCheckedStateChangeListener { _, checkedIds ->
                val selection = when (checkedIds.firstOrNull()) {
                    R.id.chipTipoPreventivo -> "PREVENTIVO"
                    R.id.chipTipoCorrectivo -> "CORRECTIVO"
                    else -> "TODOS"
                }
                if (position == 0) filterTipo0 = selection else filterTipo1 = selection
                applyFilters(position)
            }

            // Restore selection
            val currentTipo = if (position == 0) filterTipo0 else filterTipo1
            when (currentTipo) {
                "PREVENTIVO" -> binding.chipTipoPreventivo.isChecked = true
                "CORRECTIVO" -> binding.chipTipoCorrectivo.isChecked = true
                else -> binding.chipTipoTodos.isChecked = true
            }

            val states = if (position == 0) listOf("PENDIENTE", "EN_PROCESO", "VENCIDO") 
                         else listOf("FINALIZADO", "CANCELADO")
            
            val currentEstado = if (position == 0) filterEstado0 else filterEstado1
            
            // Add "TODOS" for states
            val chipTodos = createChip("TODOS", "TODOS")
            if (currentEstado == "TODOS") chipTodos.isChecked = true
            binding.chipGroupEstado.addView(chipTodos)

            states.forEach { state ->
                val chip = createChip(state.replace("_", " "), state)
                if (currentEstado == state) chip.isChecked = true
                binding.chipGroupEstado.addView(chip)
            }

            binding.chipGroupEstado.setOnCheckedStateChangeListener { _, checkedIds ->
                val chipId = checkedIds.firstOrNull()
                val selection = if (chipId != null) {
                    val chip = binding.chipGroupEstado.findViewById<Chip>(chipId)
                    chip.tag as String
                } else "TODOS"
                
                if (position == 0) filterEstado0 = selection else filterEstado1 = selection
                applyFilters(position)
            }
        }

        private fun createChip(label: String, tagValue: String): Chip {
            return Chip(itemView.context).apply {
                text = label
                tag = tagValue
                isCheckable = true
                setChipBackgroundColorResource(R.color.selector_chip_choice)
                setTextColor(ContextCompat.getColorStateList(context, R.color.selector_chip_text))
            }
        }

        private fun setupPage0() {
            applyFilters(0)
        }

        private fun setupPage1() {
            binding.selectorFechas.onRangoSeleccionado = { inicio, fin, etiqueta ->
                historyFechaInicio = inicio
                historyFechaFin = fin
                binding.tvFiltroFechaEtiqueta.text = etiqueta
                applyFilters(1)
            }
            binding.selectorFechas.dispararSeleccionActual()
        }

        private fun applyFilters(position: Int) {
            var filtered = if (position == 0) {
                allMantenimientos.filter { it.estado == "PENDIENTE" || it.estado == "EN_PROCESO" || it.estado == "VENCIDO" }
            } else {
                allMantenimientos.filter { it.estado == "FINALIZADO" || it.estado == "CANCELADO" }
            }

            val currentTipo = if (position == 0) filterTipo0 else filterTipo1
            val currentEstado = if (position == 0) filterEstado0 else filterEstado1

            if (currentTipo != "TODOS") {
                filtered = filtered.filter { it.tipoMantenimiento == currentTipo }
            }
            
            if (currentEstado != "TODOS") {
                filtered = filtered.filter { it.estado == currentEstado }
            }

            if (searchQuery.isNotEmpty()) {
                filtered = filtered.filter {
                    it.codigoMantenimiento.contains(searchQuery, ignoreCase = true) ||
                    it.nombreMaquinaria.contains(searchQuery, ignoreCase = true) ||
                    it.descripcion.contains(searchQuery, ignoreCase = true)
                }
            }

            if (position == 1) {
                filtered = filtered.filter { m ->
                    val fecha = m.fechaRealizada.takeIf { it.isNotBlank() } ?: m.fechaRegistro
                    val date = DateUtils.convertirFecha(fecha)
                    if (date != null) date.time in historyFechaInicio..historyFechaFin else false
                }
                // Sort Descending for History
                filtered = filtered.sortedByDescending { 
                    val fecha = it.fechaRealizada.takeIf { it.isNotBlank() } ?: it.fechaRegistro
                    DateUtils.convertirFecha(fecha)?.time ?: 0L
                }
                // Notify parent for CSV export
                onHistoryFiltersChanged(filtered)
            } else {
                // Sort Ascending for Activos by fechaProgramada
                filtered = filtered.sortedBy { DateUtils.convertirFecha(it.fechaProgramada)?.time ?: Long.MAX_VALUE }
            }

            adapter.actualizarLista(filtered)
        }
    }
}