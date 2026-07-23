package com.lingomak.lingomakapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.lingomak.lingomakapp.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.model.MovimientoModel
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.ui.movimientos.MovimientosEstadisticasViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    private const val BOM = "\uFEFF"

    fun exportInventario(context: Context, data: List<RepuestoModel>) {
        val header = "Código Interno,Nombre,Categoría,Marca,Stock Actual,Stock Mínimo,Stock Máximo,Ubicación,Estado,Proveedor Nombre,Proveedor Contacto,Fecha de Registro"
        val rows = data.map {
            val fecha = it.fechaRegistro?.let { d -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(d) } ?: ""
            "${escapeCsv(it.codigoInterno)},${escapeCsv(it.nombre)},${escapeCsv(it.categoria)},${escapeCsv(it.marca)},${it.stockActual},${it.stockMinimo},${it.stockMaximo},${escapeCsv(it.ubicacionAlmacen)},${escapeCsv(it.estado)},${escapeCsv(it.proveedorNombre)},${escapeCsv(it.proveedorContacto)},$fecha"
        }
        val csvContent = header + "\n" + rows.joinToString("\n")
        showExportDialog(context, "Inventario_Export.csv", csvContent)
    }

    fun exportMovimientos(context: Context, data: List<Pair<MovimientoModel, String>>) {
        val header = "Fecha,Tipo,Nombre del Repuesto,Cantidad,Destino,Registrado por,Orden Mantenimiento,Máquina,Observación"
        val rows = data.map { (mov, nombre) ->
            val fecha = mov.fecha?.let { d -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(d) } ?: ""
            val destino = when(mov.destinoSalida) {
                "CONSUMO_INTERNO" -> "Consumo Interno"
                "DISTRIBUCION_EXTERNA" -> "Distribución Externa"
                else -> mov.destinoSalida
            }
            "${fecha},${escapeCsv(mov.tipo)},${escapeCsv(nombre)},${mov.cantidad},${escapeCsv(destino)},${escapeCsv(mov.registradoPor)},${escapeCsv(mov.ordenMantenimientoUid ?: "")},${escapeCsv(mov.maquinariaUid ?: "")},${escapeCsv(mov.observacion)}"
        }
        val csvContent = header + "\n" + rows.joinToString("\n")
        showExportDialog(context, "Movimientos_Export.csv", csvContent)
    }

    fun exportMantenimiento(context: Context, data: List<MantenimientoModel>) {
        val header = "Código,Tipo,Máquina,Descripción,Fecha Programada,Fecha Realizada,Estado,Responsable,Prioridad,Costo Estimado,Costo Real,Horómetro Programado,Horómetro Real"
        val rows = data.map {
            "${escapeCsv(it.codigoMantenimiento)},${escapeCsv(it.tipoMantenimiento)},${escapeCsv(it.nombreMaquinaria)},${escapeCsv(it.descripcion)},${escapeCsv(it.fechaProgramada)},${escapeCsv(it.fechaRealizada)},${escapeCsv(it.estado)},${escapeCsv(it.responsable)},${escapeCsv(it.prioridad)},${it.costoEstimado},${it.costoReal},${it.horometroProgramado},${it.horometroReal}"
        }
        val csvContent = header + "\n" + rows.joinToString("\n")
        showExportDialog(context, "Mantenimiento_Export.csv", csvContent)
    }

    fun exportEstadisticas(context: Context, data: MovimientosEstadisticasViewModel.EstadisticasData, etiquetaRango: String) {
        val sb = StringBuilder()
        
        // 1. Resumen General
        sb.append("1. RESUMEN GENERAL\n")
        sb.append("Rango de fechas,Total Entradas,Total Salidas,Total Movimientos\n")
        sb.append("${escapeCsv(etiquetaRango)},${data.totalEntradas},${data.totalSalidas},${data.totalMovimientos}\n\n")

        // 2. Tendencia Mensual
        sb.append("2. TENDENCIA MENSUAL\n")
        sb.append("Mes,Entradas,Salidas\n")
        data.tendenciaMensual.forEach { (mes, vals) ->
            sb.append("${escapeCsv(mes)},${vals.first},${vals.second}\n")
        }
        sb.append("\n")

        // 3. Top Repuestos Consumidos
        sb.append("3. TOP REPUESTOS CONSUMIDOS\n")
        sb.append("Nombre,Cantidad de Salidas\n")
        data.topProductos.forEach { (nombre, cant) ->
            sb.append("${escapeCsv(nombre)},$cant\n")
        }
        sb.append("\n")

        // 4. Baja/Nula Rotación
        sb.append("4. BAJA/NULA ROTACIÓN\n")
        sb.append("Nombre,Cantidad de Salidas\n")
        data.bajaRotacion.forEach { (nombre, cant) ->
            sb.append("${escapeCsv(nombre)},$cant\n")
        }
        sb.append("\n")

        // 5. Consumo por Máquina
        sb.append("5. CONSUMO POR MÁQUINA\n")
        sb.append("Nombre,Cantidad de Repuestos Consumidos\n")
        data.consumoMaquina.forEach { (nombre, cant) ->
            sb.append("${escapeCsv(nombre)},$cant\n")
        }
        sb.append("\n")

        // 6. Consumo Interno vs Externo
        sb.append("6. CONSUMO INTERNO VS EXTERNO\n")
        sb.append("Categoría,Cantidad,Porcentaje\n")
        val totalSalidas = data.totalSalidas.toDouble()
        data.distribucionSalida.forEach { (cat, cant) ->
            val perc = if (totalSalidas > 0) (cant / totalSalidas) * 100 else 0.0
            val catLabel = if(cat == "CONSUMO_INTERNO") "Interno" else "Externo"
            sb.append("${escapeCsv(catLabel)},$cant,${String.format(Locale.getDefault(), "%.1f%%", perc)}\n")
        }
        sb.append("\n")

        // 7. Salidas con OM vs Sueltas
        sb.append("7. SALIDAS CON OM VS SUELTAS\n")
        sb.append("Categoría,Cantidad,Porcentaje\n")
        val om = data.omVsSueltas.first
        val sueltas = data.omVsSueltas.second
        val totalOM = (om + sueltas).toDouble()
        val percOM = if (totalOM > 0) (om / totalOM) * 100 else 0.0
        val percSueltas = if (totalOM > 0) (sueltas / totalOM) * 100 else 0.0
        sb.append("Con OM,$om,${String.format(Locale.getDefault(), "%.1f%%", percOM)}\n")
        sb.append("Sueltas,$sueltas,${String.format(Locale.getDefault(), "%.1f%%", percSueltas)}\n\n")

        // 8. Costo Real vs Estimado
        sb.append("8. COSTO REAL VS ESTIMADO\n")
        sb.append("Categoría,Monto (Soles),Diferencia\n")
        sb.append("Estimado,${data.costosComparativa.first},-\n")
        sb.append("Real,${data.costosComparativa.second},${data.costosComparativa.third}\n")

        showExportDialog(context, "Estadisticas_Export.csv", sb.toString())
    }

    fun showExportDialog(context: Context, fileName: String, content: String) {
        val dialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_share_report, null)
        
        view.findViewById<TextView>(R.id.textViewTitle)?.text = "Exportar Datos"
        
        view.findViewById<View>(R.id.layoutCompartir).setOnClickListener {
            dialog.dismiss()
            shareCsv(context, fileName, content)
        }
        
        view.findViewById<View>(R.id.layoutDescargar).setOnClickListener {
            dialog.dismiss()
            downloadCsv(context, fileName, content)
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun downloadCsv(context: Context, fileName: String, content: String) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(BOM.toByteArray(Charsets.UTF_8))
                    outputStream.write(content.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Archivo guardado en Descargas", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al descargar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private fun shareCsv(context: Context, fileName: String, content: String) {
        try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use {
                it.write(BOM.toByteArray(Charsets.UTF_8))
                it.write(content.toByteArray(Charsets.UTF_8))
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Exportar CSV"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
