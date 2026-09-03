package com.lingomak.lingomakapp.utils

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.HorizontalScrollView
import kotlin.math.abs

/**
 * Un HorizontalScrollView personalizado que intercepta el toque solo cuando el movimiento
 * es predominantemente horizontal y supera el touch slop.
 * Esto evita que componentes hijos como los Material Chips "secuestren" el toque
 * agresivamente, permitiendo el scroll horizontal fluido.
 */
class SwipeableHorizontalScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private var xDown = 0f
    private var yDown = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                xDown = ev.x
                yDown = ev.y
                // Permitir que el hijo reciba el ACTION_DOWN inicialmente
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val xDiff = abs(ev.x - xDown)
                val yDiff = abs(ev.y - yDown)

                if (xDiff > touchSlop && xDiff > yDiff) {
                    // El movimiento es predominantemente horizontal y superó el umbral.
                    // Interceptamos para que el scrollview tome el control.
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}
