package com.lingomak.lingomakapp.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.data.model.ReporteIAData
import java.io.OutputStream

class PdfGenerator(private val context: Context) {

    fun generateMaintenanceReport(
        outputStream: OutputStream,
        mantenimiento: MantenimientoModel,
        reporteIA: ReporteIAData,
        imagenes: List<Bitmap>
    ) {
        val pdfDocument = PdfDocument()
        
        // Configuración de página A4 (aprox 595 x 842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()
        var y = 40f
        val margin = 40f
        val contentWidth = 595 - (margin * 2)

        // 1. ENCABEZADO
        paint.color = Color.parseColor("#8B0000") // Primary Dark Red
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("LINGOMAK - REPORTE TÉCNICO", margin, y, paint)
        
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.GRAY
        y += 20f
        canvas.drawText("Código: ${mantenimiento.codigoMantenimiento}", margin, y, paint)
        canvas.drawText("Fecha: ${mantenimiento.fechaRealizada}", 400f, y, paint)

        y += 30f
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawLine(margin, y, 555f, y, paint)

        // 2. DATOS GENERALES
        y += 30f
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("INFORMACIÓN DE LA MAQUINARIA", margin, y, paint)
        
        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 10f
        canvas.drawText("Equipo: ${mantenimiento.nombreMaquinaria}", margin, y, paint)
        canvas.drawText("Modelo/Código: ${mantenimiento.codigoMaquinaria}", 300f, y, paint)
        
        y += 15f
        canvas.drawText("Tipo de Mantenimiento: ${mantenimiento.tipoMantenimiento}", margin, y, paint)
        canvas.drawText("Horómetro: ${mantenimiento.horometroReal} h", 300f, y, paint)
        
        y += 15f
        canvas.drawText("Responsable: ${mantenimiento.responsable}", margin, y, paint)
        canvas.drawText("Costo Real: S/ ${mantenimiento.costoReal}", 300f, y, paint)

        // 3. NARRATIVA IA (Secciones)
        y += 40f
        drawSection(canvas, "1. SÍNTOMA / PROBLEMA REPORTADO", reporteIA.sintoma, margin, y, paint)
        y += calculateTextHeight(reporteIA.sintoma, 10f, contentWidth) + 30f

        drawSection(canvas, "2. CAUSA PROBABLE", reporteIA.causa, margin, y, paint)
        y += calculateTextHeight(reporteIA.causa, 10f, contentWidth) + 30f

        drawSection(canvas, "3. ACCIONES REALIZADAS", reporteIA.acciones, margin, y, paint)
        y += calculateTextHeight(reporteIA.acciones, 10f, contentWidth) + 30f

        // Si llegamos cerca del final, abrimos nueva página
        if (y > 700f) {
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 40f
        }

        drawSection(canvas, "4. RESULTADO FINAL", reporteIA.resultado, margin, y, paint)
        y += calculateTextHeight(reporteIA.resultado, 10f, contentWidth) + 40f

        // 4. EVIDENCIAS FOTOGRÁFICAS
        if (imagenes.isNotEmpty()) {
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("EVIDENCIAS FOTOGRÁFICAS", margin, y, paint)
            y += 20f
            
            var xPos = margin
            val imgSize = 150f
            
            for (bitmap in imagenes) {
                if (xPos + imgSize > 555f) {
                    xPos = margin
                    y += imgSize + 10f
                }
                
                // Si la imagen se sale de la página, crear nueva
                if (y + imgSize > 800f) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f
                    xPos = margin
                }

                val rect = RectF(xPos, y, xPos + imgSize, y + imgSize)
                canvas.drawBitmap(bitmap, null, rect, null)
                xPos += imgSize + 10f
            }
        }

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    private fun drawSection(canvas: Canvas, title: String, content: String, x: Float, y: Float, paint: Paint) {
        paint.textSize = 11f
        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#8B0000")
        canvas.drawText(title, x, y, paint)
        
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.BLACK
        
        val textPaint = TextPaint()
        textPaint.textSize = 10f
        textPaint.color = Color.BLACK
        
        val staticLayout = android.text.StaticLayout.Builder.obtain(
            content, 0, content.length, textPaint, (595 - 80).toInt()
        ).build()
        
        canvas.save()
        canvas.translate(x, y + 10f)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun calculateTextHeight(text: String, textSize: Float, width: Float): Float {
        val textPaint = TextPaint()
        textPaint.textSize = textSize
        val staticLayout = android.text.StaticLayout.Builder.obtain(
            text, 0, text.length, textPaint, width.toInt()
        ).build()
        return staticLayout.height.toFloat()
    }
}

// Helper class for text layout
private class TextPaint : Paint()
