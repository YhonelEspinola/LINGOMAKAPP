package com.lingomak.lingomakapp.ui.movimientos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import java.util.Locale
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.ItemEstadisticaPageBinding
import com.lingomak.lingomakapp.utils.RoundedBarChartRenderer

class EstadisticasPagerAdapter() : RecyclerView.Adapter<EstadisticasPagerAdapter.ViewHolder>() {

    private var data: MovimientosEstadisticasViewModel.EstadisticasData? = null
    private val expandedPages = mutableSetOf<Int>()

    fun updateData(newData: MovimientosEstadisticasViewModel.EstadisticasData) {
        this.data = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEstadisticaPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentData = data ?: return
        holder.bind(position, currentData, expandedPages.contains(position)) {
            expandedPages.add(position)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = 9

    inner class ViewHolder(val binding: ItemEstadisticaPageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(position: Int, data: MovimientosEstadisticasViewModel.EstadisticasData, isExpanded: Boolean, onExpand: () -> Unit) {
            resetVisibility()
            
            when (position) {
                0 -> setupP1(data)
                1 -> setupP2(data)
                2 -> setupP3(data, isExpanded, onExpand)
                3 -> setupP4(data, isExpanded, onExpand)
                4 -> setupP5(data, isExpanded, onExpand)
                5 -> setupP6(data)
                6 -> setupP8(data)
                7 -> setupP9(data)
                8 -> setupP10(data)
            }
        }

        private fun resetVisibility() {
            binding.layoutP1Resumen.visibility = View.GONE
            binding.layoutP2Tendencia.visibility = View.GONE
            binding.layoutTablaGenerica.visibility = View.GONE
            binding.layoutChartGenerico.visibility = View.GONE
            binding.layoutP8Costos.visibility = View.GONE
            binding.tvEstadoVacioTabla.visibility = View.GONE
            binding.tvEstadoVacioChart.visibility = View.GONE
            binding.chartPieGenerico.visibility = View.GONE
            binding.scrollChartBar.visibility = View.GONE
            binding.tvSubtituloPagina.visibility = View.GONE
            binding.frameVerMas.visibility = View.GONE
        }

        private fun setupP1(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Resumen General"
            binding.layoutP1Resumen.visibility = View.VISIBLE
            binding.tvP1Entradas.text = data.totalEntradas.toString()
            binding.tvP1Salidas.text = data.totalSalidas.toString()
            binding.tvP1Total.text = data.totalMovimientos.toString()
            binding.tvP1ResumenTexto.text = "${data.resumenTexto}\nProducto más usado: ${data.productoMasUsado}"
            
            setupBarChart(binding.chartP1, listOf("Entradas", "Salidas"), listOf(
                BarEntry(0f, data.totalEntradas.toFloat()),
                BarEntry(1f, data.totalSalidas.toFloat())
            ), listOf(getColor(R.color.success), getColor(R.color.primary)))
        }

        private fun setupP2(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Tendencia Mensual"
            binding.layoutP2Tendencia.visibility = View.VISIBLE
            
            val entriesEntradas = data.tendenciaMensual.mapIndexed { i, pair -> Entry(i.toFloat(), pair.second.first.toFloat()) }
            val entriesSalidas = data.tendenciaMensual.mapIndexed { i, pair -> Entry(i.toFloat(), pair.second.second.toFloat()) }
            
            val colorEntradas = getColor(R.color.success)
            val colorSalidas = getColor(R.color.primary)

            val setEnt = LineDataSet(entriesEntradas, "Entradas").apply {
                setColor(colorEntradas)
                setCircleColor(colorEntradas)
                lineWidth = 4f
                circleRadius = 5f
                setDrawValues(false)
                setDrawCircleHole(false)
            }

            val setSal = LineDataSet(entriesSalidas, "Salidas").apply {
                setColor(colorSalidas)
                setCircleColor(colorSalidas)
                lineWidth = 4f
                circleRadius = 5f
                setDrawValues(false)
                setDrawCircleHole(false)
            }

            binding.chartP2.apply {
                val lineData = LineData(setEnt, setSal)
                this.data = lineData
                xAxis.valueFormatter = IndexAxisValueFormatter(data.tendenciaMensual.map { it.first })
                xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.textColor = getColor(R.color.text_primary)
                axisLeft.textColor = getColor(R.color.text_primary)
                axisRight.isEnabled = false
                description.isEnabled = false
                
                legend.apply {
                    isEnabled = true
                    textColor = getColor(R.color.text_primary)
                    verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                    horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                    orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                    setDrawInside(false)
                }

                if (tag == null) {
                    animateX(1000)
                    tag = "animated"
                }

                notifyDataSetChanged()
                invalidate()
            }
        }

        private fun setupP3(data: MovimientosEstadisticasViewModel.EstadisticasData, isExpanded: Boolean, onExpand: () -> Unit) {
            binding.tvTituloPagina.text = "Top Repuestos Consumidos"
            val displayList = if (isExpanded) data.topProductos else data.topProductos.take(10)
            setupTable("Repuesto", "Salidas", displayList)
            
            if (!isExpanded && data.topProductos.size > 10) {
                binding.frameVerMas.visibility = View.VISIBLE
                binding.btnVerMas.setOnClickListener { onExpand() }
            }
        }

        private fun setupP4(data: MovimientosEstadisticasViewModel.EstadisticasData, isExpanded: Boolean, onExpand: () -> Unit) {
            binding.tvTituloPagina.text = "Baja/Nula Rotación"
            val displayList = if (isExpanded) data.bajaRotacion else data.bajaRotacion.take(10)
            setupTable("Repuesto", "Salidas", displayList)
            
            if (!isExpanded && data.bajaRotacion.size > 10) {
                binding.frameVerMas.visibility = View.VISIBLE
                binding.btnVerMas.setOnClickListener { onExpand() }
            }
        }

        private fun setupP5(data: MovimientosEstadisticasViewModel.EstadisticasData, isExpanded: Boolean, onExpand: () -> Unit) {
            binding.tvTituloPagina.text = "Repuestos consumidos por máquina"
            binding.tvSubtituloPagina.apply {
                text = "Desglose de cantidades utilizadas en cada equipo"
                visibility = View.VISIBLE
            }
            
            val displayList = if (isExpanded) data.consumoMaquina else data.consumoMaquina.take(10)
            setupTable("Máquina", "Cantidad", displayList)
            
            if (!isExpanded && data.consumoMaquina.size > 10) {
                binding.frameVerMas.visibility = View.VISIBLE
                binding.btnVerMas.setOnClickListener { onExpand() }
            }
        }

        private fun setupP6(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Salidas: Consumo interno vs distribución externa"
            binding.layoutChartGenerico.visibility = View.VISIBLE
            binding.chartPieGenerico.visibility = View.VISIBLE
            
            val entries = data.distribucionSalida.map { PieEntry(it.value.toFloat(), if(it.key == "CONSUMO_INTERNO") "Interno" else "Externo") }
            if (entries.isEmpty()) {
                binding.tvEstadoVacioChart.visibility = View.VISIBLE
                binding.chartPieGenerico.visibility = View.GONE
            } else {
                setupPieChart(binding.chartPieGenerico, entries, listOf(getColor(R.color.primary), getColor(R.color.brand_yellow)))
            }
        }

        private fun setupP8(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Costo real vs estimado (mantenimientos)"
            binding.layoutP8Costos.visibility = View.VISIBLE
            binding.tvP8Diferencia.text = data.costosComparativa.third
            
            setupBarChart(binding.chartP8, listOf("Estimado", "Real"), listOf(
                BarEntry(0f, data.costosComparativa.first.toFloat()),
                BarEntry(1f, data.costosComparativa.second.toFloat())
            ), listOf(getColor(R.color.brand_yellow), getColor(R.color.primary)))
        }

        private fun setupP9(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Top 5 máquinas — menor consumo"
            binding.tvSubtituloPagina.apply {
                text = "Maquinarias con mejor rendimiento de combustible"
                visibility = View.VISIBLE
            }
            setupTableDouble("Máquina", "Consumo", data.topMenorConsumo)
        }

        private fun setupP10(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Rendimiento — Horas por Operario"
            binding.tvSubtituloPagina.apply {
                text = "Total de horas trabajadas agrupadas por mes"
                visibility = View.VISIBLE
            }
            setupTableRendimiento(data.rendimientoOperarios)
        }

        private fun setupBarChart(chart: BarChart, labels: List<String>, entries: List<BarEntry>, colors: List<Int>, rotateLabels: Boolean = false) {
            val textColor = getColor(R.color.text_primary)
            val dataSet = BarDataSet(entries, "").apply {
                this.colors = colors
                valueTextSize = 10f
                valueTextColor = textColor
            }
            chart.apply {
                this.data = BarData(dataSet)
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(labels)
                    position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    isGranularityEnabled = true
                    setDrawGridLines(false)
                    this.textColor = textColor
                    setLabelCount(labels.size)
                    labelRotationAngle = if (rotateLabels) -45f else 0f
                    axisMinimum = -0.5f
                    axisMaximum = labels.size - 0.5f
                }
                axisLeft.textColor = textColor
                axisRight.isEnabled = false
                description.isEnabled = false
                legend.isEnabled = false
                
                if (rotateLabels) {
                    extraBottomOffset = 40f
                }
                
                // Aplicar renderer de bordes redondeados solo si no existe o es diferente
                if (renderer !is RoundedBarChartRenderer) {
                    renderer = RoundedBarChartRenderer(this, animator, viewPortHandler, 16f)
                }
                
                // Solo animar si los datos cambiaron (o si es la primera vez)
                if (chart.data == null) {
                    animateY(800)
                }

                invalidate()
            }
        }

        private fun setupPieChart(chart: PieChart, entries: List<PieEntry>, colors: List<Int>) {
            val dataSet = PieDataSet(entries, "").apply {
                this.colors = colors
                valueTextSize = 12f
                valueTextColor = android.graphics.Color.WHITE
                valueFormatter = PercentFormatter(chart)
            }
            chart.apply {
                this.data = PieData(dataSet)
                setUsePercentValues(true)
                description.isEnabled = false
                holeRadius = 45f
                transparentCircleRadius = 50f
                setHoleColor(android.graphics.Color.TRANSPARENT)
                legend.textColor = getColor(R.color.text_primary)
                legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                
                // Solo animar si es la primera carga
                if (chart.data == null) {
                    animateY(800)
                }

                invalidate()
            }
        }

        private fun setupTable(h1: String, h2: String, rows: List<Pair<String, Int>>) {
            binding.layoutTablaGenerica.visibility = View.VISIBLE
            binding.tvCol1Header.text = h1
            binding.tvCol2Header.text = h2
            
            val table = binding.tableGenerica
            val count = table.childCount
            if (count > 2) table.removeViews(2, count - 2)
            
            if (rows.isEmpty()) {
                binding.tvEstadoVacioTabla.visibility = View.VISIBLE
            } else {
                rows.forEach { (name, value) ->
                    val row = TableRow(itemView.context).apply { setPadding(0, 4, 0, 4) }
                    val tvName = TextView(itemView.context).apply {
                        text = name
                        layoutParams = TableRow.LayoutParams(0, -2, 1f)
                        setPadding(16, 12, 16, 12)
                        setTextColor(getColor(R.color.text_primary))
                    }
                    val tvValue = TextView(itemView.context).apply {
                        text = value.toString()
                        setPadding(16, 12, 16, 12)
                        gravity = android.view.Gravity.END
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(getColor(R.color.text_primary))
                    }
                    row.addView(tvName)
                    row.addView(tvValue)
                    table.addView(row)
                    
                    val divider = View(itemView.context).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                        setBackgroundColor(getColor(R.color.border))
                    }
                    table.addView(divider)
                }
            }
        }

        private fun setupTableDouble(h1: String, h2: String, rows: List<Pair<String, Double>>) {
            binding.layoutTablaGenerica.visibility = View.VISIBLE
            binding.tvCol1Header.text = h1
            binding.tvCol2Header.text = h2
            
            val table = binding.tableGenerica
            val count = table.childCount
            if (count > 2) table.removeViews(2, count - 2)
            
            if (rows.isEmpty()) {
                binding.tvEstadoVacioTabla.text = "Sin datos suficientes"
                binding.tvEstadoVacioTabla.visibility = View.VISIBLE
            } else {
                rows.forEach { (name, value) ->
                    val row = TableRow(itemView.context).apply { setPadding(0, 4, 0, 4) }
                    val tvName = TextView(itemView.context).apply {
                        text = name
                        layoutParams = TableRow.LayoutParams(0, -2, 1f)
                        setPadding(16, 12, 16, 12)
                        setTextColor(getColor(R.color.text_primary))
                    }
                    val tvValue = TextView(itemView.context).apply {
                        text = String.format(Locale.getDefault(), "%.2f Gls/h", value)
                        setPadding(16, 12, 16, 12)
                        gravity = android.view.Gravity.END
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(getColor(R.color.text_primary))
                    }
                    row.addView(tvName)
                    row.addView(tvValue)
                    table.addView(row)
                    
                    val divider = View(itemView.context).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                        setBackgroundColor(getColor(R.color.border))
                    }
                    table.addView(divider)
                }
            }
        }

        private fun setupTableRendimiento(rows: List<com.lingomak.lingomakapp.data.repository.RendimientoOperarioMes>) {
            binding.layoutTablaGenerica.visibility = View.VISIBLE
            binding.tvCol1Header.text = "Operario | Mes"
            binding.tvCol2Header.text = "Horas Totales"
            
            val table = binding.tableGenerica
            val count = table.childCount
            if (count > 2) table.removeViews(2, count - 2)
            
            if (rows.isEmpty()) {
                binding.tvEstadoVacioTabla.text = "Sin datos suficientes"
                binding.tvEstadoVacioTabla.visibility = View.VISIBLE
            } else {
                rows.forEach { item ->
                    val row = TableRow(itemView.context).apply { setPadding(0, 4, 0, 4) }
                    val tvName = TextView(itemView.context).apply {
                        text = "${item.nombreOperario}\n${item.mes}"
                        layoutParams = TableRow.LayoutParams(0, -2, 1f)
                        setPadding(16, 12, 16, 12)
                        setTextColor(getColor(R.color.text_primary))
                    }
                    val tvValue = TextView(itemView.context).apply {
                        text = String.format(Locale.getDefault(), "%.1f h", item.totalHoras)
                        setPadding(16, 12, 16, 12)
                        gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(getColor(R.color.text_primary))
                    }
                    row.addView(tvName)
                    row.addView(tvValue)
                    table.addView(row)
                    
                    val divider = View(itemView.context).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                        setBackgroundColor(getColor(R.color.border))
                    }
                    table.addView(divider)
                }
            }
        }

        private fun getColor(resId: Int): Int = itemView.context.getColor(resId)
    }
}
