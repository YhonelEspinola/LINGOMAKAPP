package com.lingomak.lingomakapp.ui.maquinaria

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
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
    private var filterRepostajeBit = "TODOS"
    var bitacoraFechaInicio: Long = 0L
    var bitacoraFechaFin: Long = Long.MAX_VALUE

    private val expandedFilters = mutableMapOf<Int, Boolean>().apply {
        put(0, false)
        put(1, false)
    }

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
        notifyDataSetChanged() // Afecta ambos
    }

    fun updateCatalogoMaquinas(list: List<MaquinariaModel>) {
        catalogoMaquinas = list
        notifyItemChanged(1) // Solo Bitacora usa catalogoMaquinas
    }

    fun updateCatalogoOperarios(list: List<com.lingomak.lingomakapp.data.model.UserModel>) {
        catalogoOperarios = list
        notifyItemChanged(1) // Solo Bitacora usa catalogoOperarios
    }

    fun updateSearch(query: String) {
        searchQuery = query
        notifyDataSetChanged() // Afecta ambos
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        android.util.Log.d("DEBUG_BUG", "onCreateViewHolder position/viewType: $viewType")
        val binding = ItemMaquinariaAdminPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        android.util.Log.d("DEBUG_BUG", "onBindViewHolder position: $position")
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
            android.util.Log.d("DEBUG_BUG", "bind position: $position, isExpanded: $isExpanded")
            actualizarUIPorExpansion(position, isExpanded)

            binding.btnToggleFiltros.setOnClickListener {
                val newValue = !(expandedFilters[position] ?: false)
                android.util.Log.d("DEBUG_BUG", "btnToggleFiltros click position: $position, newValue: $newValue")
                expandedFilters[position] = newValue
                actualizarUIPorExpansion(position, newValue)
                binding.ivChevronFiltros.animate().rotation(if (newValue) 180f else 0f).setDuration(200).start()
                
                // AGREGADO: Si expandimos la página de Bitácora, forzamos al selector a redibujarse
                if (newValue && position == 1) {
                    binding.selectorFechas.post {
                        binding.selectorFechas.requestLayout()
                        // Buscamos el ViewPager2 interno del componente y lo obligamos a medirse
                        binding.selectorFechas.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)?.requestLayout()
                    }
                }
            }

            if (position == 0) {
                binding.rvContenido.adapter = maqAdapter
                setupPageMaquinaria()
            } else {
                binding.rvContenido.adapter = bitAdapter
                setupPageBitacora()
            }
        }

        private fun actualizarUIPorExpansion(position: Int, expanded: Boolean) {
            android.util.Log.d("DEBUG_BUG", "actualizarUIPorExpansion position: $position, expanded: $expanded")
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

            // BUG FIX: Obligar a Android a recalcular los tamaños de los componentes internos (especialmente el ViewPager2 de fechas)
            if (expanded) {
                binding.layoutFiltrosExpandible.post {
                    binding.layoutFiltrosExpandible.requestLayout()
                    if (position == 1) {
                        binding.selectorFechas.requestLayout()
                    }
                }
            }
        }

        private fun setupPageMaquinaria() {
            // Estado Dropdown
            val estados = listOf("TODOS", "OPERATIVA", "EN_MANTENIMIENTO", "INACTIVA")
            val adapterEst = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, estados)
            binding.spFiltroEstadoMaquinaria.setAdapter(adapterEst)
            binding.spFiltroEstadoMaquinaria.setText(filterEstadoMaq, false)
            binding.spFiltroEstadoMaquinaria.setOnItemClickListener { parent, _, position, _ ->
                filterEstadoMaq = parent.getItemAtPosition(position) as String
                applyFilters(0)
            }

            // Categoria Dropdown
            val cats = listOf("TODAS") + allCategorias.filter { it.tipo == "MAQUINARIA" }.map { it.nombre.uppercase() }
            val adapterCat = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, cats)
            binding.spFiltroCategoriaMaquinaria.setAdapter(adapterCat)
            binding.spFiltroCategoriaMaquinaria.setText(filterCatMaq, false)
            binding.spFiltroCategoriaMaquinaria.setOnItemClickListener { parent, _, position, _ ->
                filterCatMaq = parent.getItemAtPosition(position) as String
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

            // Repostaje Dropdown
            val repostajes = listOf("TODOS", "CON REPOSTAJE", "SIN REPOSTAJE")
            val adapterRep = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, repostajes)
            binding.spFiltroRepostajeBitacora.setAdapter(adapterRep)
            binding.spFiltroRepostajeBitacora.setText(filterRepostajeBit, false)
            binding.spFiltroRepostajeBitacora.setOnItemClickListener { parent, _, position, _ ->
                filterRepostajeBit = parent.getItemAtPosition(position) as String
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
                
                if (filterRepostajeBit != "TODOS") {
                    filtered = if (filterRepostajeBit == "CON REPOSTAJE") {
                        filtered.filter { it.galonesCombustible > 0 }
                    } else {
                        filtered.filter { it.galonesCombustible <= 0 }
                    }
                }
                
                filtered = filtered.filter { b ->
                    val date = DateUtils.convertirFecha(b.fecha)
                    if (date != null) {
                        // Forzar a comparar solo el día (00:00:00)
                        val calReg = Calendar.getInstance().apply { 
                            time = date
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        calReg.timeInMillis >= bitacoraFechaInicio && calReg.timeInMillis <= bitacoraFechaFin
                    } else false
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
