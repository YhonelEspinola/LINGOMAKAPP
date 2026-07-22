package com.lingomak.lingomakapp.ui.movimientos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.ItemEstadisticaPageBinding

class EstadisticasPagerAdapter : RecyclerView.Adapter<EstadisticasPagerAdapter.ViewHolder>() {

    private var data: MovimientosEstadisticasViewModel.EstadisticasData? = null

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
        holder.bind(position, currentData)
    }

    override fun getItemCount(): Int = 8

    class ViewHolder(val binding: ItemEstadisticaPageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(position: Int, data: MovimientosEstadisticasViewModel.EstadisticasData) {
            resetVisibility()
            
            when (position) {
                0 -> setupP1(data)
                1 -> setupP2(data)
                2 -> setupP3(data)
                3 -> setupP4(data)
                4 -> setupP5(data)
                5 -> setupP6(data)
                6 -> setupP7(data)
                7 -> setupP8(data)
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
            binding.chartBarGenerico.visibility = View.VISIBLE
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
            ), listOf(getColor(R.color.success), getColor(R.color.danger)))
        }

        private fun setupP2(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Tendencia Mensual"
            binding.layoutP2Tendencia.visibility = View.VISIBLE
            
            val entriesEntradas = data.tendenciaMensual.mapIndexed { i, pair -> Entry(i.toFloat(), pair.second.first.toFloat()) }
            val entriesSalidas = data.tendenciaMensual.mapIndexed { i, pair -> Entry(i.toFloat(), pair.second.second.toFloat()) }
            
            val setEnt = LineDataSet(entriesEntradas, "Entradas").apply {
                color = getColor(R.color.success)
                setCircleColor(getColor(R.color.success))
                lineWidth = 2f
            }
            val setSal = LineDataSet(entriesSalidas, "Salidas").apply {
                color = getColor(R.color.danger)
                setCircleColor(getColor(R.color.danger))
                lineWidth = 2f
            }
            
            binding.chartP2.apply {
                this.data = LineData(setEnt, setSal)
                xAxis.valueFormatter = IndexAxisValueFormatter(data.tendenciaMensual.map { it.first })
                xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                axisRight.isEnabled = false
                description.isEnabled = false
                animateX(1000)
                invalidate()
            }
        }

        private fun setupP3(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Top Repuestos Consumidos"
            setupTable("Repuesto", "Salidas", data.topProductos)
        }

        private fun setupP4(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Baja/Nula Rotación"
            setupTable("Repuesto", "Salidas", data.bajaRotacion)
        }

        private fun setupP5(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Consumo por Máquina"
            if (data.consumoMaquina.isEmpty()) {
                binding.layoutChartGenerico.visibility = View.VISIBLE
                binding.tvEstadoVacioChart.visibility = View.VISIBLE
                binding.chartBarGenerico.visibility = View.GONE
            } else {
                binding.layoutChartGenerico.visibility = View.VISIBLE
                setupBarChart(binding.chartBarGenerico, data.consumoMaquina.map { it.first }, 
                    data.consumoMaquina.mapIndexed { i, p -> BarEntry(i.toFloat(), p.second.toFloat()) },
                    listOf(getColor(R.color.primary)))
            }
        }

        private fun setupP6(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Interno vs Externo"
            binding.layoutChartGenerico.visibility = View.VISIBLE
            binding.chartBarGenerico.visibility = View.GONE
            binding.chartPieGenerico.visibility = View.VISIBLE
            
            val entries = data.distribucionSalida.map { PieEntry(it.value.toFloat(), it.key) }
            if (entries.isEmpty()) {
                binding.tvEstadoVacioChart.visibility = View.VISIBLE
                binding.chartPieGenerico.visibility = View.GONE
            } else {
                setupPieChart(binding.chartPieGenerico, entries)
            }
        }

        private fun setupP7(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Salidas con OM vs Sueltas"
            binding.layoutChartGenerico.visibility = View.VISIBLE
            binding.chartBarGenerico.visibility = View.GONE
            binding.chartPieGenerico.visibility = View.VISIBLE
            
            val entries = mutableListOf<PieEntry>()
            if (data.omVsSueltas.first > 0) entries.add(PieEntry(data.omVsSueltas.first.toFloat(), "Con OM"))
            if (data.omVsSueltas.second > 0) entries.add(PieEntry(data.omVsSueltas.second.toFloat(), "Sin OM"))
            
            if (entries.isEmpty()) {
                binding.tvEstadoVacioChart.visibility = View.VISIBLE
                binding.chartPieGenerico.visibility = View.GONE
            } else {
                setupPieChart(binding.chartPieGenerico, entries)
            }
        }

        private fun setupP8(data: MovimientosEstadisticasViewModel.EstadisticasData) {
            binding.tvTituloPagina.text = "Costo Real vs Estimado"
            binding.layoutP8Costos.visibility = View.VISIBLE
            binding.tvP8Diferencia.text = data.costosComparativa.third
            
            setupBarChart(binding.chartP8, listOf("Estimado", "Real"), listOf(
                BarEntry(0f, data.costosComparativa.first.toFloat()),
                BarEntry(1f, data.costosComparativa.second.toFloat())
            ), listOf(getColor(R.color.secondary), getColor(R.color.primary)))
        }

        private fun setupBarChart(chart: BarChart, labels: List<String>, entries: List<BarEntry>, colors: List<Int>) {
            val dataSet = BarDataSet(entries, "").apply {
                this.colors = colors
                valueTextSize = 10f
            }
            chart.apply {
                this.data = BarData(dataSet)
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                axisRight.isEnabled = false
                description.isEnabled = false
                legend.isEnabled = false
                animateY(800)
                invalidate()
            }
        }

        private fun setupPieChart(chart: PieChart, entries: List<PieEntry>) {
            val dataSet = PieDataSet(entries, "").apply {
                colors = listOf(getColor(R.color.primary), getColor(R.color.secondary), getColor(R.color.accent))
                valueTextSize = 12f
                valueFormatter = PercentFormatter(chart)
            }
            chart.apply {
                this.data = PieData(dataSet)
                setUsePercentValues(true)
                description.isEnabled = false
                legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                animateY(800)
                invalidate()
            }
        }

        private fun setupTable(h1: String, h2: String, rows: List<Pair<String, Int>>) {
            binding.layoutTablaGenerica.visibility = View.VISIBLE
            binding.tvCol1Header.text = h1
            binding.tvCol2Header.text = h2
            
            val table = binding.tableGenerica
            val count = table.childCount
            if (count > 1) table.removeViews(1, count - 1)
            
            if (rows.isEmpty()) {
                binding.tvEstadoVacioTabla.visibility = View.VISIBLE
            } else {
                rows.forEach { (name, value) ->
                    val row = TableRow(itemView.context).apply { setPadding(0, 8, 0, 8) }
                    val tvName = TextView(itemView.context).apply {
                        text = name
                        layoutParams = TableRow.LayoutParams(0, -2, 1f)
                        setPadding(8, 0, 8, 0)
                    }
                    val tvValue = TextView(itemView.context).apply {
                        text = value.toString()
                        setPadding(8, 0, 8, 0)
                        gravity = android.view.Gravity.END
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    row.addView(tvName)
                    row.addView(tvValue)
                    table.addView(row)
                }
            }
        }

        private fun getColor(resId: Int): Int = itemView.context.getColor(resId)
    }
}
