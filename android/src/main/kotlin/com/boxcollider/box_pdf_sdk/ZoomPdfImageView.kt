package com.boxcollider.box_pdf_sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import kotlin.math.min
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

class ZoomPdfImageView(context: Context) : ImageView(context) {
    var drawingEnabled = false
        private set

    private var activePage = 0

    private val pageStrokes =
        mutableMapOf<Int, MutableList<Path>>()

    private var pendingStroke: Path? = null

    private val inverseTransform = Matrix()

    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 0.003f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val transform = Matrix()
    //private val values = FloatArray(9)

    private var minimumScale = 1f
    private var currentScale = 1f

    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScale(
                detector: ScaleGestureDetector
            ): Boolean {
                if (drawable == null) return false

                val targetScale = (
                    currentScale * detector.scaleFactor
                ).coerceIn(
                    minimumScale,
                    minimumScale * 5f
                )

                val factor = targetScale / currentScale

                transform.postScale(
                    factor,
                    factor,
                    detector.focusX,
                    detector.focusY
                )

                currentScale = targetScale

                keepImageInBounds()
                imageMatrix = transform

                return true
            }
        }
    )

    init {
        scaleType = ScaleType.MATRIX
        isClickable = true
    }

    override fun setImageBitmap(bitmap: Bitmap?) {
        super.setImageBitmap(bitmap)
        resetZoom()
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int
    ) {
        super.onSizeChanged(
            width,
            height,
            oldWidth,
            oldHeight
        )

        resetZoom()
    }

    fun resetZoom() {
        val content = drawable ?: return

        if (width <= 0 || height <= 0) return

        val imageWidth = content.intrinsicWidth.toFloat()
        val imageHeight = content.intrinsicHeight.toFloat()

        if (imageWidth <= 0f || imageHeight <= 0f) return

        minimumScale = min(
            width.toFloat() / imageWidth,
            height.toFloat() / imageHeight
        )

        currentScale = minimumScale

        val offsetX = (
            width - imageWidth * minimumScale
        ) / 2f

        val offsetY = (
            height - imageHeight * minimumScale
        ) / 2f

        transform.reset()
        transform.postScale(minimumScale, minimumScale)
        transform.postTranslate(offsetX, offsetY)

        imageMatrix = transform
    }

    private fun keepImageInBounds() {
        val content = drawable ?: return

        val bounds = RectF(
            0f,
            0f,
            content.intrinsicWidth.toFloat(),
            content.intrinsicHeight.toFloat()
        )

        transform.mapRect(bounds)

        val correctionX = when {
            bounds.width() <= width -> {
                (width - bounds.width()) / 2f - bounds.left
            }

            bounds.left > 0f -> -bounds.left
            bounds.right < width -> width - bounds.right
            else -> 0f
        }

        val correctionY = when {
            bounds.height() <= height -> {
                (height - bounds.height()) / 2f - bounds.top
            }

            bounds.top > 0f -> -bounds.top
            bounds.bottom < height -> height - bounds.bottom
            else -> 0f
        }

        transform.postTranslate(correctionX, correctionY)
    }
    fun setDrawingMode(enabled: Boolean) {
        drawingEnabled = enabled
        pendingStroke = null
        dragging = false
        invalidate()
    }

    fun showPage(pageIndex: Int) {
        activePage = pageIndex
        pendingStroke = null
        invalidate()
    }

    fun undoStroke() {
        pendingStroke = null

        pageStrokes[activePage]?.let { strokes ->
            if (strokes.isNotEmpty()) {
                strokes.removeAt(strokes.lastIndex)
            }
        }

        invalidate()
    }

    fun clearPageStrokes() {
        pendingStroke = null
        pageStrokes.remove(activePage)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val content = drawable ?: return

        val imageWidth = content.intrinsicWidth.toFloat()
        val imageHeight = content.intrinsicHeight.toFloat()

        if (imageWidth <= 0f || imageHeight <= 0f) return

        val checkpoint = canvas.save()

        // Misma transformación que utiliza la imagen del PDF.
        canvas.concat(imageMatrix)

        // Los trazos se almacenan en coordenadas normalizadas.
        canvas.scale(imageWidth, imageHeight)
        canvas.clipRect(0f, 0f, 1f, 1f)

        pageStrokes[activePage]?.forEach { stroke ->
            canvas.drawPath(stroke, inkPaint)
        }

        pendingStroke?.let { stroke ->
            canvas.drawPath(stroke, inkPaint)
        }

        canvas.restoreToCount(checkpoint)
    }

    private fun normalizedPoint(
        event: MotionEvent
    ): FloatArray? {
        val content = drawable ?: return null

        if (
            content.intrinsicWidth <= 0 ||
            content.intrinsicHeight <= 0
        ) {
            return null
        }

        if (!imageMatrix.invert(inverseTransform)) {
            return null
        }

        val point = floatArrayOf(event.x, event.y)

        inverseTransform.mapPoints(point)

        point[0] /= content.intrinsicWidth.toFloat()
        point[1] /= content.intrinsicHeight.toFloat()

        return point
    }

    private fun handleDrawing(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val point = normalizedPoint(event) ?: return false

                // No empezar un trazo fuera de la página.
                if (
                    point[0] !in 0f..1f ||
                    point[1] !in 0f..1f
                ) {
                    return false
                }

                parent?.requestDisallowInterceptTouchEvent(true)

                pendingStroke = Path().apply {
                    moveTo(point[0], point[1])
                }

                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (pendingStroke != null && event.pointerCount == 1) {
                    val point = normalizedPoint(event) ?: return true

                    pendingStroke?.lineTo(point[0], point[1])
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                pendingStroke?.let { stroke ->
                    normalizedPoint(event)?.let { point ->
                        stroke.lineTo(point[0], point[1])
                    }

                    pageStrokes.getOrPut(activePage) {
                        mutableListOf()
                    }.add(stroke)
                }

                pendingStroke = null

                parent?.requestDisallowInterceptTouchEvent(false)

                invalidate()
                performClick()
            }

            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_CANCEL -> {
                // Cancelar el trazo si entra otro dedo.
                pendingStroke = null

                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
            }
        }

        return true
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (drawable == null) return false

        if (drawingEnabled) {
            return handleDrawing(event)
        }

        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)

                lastX = event.x
                lastY = event.y
                dragging = true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                dragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (
                    event.pointerCount == 1 &&
                    dragging &&
                    !scaleDetector.isInProgress
                ) {
                    val deltaX = event.x - lastX
                    val deltaY = event.y - lastY

                    transform.postTranslate(deltaX, deltaY)

                    keepImageInBounds()
                    imageMatrix = transform

                    lastX = event.x
                    lastY = event.y
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Evita saltos al retirar uno de los dedos.
                val remainingIndex =
                    if (event.actionIndex == 0) 1 else 0

                if (remainingIndex < event.pointerCount) {
                    lastX = event.getX(remainingIndex)
                    lastY = event.getY(remainingIndex)
                    dragging = true
                }
            }

            MotionEvent.ACTION_UP -> {
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
            }

            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}