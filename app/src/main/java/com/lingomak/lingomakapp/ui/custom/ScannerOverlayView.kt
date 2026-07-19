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

    private val transparentPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val rect = RectF()
    private val frameSize = 250f // dp

    init {
        // Necesario para que PorterDuff.Mode.CLEAR funcione correctamente
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

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

        // 1. Dibujar el fondo oscuro en toda la pantalla
        canvas.drawRect(0f, 0f, width, height, maskPaint)

        // 2. Limpiar el área del recuadro central (hacerlo transparente sin bordes)
        canvas.drawRoundRect(rect, 12f * density, 12f * density, transparentPaint)
    }
}
