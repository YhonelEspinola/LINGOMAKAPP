package com.lingomak.lingomakapp.ui.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ScannerOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maskPaint = Paint().apply {
        color = Color.BLACK
        alpha = 150
        style = Paint.Style.FILL
    }

    private val framePaint = Paint().apply {
        color = Color.parseColor("#E67E22") // color primary
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val rect = RectF()
    private val frameSize = 250f // dp

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val density = resources.displayMetrics.density
        val sizePx = frameSize * density
        
        val left = (width - sizePx) / 2
        val top = (height - sizePx) / 2
        val right = left + sizePx
        val bottom = top + sizePx
        
        rect.set(left, top, right, bottom)

        // Dibujar máscara oscura
        canvas.drawRect(0f, 0f, width, top, maskPaint) // arriba
        canvas.drawRect(0f, bottom, width, height, maskPaint) // abajo
        canvas.drawRect(0f, top, left, bottom, maskPaint) // izquierda
        canvas.drawRect(right, top, width, bottom, maskPaint) // derecha

        // Dibujar recuadro claro
        canvas.drawRoundRect(rect, 8f * density, 8f * density, framePaint)
    }
}
