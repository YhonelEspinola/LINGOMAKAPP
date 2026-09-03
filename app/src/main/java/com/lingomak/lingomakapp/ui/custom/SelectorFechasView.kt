package com.lingomak.lingomakapp.ui.custom

import android.app.DatePickerDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.lingomak.lingomakapp.R
import java.text.SimpleDateFormat
import android.view.MotionEvent
import java.util.*

class SelectorFechasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager2
    private val dots: List<View>
    
    var onRangoSeleccionado: ((fechaInicio: Long, fechaFin: Long, etiqueta: String) -> Unit)? = null

    fun dispararSeleccionActual() {
        val current = viewPager.currentItem
        when(current) {
            0 -> { // Modo Chips
                val calFin = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }
                val calInicio = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                
                // Intentar recuperar el chip seleccionado para disparar el rango correcto
                val etiqueta = "Últimos 30 días" // Default
                calInicio.add(Calendar.DAY_OF_YEAR, -30)
                onRangoSeleccionado?.invoke(calInicio.timeInMillis, calFin.timeInMillis, etiqueta)
            }
            1 -> { // Modo Nav
                val (inicio, fin, label) = calcularRangoNav()
                onRangoSeleccionado?.invoke(inicio, fin, label)
            }
            2 -> { // Modo Custom
                val calFinReal = Calendar.getInstance().apply {
                    timeInMillis = customFechaFin
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }
                val calInicioReal = Calendar.getInstance().apply {
                    timeInMillis = customFechaInicio
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val etiqueta = "${displayFormat.format(calInicioReal.timeInMillis)} — ${displayFormat.format(calFinReal.timeInMillis)}"
                onRangoSeleccionado?.invoke(calInicioReal.timeInMillis, calFinReal.timeInMillis, etiqueta)
            }
        }
    }

    // Estado para Modo Nav
    private var tipoPeriodoActual = "ESTE MES" // HOY, ESTA SEMANA, ESTE MES, ESTE AÑO
    private var offsetPeriodo = 0

    // Estado para Modo Custom
    private var customFechaInicio = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
    private var customFechaFin = Calendar.getInstance().timeInMillis

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    init {
        LayoutInflater.from(context).inflate(R.layout.view_selector_fechas, this, true)
        viewPager = findViewById(R.id.viewPager)
        dots = listOf(
            findViewById(R.id.dot0),
            findViewById(R.id.dot1),
            findViewById(R.id.dot2)
        )

        setupViewPager()
    }

    private fun setupViewPager() {
        viewPager.adapter = SelectorAdapter()
        val innerRecyclerView = viewPager.getChildAt(0) as? RecyclerView

        // INTERCEPTOR DE GESTOS: Esta es la solución para la "capa partida".
        // Detecta el toque antes que los botones internos para bloquear al ViewPager padre.
        innerRecyclerView?.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_DOWN) {
                    // "Secuestramos" el toque para que el padre externo no se mueva
                    this@SelectorFechasView.parent.requestDisallowInterceptTouchEvent(true)
                }
                return false // Importante retornar false para que los botones sigan siendo clickeables
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                actualizarDots(position)
            }
        })

        // Empezar en Modo 1 (chips) y disparar 30D por defecto
        viewPager.post {
            viewPager.setCurrentItem(0, false)
            actualizarDots(0)
            // Notificaremos 30D cuando el adapter esté listo o mediante interacción
        }
    }

    private fun actualizarDots(position: Int) {
        dots.forEachIndexed { index, dot -> dot.isSelected = index == position }
    }

    inner class SelectorAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemViewType(position: Int): Int = position
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                0 -> ChipsViewHolder(inflater.inflate(R.layout.item_selector_page_chips, parent, false))
                1 -> NavViewHolder(inflater.inflate(R.layout.item_selector_page_nav, parent, false))
                else -> CustomViewHolder(inflater.inflate(R.layout.item_selector_page_custom, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is ChipsViewHolder -> holder.bind()
                is NavViewHolder -> holder.bind()
                is CustomViewHolder -> holder.bind()
            }
        }
        override fun getItemCount(): Int = 3
    }

    // --- MODO 1: CHIPS ---
    inner class ChipsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val radioGroup: RadioGroup = view.findViewById(R.id.radioGroupChips)
        fun bind() {
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                val calInicio = Calendar.getInstance()
                val calFin = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }

                val etiqueta = when (checkedId) {
                    R.id.chip7D -> {
                        calInicio.add(Calendar.DAY_OF_YEAR, -7)
                        "Últimos 7 días"
                    }
                    R.id.chip30D -> {
                        calInicio.add(Calendar.DAY_OF_YEAR, -30)
                        "Últimos 30 días"
                    }
                    R.id.chip12S -> {
                        calInicio.add(Calendar.WEEK_OF_YEAR, -12)
                        "Últimas 12 semanas"
                    }
                    R.id.chip6M -> {
                        calInicio.add(Calendar.MONTH, -6)
                        "Últimos 6 meses"
                    }
                    R.id.chip1A -> {
                        calInicio.add(Calendar.YEAR, -1)
                        "Último año"
                    }
                    else -> "Últimos 30 días"
                }
                
                // Normalizar horas para el filtro (Día completo)
                calInicio.set(Calendar.HOUR_OF_DAY, 0); calInicio.set(Calendar.MINUTE, 0); calInicio.set(Calendar.SECOND, 0); calInicio.set(Calendar.MILLISECOND, 0)
                calFin.set(Calendar.HOUR_OF_DAY, 23); calFin.set(Calendar.MINUTE, 59); calFin.set(Calendar.SECOND, 59); calFin.set(Calendar.MILLISECOND, 999)

                onRangoSeleccionado?.invoke(calInicio.timeInMillis, calFin.timeInMillis, etiqueta)
            }
            // Disparar selección inicial
            val checkedId = radioGroup.checkedRadioButtonId
            if (checkedId != -1) {
                val calFin = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                val calInicio = Calendar.getInstance()
                val etiqueta = when (checkedId) {
                    R.id.chip7D -> { calInicio.add(Calendar.DAY_OF_YEAR, -7); "Últimos 7 días" }
                    R.id.chip30D -> { calInicio.add(Calendar.DAY_OF_YEAR, -30); "Últimos 30 días" }
                    R.id.chip12S -> { calInicio.add(Calendar.WEEK_OF_YEAR, -12); "Últimas 12 semanas" }
                    R.id.chip6M -> { calInicio.add(Calendar.MONTH, -6); "Últimos 6 meses" }
                    R.id.chip1A -> { calInicio.add(Calendar.YEAR, -1); "Último año" }
                    else -> "Últimos 30 días"
                }
                onRangoSeleccionado?.invoke(calInicio.timeInMillis, calFin.timeInMillis, etiqueta)
            }
        }
    }

    // --- MODO 2: NAV ---
    inner class NavViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val btnPrev: ImageButton = view.findViewById(R.id.btnPrev)
        private val btnNext: ImageButton = view.findViewById(R.id.btnNext)
        private val tvLabel: TextView = view.findViewById(R.id.tvPeriodoLabel)

        fun bind() {
            actualizarNav()
            tvLabel.setOnClickListener { mostrarMenuPeriodos(it) }
            btnPrev.setOnClickListener {
                offsetPeriodo--
                actualizarNav()
            }
            btnNext.setOnClickListener {
                offsetPeriodo++
                actualizarNav()
            }
        }

        private fun actualizarNav() {
            val (inicio, fin, label) = calcularRangoNav()
            tvLabel.text = "$label ▼"
            onRangoSeleccionado?.invoke(inicio, fin, label)
        }

        private fun mostrarMenuPeriodos(view: View) {
            val popup = PopupMenu(context, view)
            popup.menu.add("Hoy")
            popup.menu.add("Esta semana")
            popup.menu.add("Este mes")
            popup.menu.add("Este año")
            popup.setOnMenuItemClickListener { item ->
                tipoPeriodoActual = item.title.toString().uppercase()
                offsetPeriodo = 0
                actualizarNav()
                true
            }
            popup.show()
        }
    }

    private fun calcularRangoNav(): Triple<Long, Long, String> {
        val cal = Calendar.getInstance()
        val calFin = Calendar.getInstance()
        var label = ""

        when (tipoPeriodoActual) {
            "HOY" -> {
                cal.add(Calendar.DAY_OF_YEAR, offsetPeriodo)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                
                calFin.time = cal.time
                calFin.set(Calendar.HOUR_OF_DAY, 23)
                calFin.set(Calendar.MINUTE, 59)
                calFin.set(Calendar.SECOND, 59)
                calFin.set(Calendar.MILLISECOND, 999)

                label = when (offsetPeriodo) {
                    1 -> "MAÑANA"
                    0 -> "HOY"
                    -1 -> "AYER"
                    else -> displayFormat.format(cal.time)
                }
            }
            "ESTA SEMANA" -> {
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.add(Calendar.WEEK_OF_YEAR, offsetPeriodo)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                calFin.time = cal.time
                calFin.add(Calendar.DAY_OF_YEAR, 6)
                calFin.set(Calendar.HOUR_OF_DAY, 23)
                calFin.set(Calendar.MINUTE, 59)
                calFin.set(Calendar.SECOND, 59)
                calFin.set(Calendar.MILLISECOND, 999)

                label = "${displayFormat.format(cal.time)} — ${displayFormat.format(calFin.time)}"
            }
            "ESTE MES" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, offsetPeriodo)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                calFin.time = cal.time
                calFin.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                calFin.set(Calendar.HOUR_OF_DAY, 23)
                calFin.set(Calendar.MINUTE, 59)
                calFin.set(Calendar.SECOND, 59)
                calFin.set(Calendar.MILLISECOND, 999)

                label = monthYearFormat.format(cal.time)
            }
            "ESTE AÑO" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.add(Calendar.YEAR, offsetPeriodo)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                calFin.time = cal.time
                calFin.set(Calendar.MONTH, 11)
                calFin.set(Calendar.DAY_OF_MONTH, 31)
                calFin.set(Calendar.HOUR_OF_DAY, 23)
                calFin.set(Calendar.MINUTE, 59)
                calFin.set(Calendar.SECOND, 59)
                calFin.set(Calendar.MILLISECOND, 999)

                label = cal.get(Calendar.YEAR).toString()
            }
        }
        return Triple(cal.timeInMillis, calFin.timeInMillis, label)
    }

    // --- MODO 3: CUSTOM ---
    inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val btnInicio: Button = view.findViewById(R.id.btnFechaInicio)
        private val btnFin: Button = view.findViewById(R.id.btnFechaFin)

        fun bind() {
            actualizarBotones()
            btnInicio.setOnClickListener { mostrarDatePicker(true) }
            btnFin.setOnClickListener { mostrarDatePicker(false) }
        }

        private fun mostrarDatePicker(esInicio: Boolean) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = if (esInicio) customFechaInicio else customFechaFin
            
            // Forzar Locale para los textos internos (días de la semana, meses)
            val locale = Locale("es", "ES")
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)

            val dialog = DatePickerDialog(context, { _, y, m, d ->
                val startCal = Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                val endCal = Calendar.getInstance().apply {
                    set(y, m, d, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                if (esInicio) {
                    customFechaInicio = startCal.timeInMillis
                } else {
                    customFechaFin = endCal.timeInMillis
                }

                // Validar orden
                if (customFechaInicio > customFechaFin) {
                    val temp = customFechaInicio
                    // Al invertir, debemos asegurar que el nuevo fin sea el final del día
                    val calTemp = Calendar.getInstance().apply { 
                        timeInMillis = customFechaFin 
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) 
                    }
                    val calTempEnd = Calendar.getInstance().apply {
                        timeInMillis = temp
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
                    }
                    customFechaInicio = calTemp.timeInMillis
                    customFechaFin = calTempEnd.timeInMillis
                }
                
                actualizarBotones()
                val etiqueta = "${displayFormat.format(customFechaInicio)} — ${displayFormat.format(customFechaFin)}"
                onRangoSeleccionado?.invoke(customFechaInicio, customFechaFin, etiqueta)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

            dialog.show()
        }

        private fun actualizarBotones() {
            btnInicio.text = dateFormat.format(Date(customFechaInicio))
            
            val calHoy = Calendar.getInstance()
            val calFin = Calendar.getInstance().apply { timeInMillis = customFechaFin }
            
            if (calHoy.get(Calendar.YEAR) == calFin.get(Calendar.YEAR) &&
                calHoy.get(Calendar.DAY_OF_YEAR) == calFin.get(Calendar.DAY_OF_YEAR)) {
                btnFin.text = "Hoy"
            } else {
                btnFin.text = dateFormat.format(Date(customFechaFin))
            }
        }
    }
}
