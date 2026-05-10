package com.pocketlinux.de.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.pocketlinux.de.vnc.InputEncoder
import com.pocketlinux.de.vnc.RfbProtocol

class VncCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()

    // Scale and pan state
    private var scale = 1f
    private var panX = 0f
    private var panY = 0f

    // Track pinch mid-point to anchor zoom
    private var focalX = 0f
    private var focalY = 0f

    var framebuffer: Bitmap? = null
        set(value) {
            field = value
            if (value != null && scale == 1f) fitToScreen(value)
            invalidate()
        }

    var inputEncoder: InputEncoder? = null

    // --- Gesture detectors ---

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                focalX = detector.focusX
                focalY = detector.focusY
                return true
            }
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                val newScale = (scale * factor).coerceIn(0.25f, 4f)
                // Adjust pan to keep focal point stationary
                panX = focalX - (focalX - panX) * (newScale / scale)
                panY = focalY - (focalY - panY) * (newScale / scale)
                scale = newScale
                invalidate()
                return true
            }
        })

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent,
                distanceX: Float, distanceY: Float
            ): Boolean {
                panX -= distanceX
                panY -= distanceY
                clampPan()
                invalidate()
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                sendPointerClick(e.x, e.y, RfbProtocol.BTN_LEFT)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                sendPointerClick(e.x, e.y, RfbProtocol.BTN_RIGHT)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                sendPointerClick(e.x, e.y, RfbProtocol.BTN_MIDDLE)
                return true
            }
        })

    // --- Drawing ---

    private fun fitToScreen(bitmap: Bitmap) {
        if (width == 0 || height == 0) return
        val scaleX = width.toFloat() / bitmap.width
        val scaleY = height.toFloat() / bitmap.height
        scale = minOf(scaleX, scaleY)
        panX = (width - bitmap.width * scale) / 2f
        panY = (height - bitmap.height * scale) / 2f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        framebuffer?.let { fitToScreen(it) }
    }

    override fun onDraw(canvas: Canvas) {
        val bitmap = framebuffer ?: return
        matrix.reset()
        matrix.setScale(scale, scale)
        matrix.postTranslate(panX, panY)
        canvas.drawBitmap(bitmap, matrix, paint)
    }

    // --- Touch input ---

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun viewToRemote(viewX: Float, viewY: Float): Pair<Int, Int> {
        val fb = framebuffer ?: return Pair(0, 0)
        val rx = ((viewX - panX) / scale).toInt().coerceIn(0, fb.width - 1)
        val ry = ((viewY - panY) / scale).toInt().coerceIn(0, fb.height - 1)
        return Pair(rx, ry)
    }

    private fun sendPointerClick(viewX: Float, viewY: Float, button: Int) {
        val enc = inputEncoder ?: return
        val (rx, ry) = viewToRemote(viewX, viewY)
        enc.sendPointerEvent(button, rx, ry)
        enc.sendPointerEvent(0, rx, ry)
    }

    private fun clampPan() {
        val fb = framebuffer ?: return
        val scaledW = fb.width * scale
        val scaledH = fb.height * scale
        panX = panX.coerceIn(minOf(0f, width - scaledW), maxOf(0f, width - scaledW))
        panY = panY.coerceIn(minOf(0f, height - scaledH), maxOf(0f, height - scaledH))
    }

    // --- Keyboard passthrough ---

    override fun onCheckIsTextEditor() = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = android.text.InputType.TYPE_NULL
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
        return object : BaseInputConnection(this, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                text?.forEach { ch -> inputEncoder?.sendCharacter(ch) }
                return true
            }
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                repeat(beforeLength) { inputEncoder?.sendKeyPress(RfbProtocol.KEY_BACKSPACE) }
                return true
            }
        }
    }

    fun sendKeyEvent(down: Boolean, keySym: Int) {
        inputEncoder?.sendKeyEvent(down, keySym)
    }

    fun sendCharacter(ch: Char) {
        inputEncoder?.sendCharacter(ch)
    }
}
