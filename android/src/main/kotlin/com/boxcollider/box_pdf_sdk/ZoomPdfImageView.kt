package com.boxcollider.box_pdf_sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import kotlin.math.min

class ZoomPdfImageView(context: Context) : ImageView(context) {

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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (drawable == null) return false

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