package com.lingomak.lingomakapp.ui.mantenimiento

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
    private val onMantenimientoClick: (MantenimientoModel) -> Unit,
    private val onHistoryFiltersChanged: (List<MantenimientoModel>) -> Unit
) : RecyclerView.Adapter<MantenimientoPagerAdapter.PageViewHolder>() {

    private var allMantenimientos: List<MantenimientoModel> = emptyList()
    private var allUsuarios: List<com.lingomak.lingomakapp.data.model.UserModel> = emptyList()
    private var searchQuery: String = ""
    
    // Page states
    private var filterTipo0: String = "TODOS"
    private var filterEstado0: String = "TODOS"
    
    private var filterTipo1: String = "TODOS"
    private var filterEstado1: String = "TODOS"
    private var filterUsuario1: String = "TODOS"
    var historyFechaInicio: Long = 0L
    var historyFechaFin: Long = Long.MAX_VALUE
    
    // Estado de expansión de filtros por página
    private val expandedFilters = mutableMapOf<Int, Boolean>().apply {
        put(0, false) // Contraídos por defecto en "En Cola"
        put(1, false) // Contraídos por defecto en "Historial"
    }

    fun updateData(newList: List<MantenimientoModel>) {
        allMantenimientos = newList
        notifyDataSetChanged()
    }

    fun updateUsuarios(newList: List<com.lingomak.lingomakapp.data.model.UserModel>) {
        allUsuarios = newList
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

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int = 2

    inner class PageViewHolder(val binding: ItemMantenimientoPageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val adapter = MantenimientoAdapter(
            listaMantenimientos = emptyList(),
            onMantenimientoClick = onMantenimientoClick
        )

        init {
            binding.rvMantenimientos.layoutManager = LinearLayoutManager(itemView.context)
            binding.rvMantenimientos.adapter = adapter
        }

        fun bind(position: Int) {
            setupFilters(position)
            
            // 1. Preparar qué filtros se muestran según la página (internas siempre visibles dentro de su contenedor)
            if (position == 0) {
                binding.layoutFiltroFecha.visibility = View.GONE
                binding.tilFiltroUsuario.visibility = View.GONE
                binding.tilFiltroEstado.visibility = View.VISIBLE
                binding.scrollEstado.visibility = View.GONE
                setupPage0()
            } else {
                binding.layoutFiltroFecha.visibility = View.VISIBLE
                binding.tilFiltroUsuario.visibility = View.VISIBLE
                binding.tilFiltroEstado.visibility = View.GONE
                binding.scrollEstado.visibility = View.VISIBLE
                setupPage1()
            }

            val isExpanded = expandedFilters[position] ?: false
            actualizarUIPorExpansion(position, isExpanded)

            binding.btnToggleFiltros.setOnClickListener {
                val currentValue = expandedFilters[position] ?: false
                val newValue = !currentValue
                expandedFilters[position] = newValue
                
                actualizarUIPorExpansion(position, newValue)
                binding.ivChevronFiltros.animate().rotation(if (newValue) 180f else 0f).setDuration(200).start()
                
                // Forzar despertar del selector en la pestaña de Historial
                if (newValue && position == 1) {
                    binding.selectorFechas.post {
                        binding.selectorFechas.requestLayout()
                        binding.selectorFechas.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)?.requestLayout()
                    }
                }
            }
        }

        private fun actualizarUIPorExpansion(position: Int, expanded: Boolean) {
            binding.layoutFiltrosExpandible.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.ivChevronFiltros.rotation = if (expanded) 180f else 0f
        }

        private fun setupFilters(position: Int) {
            binding.chipGroupTipo.removeAllViews()
            
            // Setup Tipo Chips
            val tipos = listOf("TODOS", "PREVENTIVO", "CORRECTIVO")
            val currentTipo = if (position == 0) filterTipo0 else filterTipo1
            
            tipos.forEach { tipo ->
                val chip = createChip(tipo, tipo)
                if (currentTipo == tipo) chip.isChecked = true
                binding.chipGroupTipo.addView(chip)
            }

            binding.chipGroupTipo.setOnCheckedStateChangeListener { group, checkedIds ->
                val chipId = checkedIds.firstOrNull()
                val selection = if (chipId != null) {
                    group.findViewById<Chip>(chipId).tag as String
                } else "TODOS"
                
                if (position == 0) filterTipo0 = selection else filterTipo1 = selection
                applyFilters(position)
            }

            if (position == 0) {
                // Estado Dropdown para "En Cola"
                val states = listOf("TODOS", "PENDIENTE", "EN_PROCESO", "VENCIDO")
                val adapter = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, states.map { it.replace("_", " ") })
                binding.spFiltroEstado.setAdapter(adapter)
                binding.spFiltroEstado.setText(filterEstado0.replace("_", " "), false)
                binding.spFiltroEstado.setOnItemClickListener { parent, _, pos, _ ->
                    val selection = states[pos]
                    filterEstado0 = selection
                    applyFilters(0)
                }
            } else {
                // Estado Chips para "Historial"
                binding.chipGroupEstado.removeAllViews()
                val states = listOf("TODOS", "FINALIZADO", "CANCELADO")
                states.forEach { state ->
                    val chip = createChip(state.replace("_", " "), state)
                    if (filterEstado1 == state) chip.isChecked = true
                    binding.chipGroupEstado.addView(chip)
                }

                binding.chipGroupEstado.setOnCheckedStateChangeListener { group, checkedIds ->
                    val chipId = checkedIds.firstOrNull()
                    filterEstado1 = if (chipId != null) group.findViewById<Chip>(chipId).tag as String else "TODOS"
                    applyFilters(1)
                }
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
            // User Dropdown (Fix 6.1)
            binding.tilFiltroUsuario.visibility = View.VISIBLE
            val usuarios = listOf("TODOS") + allUsuarios.map { it.nombre }.sorted()
            val adapterUser = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, usuarios)
            binding.spFiltroUsuario.setAdapter(adapterUser)
            binding.spFiltroUsuario.setText(filterUsuario1, false)
            binding.spFiltroUsuario.setOnItemClickListener { parent, _, position, _ ->
                filterUsuario1 = parent.getItemAtPosition(position) as String
                applyFilters(1)
            }

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

            if (position == 1 && filterUsuario1 != "TODOS") {
                filtered = filtered.filter { it.responsable == filterUsuario1 || it.resolutorNombre == filterUsuario1 }
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
                    // Priorizar fecha de finalización para el historial
                    val fechaParaFiltrar = m.fechaRealizada.takeIf { it.isNotBlank() } ?: m.fechaRegistro
                    val date = DateUtils.convertirFecha(fechaParaFiltrar)
                    if (date != null) {
                        // Normalizar a 00:00 para comparación de día puro
                        val calReg = Calendar.getInstance().apply { 
                            time = date
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        calReg.timeInMillis >= historyFechaInicio && calReg.timeInMillis <= historyFechaFin
                    } else false
                }
                // Ordenar por la misma fecha de finalización
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