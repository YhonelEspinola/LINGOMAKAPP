package com.lingomak.lingomakapp.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.model.ReporteIAData
import java.io.OutputStream

class PdfGenerator(private val context: Context) {

    private var currentPage: PdfDocument.Page? = null
    private var currentCanvas: Canvas? = null

    fun generateMaintenanceReport(
        outputStream: OutputStream,
        mantenimiento: MantenimientoModel,
        reporteIA: ReporteIAData,
        fotosReporte: List<Bitmap>,
        fotosFinalizacion: List<Bitmap>
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        
        startNewPage(pdfDocument, pageInfo)
        val paint = Paint()
        var y = 40f
        val margin = 40f
        val contentWidth = 515

        // 1. ENCABEZADO
        paint.color = Color.parseColor("#8B0000")
        paint.textSize = 18f
        paint.isFakeBoldText = true
        currentCanvas?.drawText("LINGOMAK - REPORTE TÉCNICO", margin, y, paint)
        
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.GRAY
        y += 20f
        currentCanvas?.drawText("Código: ${mantenimiento.codigoMantenimiento}", margin, y, paint)
        currentCanvas?.drawText("Fecha: ${mantenimiento.fechaRealizada}", 400f, y, paint)

        y += 30f
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        currentCanvas?.drawLine(margin, y, 555f, y, paint)

        // 2. DATOS GENERALES
        y += 30f
        paint.textSize = 12f
        paint.isFakeBoldText = true
        currentCanvas?.drawText("INFORMACIÓN DE LA MAQUINARIA", margin, y, paint)
        
        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 10f
        currentCanvas?.drawText("Equipo: ${mantenimiento.nombreMaquinaria}", margin, y, paint)
        currentCanvas?.drawText("Modelo/Código: ${mantenimiento.codigoMaquinaria}", 300f, y, paint)
        
        y += 15f
        currentCanvas?.drawText("Tipo de Mantenimiento: ${mantenimiento.tipoMantenimiento}", margin, y, paint)
        currentCanvas?.drawText("Horómetro: ${mantenimiento.horometroReal} h", 300f, y, paint)
        
        y += 15f
        currentCanvas?.drawText("Asignado a: ${mantenimiento.responsable}", margin, y, paint)
        currentCanvas?.drawText("Costo Real: S/ ${mantenimiento.costoReal}", 300f, y, paint)
        
        y += 15f
        val resolutor = if (mantenimiento.resolutorNombre.isBlank()) "No registrado" else mantenimiento.resolutorNombre
        currentCanvas?.drawText("Resolutor: $resolutor", margin, y, paint)

        // 3. NARRATIVA IA
        y += 40f
        y = drawSection(pdfDocument, pageInfo, "1. SÍNTOMA / PROBLEMA REPORTADO", reporteIA.sintoma, margin, y, paint, contentWidth)
        y = drawSection(pdfDocument, pageInfo, "2. CAUSA PROBABLE", reporteIA.causa, margin, y, paint, contentWidth)
        y = drawSection(pdfDocument, pageInfo, "3. ACCIONES REALIZADAS", reporteIA.acciones, margin, y, paint, contentWidth)
        y = drawSection(pdfDocument, pageInfo, "4. RESULTADO FINAL", reporteIA.resultado, margin, y, paint, contentWidth)

        // 4. EVIDENCIAS
        y += 20f
        if (fotosReporte.isNotEmpty()) {
            y = drawImageGrid(pdfDocument, pageInfo, "EVIDENCIAS DE REPORTE (INICIAL)", fotosReporte, margin, y, paint)
            y += 20f
        }

        if (fotosFinalizacion.isNotEmpty()) {
            y = drawImageGrid(pdfDocument, pageInfo, "EVIDENCIAS DE FINALIZACIÓN", fotosFinalizacion, margin, y, paint)
        }

        pdfDocument.finishPage(currentPage)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    private fun startNewPage(pdfDocument: PdfDocument, pageInfo: PdfDocument.PageInfo) {
        if (currentPage != null) pdfDocument.finishPage(currentPage)
        currentPage = pdfDocument.startPage(pageInfo)
        currentCanvas = currentPage?.canvas
    }

    private fun drawSection(
        pdfDocument: PdfDocument,
        pageInfo: PdfDocument.PageInfo,
        title: String,
        content: String,
        x: Float,
        y: Float,
        paint: Paint,
        width: Int
    ): Float {
        var currentY = y
        val textPaint = TextPaint()
        textPaint.textSize = 10f
        val staticLayout = StaticLayout.Builder.obtain(content, 0, content.length, textPaint, width).build()
        
        if (currentY + staticLayout.height + 40f > 800f) {
            startNewPage(pdfDocument, pageInfo)
            currentY = 40f
        }

        paint.textSize = 11f
        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#8B0000")
        currentCanvas?.drawText(title, x, currentY, paint)
        
        currentCanvas?.save()
        currentCanvas?.translate(x, currentY + 10f)
        staticLayout.draw(currentCanvas)
        currentCanvas?.restore()
        
        return currentY + staticLayout.height + 30f
    }

    private fun drawImageGrid(
        pdfDocument: PdfDocument,
        pageInfo: PdfDocument.PageInfo,
        title: String,
        bitmaps: List<Bitmap>,
        margin: Float,
        y: Float,
        paint: Paint
    ): Float {
        var currentY = y
        if (currentY + 180f > 800f) {
            startNewPage(pdfDocument, pageInfo)
            currentY = 40f
        }

        paint.textSize = 11f
        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#8B0000")
        currentCanvas?.drawText(title, margin, currentY, paint)
        currentY += 20f

        var xPos = margin
        val imgSize = 150f
        
        for (bitmap in bitmaps) {
            if (xPos + imgSize > 555f) {
                xPos = margin
                currentY += imgSize + 10f
            }
            if (currentY + imgSize > 800f) {
                startNewPage(pdfDocument, pageInfo)
                currentY = 40f
                xPos = margin
            }
            val rect = RectF(xPos, currentY, xPos + imgSize, currentY + imgSize)
            currentCanvas?.drawBitmap(bitmap, null, rect, null)
            xPos += imgSize + 10f
        }
        return currentY + imgSize + 10f
    }
}
