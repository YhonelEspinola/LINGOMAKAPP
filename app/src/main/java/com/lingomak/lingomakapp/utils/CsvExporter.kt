package com.lingomak.lingomakapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
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
import com.lingomak.lingomakapp.data.model.BitacoraUsoModel
import com.lingomak.lingomakapp.ui.movimientos.MovimientosEstadisticasViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    private const val BOM = "\uFEFF"

    private fun generateFileName(prefix: String, inicio: Long?, fin: Long?): String {
        if (inicio == null || fin == null || inicio == 0L || fin == Long.MAX_VALUE) {
            return prefix + "_Export.csv"
        }
        val df = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
        val startStr = df.format(Date(inicio))
        val endStr = df.format(Date(fin))
        return prefix + "_" + startStr + "_a_" + endStr + ".csv"
    }

    fun exportInventario(context: Context, data: List<RepuestoModel>, launcher: ActivityResultLauncher<String>? = null) {
        val header = "Código Interno,Nombre,Categoría,Marca,Stock Actual,Stock Mínimo,Stock Máximo,Ubicación,Estado,Proveedor Nombre,Proveedor Contacto,Fecha de Registro"
        val rows = data.map {
            val fecha = it.fechaRegistro?.let { d -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(d) } ?: ""
            escapeCsv(it.codigoInterno) + "," + escapeCsv(it.nombre) + "," + escapeCsv(it.categoria) + "," + escapeCsv(it.marca) + "," + it.stockActual + "," + it.stockMinimo + "," + it.stockMaximo + "," + escapeCsv(it.ubicacionAlmacen) + "," + escapeCsv(it.estado) + "," + escapeCsv(it.proveedorNombre) + "," + escapeCsv(it.proveedorContacto) + "," + fecha
        }
        val csvContent = header + "\n" + rows.joinToString("\n")
        showExportDialog(context, "Inventario_Export.csv", csvContent, launcher)
    }

    fun exportMovimientos(
        context: Context, 
        data: List<Pair<MovimientoModel, String>>, 
        launcher: ActivityResultLauncher<String>? = null,
        inicio: Long? = null,
        fin: Long? = null,
        nombreRepuesto: String? = null
    ) {
        val csvContent = buildMovimientosCsvContent(data)
        val prefix = if (nombreRepuesto != null) "Historial_" + nombreRepuesto.replace(" ", "_") else "Movimientos"
        val fileName = generateFileName(prefix, inicio, fin)
        showExportDialog(context, fileName, csvContent, launcher)
    }

    fun buildMovimientosCsvContent(data: List<Pair<MovimientoModel, String>>): String {
        val header = "Fecha,Tipo,Nombre del Repuesto,Cantidad,Destino,Registrado por,Orden Mantenimiento,Máquina,Observación"
        val rows = data.map { (mov, nombre) ->
            val fecha = mov.fecha?.let { d -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(d) } ?: ""
            val destino = when(mov.destinoSalida) {
                "CONSUMO_INTERNO" -> "Consumo Interno"
                "DISTRIBUCION_EXTERNA" -> "Distribución Externa"
                else -> mov.destinoSalida
            }
            val responsable = if (mov.nombreRegistradoPor.isBlank()) "Sin registrar" else mov.nombreRegistradoPor
            val om = mov.codigoMantenimientoAsociado ?: ""
            val maq = mov.nombreMaquinariaAsociada ?: ""
            
            fecha + "," + escapeCsv(mov.tipo) + "," + escapeCsv(nombre) + "," + mov.cantidad + "," + escapeCsv(destino) + "," + escapeCsv(responsable) + "," + escapeCsv(om) + "," + escapeCsv(maq) + "," + escapeCsv(mov.observacion)
        }
        return header + "\n" + rows.joinToString("\n")
    }

    fun exportMantenimiento(
        context: Context, 
        data: List<MantenimientoModel>, 
        launcher: ActivityResultLauncher<String>? = null,
        inicio: Long? = null,
        fin: Long? = null
    ) {
        val csvContent = buildMantenimientoCsvContent(data)
        val fileName = generateFileName("Mantenimiento", inicio, fin)
        showExportDialog(context, fileName, csvContent, launcher)
    }

    fun buildMantenimientoCsvContent(data: List<MantenimientoModel>): String {
        val header = "Código,Tipo,Máquina,Descripción,Fecha Programada,Fecha Realizada,Estado,Asignado a,Resolutor,Prioridad,Costo Estimado,Costo Real,Horómetro Programado,Horómetro Real"
        val rows = data.map {
            val resolutor = if (it.resolutorNombre.isBlank()) "No registrado" else it.resolutorNombre
            escapeCsv(it.codigoMantenimiento) + "," + escapeCsv(it.tipoMantenimiento) + "," + escapeCsv(it.nombreMaquinaria) + "," + escapeCsv(it.descripcion) + "," + escapeCsv(it.fechaProgramada) + "," + escapeCsv(it.fechaRealizada) + "," + escapeCsv(it.estado) + "," + escapeCsv(it.responsable) + "," + escapeCsv(resolutor) + "," + escapeCsv(it.prioridad) + "," + it.costoEstimado + "," + it.costoReal + "," + it.horometroProgramado + "," + it.horometroReal
        }
        return header + "\n" + rows.joinToString("\n")
    }

    fun exportBitacora(
        context: Context,
        data: List<BitacoraUsoModel>,
        launcher: ActivityResultLauncher<String>? = null,
        inicio: Long? = null,
        fin: Long? = null
    ) {
        val csvContent = buildBitacoraCsvContent(data)
        val fileName = generateFileName("BitacoraUso", inicio, fin)
        showExportDialog(context, fileName, csvContent, launcher)
    }

    fun buildBitacoraCsvContent(data: List<BitacoraUsoModel>): String {
        val header = "Fecha,Máquina,Marca,Modelo,Código,Operario,Tipo Movimiento,Trabajo Realizado,Horómetro Inicial,Horómetro Final,Horas,Obra,Contratista,Ubicación,Repostaje (Gls),Tipo Carga"
        val rows = data.map {
            val repostaje = it.galonesCombustible.toString()
            val tipoCarga = it.tipoCarga ?: "N/A"
            escapeCsv(it.fecha) + "," + escapeCsv(it.nombreMaquinaria) + "," + escapeCsv(it.marcaMaquinaria) + "," + 
            escapeCsv(it.modeloMaquinaria) + "," + escapeCsv(it.codigoMaquinaria) + "," + escapeCsv(it.operarioNombre) + "," + 
            escapeCsv(it.tipoMovimiento) + "," + escapeCsv(it.trabajoRealizado) + "," + it.horometroAnterior + "," + 
            it.horometroFinal + "," + it.horasUso + "," + escapeCsv(it.obra ?: "") + "," + 
            escapeCsv(it.contratista ?: "") + "," + escapeCsv(it.ubicacion ?: "") + "," + repostaje + "," + escapeCsv(tipoCarga)
        }
        return header + "\n" + rows.joinToString("\n")
    }

    fun exportEstadisticas(
        context: Context, 
        data: MovimientosEstadisticasViewModel.EstadisticasData, 
        etiquetaRango: String, 
        launcher: ActivityResultLauncher<String>? = null,
        inicio: Long? = null,
        fin: Long? = null
    ) {
        val csvContent = buildEstadisticasCsvContent(data, etiquetaRango)
        val fileName = generateFileName("Estadisticas", inicio, fin)
        showExportDialog(context, fileName, csvContent, launcher)
    }

    fun buildEstadisticasCsvContent(data: MovimientosEstadisticasViewModel.EstadisticasData, etiquetaRango: String): String {
        val sb = StringBuilder()
        sb.append("1. RESUMEN GENERAL\n")
        sb.append("Rango de fechas,Total Entradas,Total Salidas,Total Movimientos\n")
        sb.append(escapeCsv(etiquetaRango) + "," + data.totalEntradas + "," + data.totalSalidas + "," + data.totalMovimientos + "\n\n")

        sb.append("2. TENDENCIA MENSUAL\n")
        sb.append("Mes,Entradas,Salidas\n")
        data.tendenciaMensual.forEach { (mes, vals) ->
            sb.append(escapeCsv(mes) + "," + vals.first + "," + vals.second + "\n")
        }
        sb.append("\n")

        sb.append("3. TOP REPUESTOS CONSUMIDOS\n")
        sb.append("Nombre,Cantidad de Salidas\n")
        data.topProductos.forEach { (nombre, cant) ->
            sb.append(escapeCsv(nombre) + "," + cant + "\n")
        }
        sb.append("\n")

        sb.append("4. BAJA/NULA ROTACIÓN\n")
        sb.append("Nombre,Cantidad de Salidas\n")
        data.bajaRotacion.forEach { (nombre, cant) ->
            sb.append(escapeCsv(nombre) + "," + cant + "\n")
        }
        sb.append("\n")

        sb.append("5. CONSUMO POR MÁQUINA\n")
        sb.append("Nombre,Cantidad de Repuestos Consumidos\n")
        data.consumoMaquina.forEach { (nombre, cant) ->
            sb.append(escapeCsv(nombre) + "," + cant + "\n")
        }
        sb.append("\n")

        sb.append("6. CONSUMO INTERNO VS EXTERNO\n")
        sb.append("Categoría,Cantidad,Porcentaje\n")
        val totalSalidas = data.totalSalidas.toDouble()
        data.distribucionSalida.forEach { (cat, cant) ->
            val perc = if (totalSalidas > 0) (cant / totalSalidas) * 100 else 0.0
            val catLabel = if(cat == "CONSUMO_INTERNO") "Interno" else "Externo"
            sb.append(escapeCsv(catLabel) + "," + cant + "," + String.format(Locale.getDefault(), "%.1f%%", perc) + "\n")
        }
        sb.append("\n")

        sb.append("7. SALIDAS CON OM VS SUELTAS\n")
        sb.append("Categoría,Cantidad,Porcentaje\n")
        val om = data.omVsSueltas.first
        val sueltas = data.omVsSueltas.second
        val totalOM = (om + sueltas).toDouble()
        val percOM = if (totalOM > 0) (om / totalOM) * 100 else 0.0
        val percSueltas = if (totalOM > 0) (sueltas / totalOM) * 100 else 0.0
        sb.append("Con OM," + om + "," + String.format(Locale.getDefault(), "%.1f%%", percOM) + "\n")
        sb.append("Sueltas," + sueltas + "," + String.format(Locale.getDefault(), "%.1f%%", percSueltas) + "\n\n")

        sb.append("8. COSTO REAL VS ESTIMADO\n")
        sb.append("Categoría,Monto (Soles),Diferencia\n")
        sb.append("Estimado," + data.costosComparativa.first + ",-\n")
        sb.append("Real," + data.costosComparativa.second + "," + data.costosComparativa.third + "\n")

        return sb.toString()
    }

    fun showExportDialog(
        context: Context,
        fileName: String,
        content: String,
        launcher: ActivityResultLauncher<String>? = null
    ) {
        val dialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_share_report, null)
        
        view.findViewById<TextView>(R.id.textViewTitle)?.text = "Exportar Datos"
        
        view.findViewById<View>(R.id.layoutCompartir).setOnClickListener {
            dialog.dismiss()
            shareCsv(context, fileName, content)
        }
        
        view.findViewById<View>(R.id.layoutDescargar).setOnClickListener {
            dialog.dismiss()
            if (launcher != null) {
                launcher.launch(fileName)
            } else {
                downloadCsv(context, fileName, content)
            }
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    fun saveCsvToUri(context: Context, uri: Uri, content: String) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(BOM.toByteArray(Charsets.UTF_8))
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "Archivo guardado correctamente", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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

    fun escapeCsv(value: String): String {
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
