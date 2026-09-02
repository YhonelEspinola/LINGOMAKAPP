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
import java.util.*

class SelectorFechasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager2
    private val dots: List<View>
    
    var onRangoSeleccionado: ((fechaInicio: Long, fechaFin: Long, etiqueta: String) -> Unit)? = null

    fun dispararSeleccionActual() {
        val current = viewPager.currentItem
        // Esto es un poco complejo porque necesitamos acceder a los viewholders.
        // Una mejor forma es que el View maneje el estado y notifique.
        when(current) {
            0 -> {
                // Notificar 30D por defecto o el chip seleccionado
                val calInicio = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
                val calFin = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                onRangoSeleccionado?.invoke(calInicio.timeInMillis, calFin.timeInMillis, "Últimos 30 días")
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

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isDraggingHorizontally = false
    private val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop

    /**
     * Por qué el fix anterior (un simple setOnTouchListener en la vista raíz) no funcionaba:
     * item_selector_page_chips.xml tiene RadioButtons ocupando casi todo el ancho de la página.
     * Un View.OnTouchListener puesto en un ViewGroup padre NUNCA recibe los eventos si un hijo
     * clickable (como un RadioButton) los consume desde el ACTION_DOWN — el listener del padre
     * simplemente no se dispara para esos toques, sin importar dónde esté puesto.
     *
     * La única forma correcta de "robarle" el gesto a un hijo clickable a mitad de camino es
     * sobrescribir onInterceptTouchEvent en el ViewGroup padre: se deja pasar el ACTION_DOWN al
     * hijo normalmente (return false), pero si en un ACTION_MOVE posterior se detecta arrastre
     * horizontal claro (mayor que el touchSlop y más horizontal que vertical), se empieza a
     * interceptar (return true) — Android automáticamente cancela el gesto en el hijo (le manda
     * ACTION_CANCEL) y las siguientes MOVE/UP se procesan en onTouchEvent de este padre.
     */
    override fun onInterceptTouchEvent(ev: android.view.MotionEvent): Boolean {
        when (ev.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                touchDownX = ev.x
                touchDownY = ev.y
                isDraggingHorizontally = false
                // Solicitamos a TODOS los padres que no intercepten toques al empezar.
                // Esto es clave para que el ViewPager2 principal deje de "robar" el evento.
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                val deltaX = ev.x - touchDownX
                val deltaY = ev.y - touchDownY
                
                // Si detectamos que es un movimiento vertical, le devolvemos el control al padre
                if (kotlin.math.abs(deltaY) > touchSlop && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX)) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return false
                }

                if (!isDraggingHorizontally &&
                    kotlin.math.abs(deltaX) > touchSlop &&
                    kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY)
                ) {
                    isDraggingHorizontally = true
                    return true 
                }
            }
        }
        return false
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> return true // Necesario para recibir MOVE/UP
            android.view.MotionEvent.ACTION_MOVE -> return true
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                if (isDraggingHorizontally) {
                    val deltaX = event.x - touchDownX
                    if (kotlin.math.abs(deltaX) > touchSlop) {
                        if (deltaX < 0 && viewPager.currentItem < 2) {
                            viewPager.setCurrentItem(viewPager.currentItem + 1, true)
                        } else if (deltaX > 0 && viewPager.currentItem > 0) {
                            viewPager.setCurrentItem(viewPager.currentItem - 1, true)
                        }
                    }
                }
                isDraggingHorizontally = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return false
    }

    private fun setupViewPager() {
        viewPager.adapter = SelectorAdapter()

        // Fix clásico para ViewPager2 anidado: evitar que el padre (otro ViewPager2)
        // intercepte el gesto de arrastre horizontal cuando el dedo está directamente
        // sobre el RecyclerView interno del ViewPager2 (área donde SÍ funcionaba antes).
        val innerRecyclerView = viewPager.getChildAt(0) as? RecyclerView
        innerRecyclerView?.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> parent.requestDisallowInterceptTouchEvent(true)
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                    parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

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
                    
                    calFin.time = cal.time
                    calFin.set(Calendar.HOUR_OF_DAY, 23)
                    calFin.set(Calendar.MINUTE, 59)
                    calFin.set(Calendar.SECOND, 59)

                    label = when (offsetPeriodo) {
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

                    calFin.time = cal.time
                    calFin.add(Calendar.DAY_OF_YEAR, 6)
                    calFin.set(Calendar.HOUR_OF_DAY, 23)
                    calFin.set(Calendar.MINUTE, 59)
                    calFin.set(Calendar.SECOND, 59)

                    label = "${displayFormat.format(cal.time)} — ${displayFormat.format(calFin.time)}"
                }
                "ESTE MES" -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.add(Calendar.MONTH, offsetPeriodo)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)

                    calFin.time = cal.time
                    calFin.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    calFin.set(Calendar.HOUR_OF_DAY, 23)
                    calFin.set(Calendar.MINUTE, 59)
                    calFin.set(Calendar.SECOND, 59)

                    label = monthYearFormat.format(cal.time)
                }
                "ESTE AÑO" -> {
                    cal.set(Calendar.DAY_OF_YEAR, 1)
                    cal.add(Calendar.YEAR, offsetPeriodo)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)

                    calFin.time = cal.time
                    calFin.set(Calendar.MONTH, 11)
                    calFin.set(Calendar.DAY_OF_MONTH, 31)
                    calFin.set(Calendar.HOUR_OF_DAY, 23)
                    calFin.set(Calendar.MINUTE, 59)
                    calFin.set(Calendar.SECOND, 59)

                    label = cal.get(Calendar.YEAR).toString()
                }
            }
            return Triple(cal.timeInMillis, calFin.timeInMillis, label)
        }
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
                val selection = Calendar.getInstance()
                selection.set(y, m, d)
                if (esInicio) {
                    customFechaInicio = selection.timeInMillis
                } else {
                    customFechaFin = selection.timeInMillis
                }

                // Validar orden
                if (customFechaInicio > customFechaFin) {
                    val temp = customFechaInicio
                    customFechaInicio = customFechaFin
                    customFechaFin = temp
                }
                
                actualizarBotones()
                val etiqueta = "${displayFormat.format(customFechaInicio)} — ${displayFormat.format(customFechaFin)}"
                
                // Asegurar fin del día para fecha fin
                val calFinReal = Calendar.getInstance().apply {
                    timeInMillis = customFechaFin
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                onRangoSeleccionado?.invoke(customFechaInicio, calFinReal.timeInMillis, etiqueta)
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
