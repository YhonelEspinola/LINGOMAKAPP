package com.lingomak.lingomakapp.ui.repuestos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.data.model.CategoriaModel
import com.lingomak.lingomakapp.databinding.ItemInventarioPageBinding
import com.lingomak.lingomakapp.ui.movimientos.MovimientosGlobalAdapter
import com.lingomak.lingomakapp.utils.DateUtils
import java.util.*

class InventarioPagerAdapter(
    private val onRepuestoClick: (RepuestoModel) -> Unit,
    private val onMovimientoClick: (Pair<MovimientoModel, String>) -> Unit,
    private val onMovimientosFiltered: (List<Pair<MovimientoModel, String>>) -> Unit
) : RecyclerView.Adapter<InventarioPagerAdapter.PageViewHolder>() {

    private var allRepuestos: List<RepuestoModel> = emptyList()
    private var allMovimientos: List<Pair<MovimientoModel, String>> = emptyList()
    private var allCategorias: List<CategoriaModel> = emptyList()
    private var allUsuarios: List<com.lingomak.lingomakapp.data.model.UserModel> = emptyList()
    private var searchQuery: String = ""

    // Page 0 (Inventario) filters
    private var filterCatInv = "TODAS LAS CATEGORÍAS"
    private var filterEstadoInv = "TODOS"
    private var filterCriticidadInv = "TODOS"

    // Page 1 (Movimientos) filters
    private var filterTipoMov = "TODOS"
    private var filterUsuarioMov = "TODOS"
    private var filterRepuestoMov = "TODOS"
    var movFechaInicio: Long = 0L
    var movFechaFin: Long = Long.MAX_VALUE

    private val expandedFilters = mutableMapOf<Int, Boolean>().apply {
        put(0, false)
        put(1, false)
    }

    fun updateRepuestos(list: List<RepuestoModel>) {
        allRepuestos = list
        notifyDataSetChanged()
    }

    fun updateMovimientos(list: List<Pair<MovimientoModel, String>>) {
        allMovimientos = list
        notifyDataSetChanged()
    }

    fun updateCategorias(list: List<CategoriaModel>) {
        allCategorias = list
        notifyDataSetChanged()
    }

    fun updateUsuarios(list: List<com.lingomak.lingomakapp.data.model.UserModel>) {
        allUsuarios = list
        notifyDataSetChanged()
    }

    fun updateSearch(query: String) {
        searchQuery = query
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemInventarioPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int = 2

    inner class PageViewHolder(val binding: ItemInventarioPageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val repoAdapter = RepuestosAdapter(emptyList(), onRepuestoClick)
        private val movAdapter = MovimientosGlobalAdapter(emptyList(), onMovimientoClick)

        init {
            binding.rvContenido.layoutManager = LinearLayoutManager(itemView.context)
        }

        fun bind(position: Int) {
            val isExpanded = expandedFilters[position] ?: false
            actualizarUIPorExpansion(position, isExpanded)

            binding.btnToggleFiltros.setOnClickListener {
                val newValue = !(expandedFilters[position] ?: false)
                expandedFilters[position] = newValue
                actualizarUIPorExpansion(position, newValue)
                binding.ivChevronFiltros.animate().rotation(if (newValue) 180f else 0f).setDuration(200).start()
            }

            if (position == 0) {
                binding.rvContenido.adapter = repoAdapter
                setupPageInventario()
            } else {
                binding.rvContenido.adapter = movAdapter
                setupPageMovimientos()
            }
        }

        private fun actualizarUIPorExpansion(position: Int, expanded: Boolean) {
            binding.layoutFiltrosExpandible.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.ivChevronFiltros.rotation = if (expanded) 180f else 0f
            
            if (position == 0) {
                binding.layoutFiltrosInventario.visibility = if (expanded) View.VISIBLE else View.GONE
                binding.layoutFiltrosMovimientos.visibility = View.GONE
                binding.layoutFiltroFecha.visibility = View.GONE
            } else {
                binding.layoutFiltrosMovimientos.visibility = if (expanded) View.VISIBLE else View.GONE
                binding.layoutFiltroFecha.visibility = if (expanded) View.VISIBLE else View.GONE
                binding.layoutFiltrosInventario.visibility = View.GONE
            }
        }

        private fun setupPageInventario() {
            // Categoría Dropdown
            val cats = listOf("TODAS LAS CATEGORÍAS") + allCategorias.filter { it.tipo == "REPUESTO" }.map { it.nombre.uppercase() }
            val adapterCat = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, cats)
            binding.spFiltroCategoria.setAdapter(adapterCat)
            binding.spFiltroCategoria.setText(filterCatInv, false)
            
            binding.spFiltroCategoria.setOnItemClickListener { parent, _, pos, _ ->
                filterCatInv = parent.getItemAtPosition(pos) as String
                applyFilters(0)
            }

            // Chips Criticidad
            binding.chipGroupStock.removeAllViews()
            val criticidades = listOf("TODOS", "CON STOCK", "BAJO STOCK", "SIN STOCK")
            criticidades.forEach { crit ->
                val chip = createChip(crit, crit, binding.chipGroupStock)
                if (filterCriticidadInv == crit) chip.isChecked = true
                binding.chipGroupStock.addView(chip)
            }
            binding.chipGroupStock.setOnCheckedStateChangeListener { group, checkedIds ->
                val chipId = checkedIds.firstOrNull()
                filterCriticidadInv = if (chipId != null) group.findViewById<Chip>(chipId).tag as String else "TODOS"
                applyFilters(0)
            }

            // Chips Estado
            binding.chipGroupEstadoInventario.removeAllViews()
            val estados = listOf("TODOS", "ACTIVO", "INACTIVO")
            estados.forEach { est ->
                val chip = createChip(est, est, binding.chipGroupEstadoInventario)
                if (filterEstadoInv == est) chip.isChecked = true
                binding.chipGroupEstadoInventario.addView(chip)
            }
            binding.chipGroupEstadoInventario.setOnCheckedStateChangeListener { group, checkedIds ->
                val chipId = checkedIds.firstOrNull()
                filterEstadoInv = if (chipId != null) group.findViewById<Chip>(chipId).tag as String else "TODOS"
                applyFilters(0)
            }

            applyFilters(0)
        }

        private fun setupPageMovimientos() {
            // Chips Tipo
            binding.chipGroupTipoMovimiento.removeAllViews()
            val tipos = listOf("TODOS", "ENTRADA", "SALIDA")
            tipos.forEach { t ->
                val chip = createChip(t, t, binding.chipGroupTipoMovimiento)
                if (filterTipoMov == t) chip.isChecked = true
                binding.chipGroupTipoMovimiento.addView(chip)
            }
            binding.chipGroupTipoMovimiento.setOnCheckedStateChangeListener { group, checkedIds ->
                val chipId = checkedIds.firstOrNull()
                filterTipoMov = if (chipId != null) group.findViewById<Chip>(chipId).tag as String else "TODOS"
                applyFilters(1)
            }

            // Usuario Dropdown
            val usuarios = listOf("TODOS") + allUsuarios.map { it.nombre }.sorted()
            val adapterUser = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, usuarios)
            binding.spFiltroUsuario.setAdapter(adapterUser)
            binding.spFiltroUsuario.setText(filterUsuarioMov, false)
            binding.spFiltroUsuario.setOnItemClickListener { parent, _, pos, _ ->
                filterUsuarioMov = parent.getItemAtPosition(pos) as String
                applyFilters(1)
            }

            // Repuesto Dropdown
            val repuestos = listOf("TODOS") + allRepuestos.map { it.nombre }.sorted()
            val adapterRep = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, repuestos)
            binding.spFiltroRepuesto.setAdapter(adapterRep)
            binding.spFiltroRepuesto.setText(filterRepuestoMov, false)
            binding.spFiltroRepuesto.setOnItemClickListener { parent, _, pos, _ ->
                filterRepuestoMov = parent.getItemAtPosition(pos) as String
                applyFilters(1)
            }

            // Fechas
            binding.selectorFechas.onRangoSeleccionado = { inicio, fin, etiqueta ->
                movFechaInicio = inicio
                movFechaFin = fin
                binding.tvFiltroFechaEtiqueta.text = etiqueta
                applyFilters(1)
            }
            binding.selectorFechas.dispararSeleccionActual()
        }

        private fun createChip(label: String, tagValue: String, group: ViewGroup): Chip {
            val chip = LayoutInflater.from(itemView.context).inflate(R.layout.layout_chip_choice, group, false) as Chip
            chip.text = label
            chip.tag = tagValue
            chip.isCheckable = true
            return chip
        }

        private fun applyFilters(position: Int) {
            if (position == 0) {
                var filtered = allRepuestos
                if (filterCatInv != "TODAS LAS CATEGORÍAS") filtered = filtered.filter { it.categoria.uppercase() == filterCatInv }
                if (filterEstadoInv != "TODOS") filtered = filtered.filter { it.estado.uppercase() == filterEstadoInv }
                
                filtered = when (filterCriticidadInv) {
                    "SIN STOCK" -> filtered.filter { it.stockActual == 0 }
                    "BAJO STOCK" -> filtered.filter { it.stockActual in 1..it.stockMinimo }
                    "CON STOCK" -> filtered.filter { it.stockActual > it.stockMinimo }
                    else -> filtered
                }

                if (searchQuery.isNotEmpty()) {
                    filtered = filtered.filter {
                        it.nombre.contains(searchQuery, ignoreCase = true) ||
                        it.codigoInterno.contains(searchQuery, ignoreCase = true)
                    }
                }
                repoAdapter.actualizarLista(filtered)
            } else {
                var filtered = allMovimientos
                if (filterTipoMov != "TODOS") filtered = filtered.filter { it.first.tipo == filterTipoMov }
                if (filterUsuarioMov != "TODOS") filtered = filtered.filter { it.first.nombreRegistradoPor == filterUsuarioMov }
                if (filterRepuestoMov != "TODOS") filtered = filtered.filter { it.second == filterRepuestoMov }
                
                filtered = filtered.filter { m ->
                    val date = m.first.fecha
                    date != null && date.time in movFechaInicio..movFechaFin
                }

                if (searchQuery.isNotEmpty()) {
                    filtered = filtered.filter {
                        it.second.contains(searchQuery, ignoreCase = true) ||
                        it.first.observacion.contains(searchQuery, ignoreCase = true)
                    }
                }
                
                filtered = filtered.sortedByDescending { it.first.fecha?.time ?: 0L }
                movAdapter.actualizarLista(filtered)
                onMovimientosFiltered(filtered)
            }
        }
    }
}
