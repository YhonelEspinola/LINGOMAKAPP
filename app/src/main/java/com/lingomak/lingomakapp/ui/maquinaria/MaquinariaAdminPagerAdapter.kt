package com.lingomak.lingomakapp.ui.maquinaria

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.BitacoraUsoModel
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.data.model.CategoriaModel
import com.lingomak.lingomakapp.databinding.ItemMaquinariaAdminPageBinding
import com.lingomak.lingomakapp.utils.DateUtils

class MaquinariaAdminPagerAdapter(
    private val onMaquinariaClick: (MaquinariaModel) -> Unit,
    private val onBitacoraClick: (BitacoraUsoModel) -> Unit,
    private val onBitacoraFiltered: (List<BitacoraUsoModel>) -> Unit
) : RecyclerView.Adapter<MaquinariaAdminPagerAdapter.PageViewHolder>() {

    private var allMaquinarias: List<MaquinariaModel> = emptyList()
    private var allBitacora: List<BitacoraUsoModel> = emptyList()
    private var allCategorias: List<CategoriaModel> = emptyList()
    private var catalogoMaquinas: List<MaquinariaModel> = emptyList()
    private var catalogoOperarios: List<com.lingomak.lingomakapp.data.model.UserModel> = emptyList()
    private var searchQuery: String = ""
    
    val bitacoraActual: List<BitacoraUsoModel> get() = allBitacora
    val catalogoMaquinasActual: List<MaquinariaModel> get() = catalogoMaquinas
    val catalogoOperariosActual: List<com.lingomak.lingomakapp.data.model.UserModel> get() = catalogoOperarios

    // Page 0 (Maquinaria) filters
    private var filterEstadoMaq = "TODOS"
    private var filterCatMaq = "TODAS"
    
    // Page 1 (Bitacora) filters
    private var filterMaquinaBit = "TODAS"
    private var filterOperarioBit = "TODOS"
    var bitacoraFechaInicio: Long = 0L
    var bitacoraFechaFin: Long = Long.MAX_VALUE

    private val expandedFilters = mutableMapOf<Int, Boolean>().apply {
        put(0, false)
        put(1, false)
    }

    init {
        // IDs estables: con solo 2 páginas fijas (Maquinaria=0, Bitácora=1), esto le garantiza a
        // RecyclerView que la posición 0 siempre es la misma "identidad" y la 1 también, sin
        // ambigüedad al reciclar/rebindear tras volver de un detalle. Es la causa más probable
        // de que el estado de expandedFilters se desincronizara con la vista real.
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = position.toLong()

    fun updateMaquinarias(list: List<MaquinariaModel>) {
        allMaquinarias = list
        notifyItemChanged(0)
    }

    fun updateBitacora(list: List<BitacoraUsoModel>) {
        allBitacora = list
        notifyItemChanged(1)
    }

    fun updateCategorias(list: List<CategoriaModel>) {
        allCategorias = list
        notifyItemChanged(0)
    }

    fun updateCatalogoMaquinas(list: List<MaquinariaModel>) {
        catalogoMaquinas = list
        notifyItemChanged(1)
    }

    fun updateCatalogoOperarios(list: List<com.lingomak.lingomakapp.data.model.UserModel>) {
        catalogoOperarios = list
        notifyItemChanged(1)
    }

    fun updateSearch(query: String) {
        searchQuery = query
        // La búsqueda aplica a ambas pestañas.
        notifyItemChanged(0)
        notifyItemChanged(1)
    }

    fun updateNivelesCombustible(map: Map<String, com.lingomak.lingomakapp.data.repository.NivelCombustibleEstimado>) {
        this.nivelesCombustibleMap = map
        notifyItemChanged(0)
    }

    private var nivelesCombustibleMap: Map<String, com.lingomak.lingomakapp.data.repository.NivelCombustibleEstimado> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemMaquinariaAdminPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int = 2

    inner class PageViewHolder(val binding: ItemMaquinariaAdminPageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val maqAdapter = MaquinariaAdapter(emptyList(), onMaquinariaClick)
        private val bitAdapter = BitacoraUsoAdapter(emptyList(), onBitacoraClick)

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
                binding.rvContenido.adapter = maqAdapter
                maqAdapter.actualizarNivelesCombustible(nivelesCombustibleMap)
                setupPageMaquinaria()
            } else {
                binding.rvContenido.adapter = bitAdapter
                setupPageBitacora()
            }
        }

        private fun actualizarUIPorExpansion(position: Int, expanded: Boolean) {
            binding.layoutFiltrosExpandible.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.ivChevronFiltros.rotation = if (expanded) 180f else 0f
            
            // Fix 3.2: Sincronizar visibilidad de contenido interno
            if (position == 0) {
                binding.layoutFiltrosMaquinaria.visibility = if (expanded) View.VISIBLE else View.GONE
                binding.layoutFiltrosBitacora.visibility = View.GONE
                binding.layoutFiltroFecha.visibility = View.GONE
            } else {
                binding.layoutFiltrosBitacora.visibility = if (expanded) View.VISIBLE else View.GONE
                binding.layoutFiltroFecha.visibility = if (expanded) View.VISIBLE else View.GONE
                binding.layoutFiltrosMaquinaria.visibility = View.GONE
            }
        }

        private fun setupPageMaquinaria() {
            // Estado Chips
            binding.chipGroupEstadoMaquinaria.removeAllViews()
            val estados = listOf("TODOS", "OPERATIVA", "EN_MANTENIMIENTO", "INACTIVA")
            estados.forEach { est ->
                val chip = createChip(est, est)
                if (filterEstadoMaq == est) chip.isChecked = true
                binding.chipGroupEstadoMaquinaria.addView(chip)
            }
            binding.chipGroupEstadoMaquinaria.setOnCheckedStateChangeListener { group, checkedIds ->
                val chipId = checkedIds.firstOrNull()
                filterEstadoMaq = if (chipId != null) group.findViewById<Chip>(chipId).tag as String else "TODOS"
                applyFilters(0)
            }

            // Categoria Chips
            binding.chipGroupCategoriaMaquinaria.removeAllViews()
            val cats = listOf("TODAS") + allCategorias.filter { it.tipo == "MAQUINARIA" }.map { it.nombre.uppercase() }
            cats.forEach { cat ->
                val chip = createChip(cat, cat)
                if (filterCatMaq == cat) chip.isChecked = true
                binding.chipGroupCategoriaMaquinaria.addView(chip)
            }
            binding.chipGroupCategoriaMaquinaria.setOnCheckedStateChangeListener { group, checkedIds ->
                val chipId = checkedIds.firstOrNull()
                filterCatMaq = if (chipId != null) group.findViewById<Chip>(chipId).tag as String else "TODAS"
                applyFilters(0)
            }

            applyFilters(0)
        }

        private fun setupPageBitacora() {
            // Maquina Dropdown (Fix 3.1 & 3.2)
            val maquinas = listOf("TODAS") + catalogoMaquinas.map { it.nombre }.sorted()
            val adapterMaq = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, maquinas)
            binding.spFiltroMaquinaBitacora.setAdapter(adapterMaq)
            binding.spFiltroMaquinaBitacora.setText(filterMaquinaBit, false)
            binding.spFiltroMaquinaBitacora.setOnItemClickListener { parent, _, position, _ ->
                filterMaquinaBit = parent.getItemAtPosition(position) as String
                applyFilters(1)
            }

            // Operario Dropdown (Fix 3.1 & 3.2)
            val operarios = listOf("TODOS") + catalogoOperarios.map { it.nombre }.sorted()
            val adapterOp = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, operarios)
            binding.spFiltroOperarioBitacora.setAdapter(adapterOp)
            binding.spFiltroOperarioBitacora.setText(filterOperarioBit, false)
            binding.spFiltroOperarioBitacora.setOnItemClickListener { parent, _, position, _ ->
                filterOperarioBit = parent.getItemAtPosition(position) as String
                applyFilters(1)
            }

            // Fechas
            binding.selectorFechas.onRangoSeleccionado = { inicio, fin, etiqueta ->
                bitacoraFechaInicio = inicio
                bitacoraFechaFin = fin
                binding.tvFiltroFechaEtiqueta.text = etiqueta
                applyFilters(1)
            }
            binding.selectorFechas.dispararSeleccionActual()
        }

        private fun createChip(label: String, tagValue: String): Chip {
            val chip = LayoutInflater.from(itemView.context).inflate(R.layout.layout_chip_choice, binding.chipGroupEstadoMaquinaria, false) as Chip
            chip.text = label
            chip.tag = tagValue
            chip.isCheckable = true
            return chip
        }

        private fun applyFilters(position: Int) {
            if (position == 0) {
                var filtered = allMaquinarias
                if (filterEstadoMaq != "TODOS") filtered = filtered.filter { it.estado == filterEstadoMaq }
                if (filterCatMaq != "TODAS") filtered = filtered.filter { it.tipo.uppercase() == filterCatMaq }
                if (searchQuery.isNotEmpty()) {
                    filtered = filtered.filter {
                        it.nombre.contains(searchQuery, ignoreCase = true) ||
                        it.codigoMaquinaria.contains(searchQuery, ignoreCase = true) ||
                        it.placaSerie.contains(searchQuery, ignoreCase = true)
                    }
                }
                maqAdapter.actualizarLista(filtered)
            } else {
                var filtered = allBitacora
                if (filterMaquinaBit != "TODAS") filtered = filtered.filter { it.nombreMaquinaria == filterMaquinaBit }
                if (filterOperarioBit != "TODOS") filtered = filtered.filter { it.operarioNombre == filterOperarioBit }
                
                filtered = filtered.filter { b ->
                    val date = DateUtils.convertirFecha(b.fecha)
                    date != null && date.time in bitacoraFechaInicio..bitacoraFechaFin
                }

                if (searchQuery.isNotEmpty()) {
                    filtered = filtered.filter {
                        it.codigoMaquinaria.contains(searchQuery, ignoreCase = true) ||
                        it.nombreMaquinaria.contains(searchQuery, ignoreCase = true) ||
                        it.trabajoRealizado.contains(searchQuery, ignoreCase = true)
                    }
                }
                
                filtered = filtered.sortedByDescending { DateUtils.convertirFecha(it.fecha)?.time ?: 0L }
                bitAdapter.actualizarLista(filtered)
                onBitacoraFiltered(filtered)
            }
        }
    }
}
