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
import java.io.File
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

class ZoomPdfImageView(context: Context) : ImageView(context) {
    var drawingEnabled = false
        private set

    private var activePage = 0

    private data class InkStroke(
        val path: Path,
        val paint: Paint,
        val points: MutableList<FloatArray>
    )

    private val pageStrokes =
        mutableMapOf<Int, MutableList<InkStroke>>()

    private var pendingStroke: InkStroke? = null

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
    fun setInkColor(color: Int) {
        inkPaint.color = color
    }

    fun setInkWidth(width: Float) {
        inkPaint.strokeWidth = width.coerceIn(0.001f, 0.02f)
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
            canvas.drawPath(stroke.path, stroke.paint)
        }

        pendingStroke?.let { stroke ->
            canvas.drawPath(stroke.path, stroke.paint)
        }

        canvas.restoreToCount(checkpoint)
    }

    private fun annotationFile(pdfBytes: ByteArray): File {
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(pdfBytes)

        val documentId = digest.joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }

        val directory = File(
            context.filesDir,
            "pdf_annotations"
        )

        check(directory.exists() || directory.mkdirs()) {
            "No se pudo crear la carpeta de anotaciones."
        }

        return File(directory, "$documentId.json")
    }

    fun saveAnnotations(pdfBytes: ByteArray): Int {
        val pagesJson = JSONArray()
        var strokeCount = 0

        pageStrokes.toSortedMap().forEach { (pageIndex, strokes) ->
            val strokesJson = JSONArray()

            strokes.forEach { stroke ->
                val pointsJson = JSONArray()

                stroke.points.forEach { point ->
                    pointsJson.put(
                        JSONArray()
                            .put(point[0].toDouble())
                            .put(point[1].toDouble())
                    )
                }

                strokesJson.put(
                    JSONObject()
                        .put("color", stroke.paint.color)
                        .put(
                            "width",
                            stroke.paint.strokeWidth.toDouble()
                        )
                        .put("points", pointsJson)
                )

                strokeCount++
            }

            pagesJson.put(
                JSONObject()
                    .put("page", pageIndex)
                    .put("strokes", strokesJson)
            )
        }

        val documentJson = JSONObject()
            .put("version", 1)
            .put("pages", pagesJson)

        annotationFile(pdfBytes).writeText(
            documentJson.toString(),
            Charsets.UTF_8
        )

        return strokeCount
    }

    fun loadAnnotations(pdfBytes: ByteArray): Int {
        val file = annotationFile(pdfBytes)

        if (!file.exists()) {
            return -1
        }

        val documentJson = JSONObject(
            file.readText(Charsets.UTF_8)
        )

        require(documentJson.getInt("version") == 1) {
            "Versión de anotaciones no compatible."
        }

        val restoredPages =
            mutableMapOf<Int, MutableList<InkStroke>>()

        val pagesJson = documentJson.getJSONArray("pages")
        var strokeCount = 0

        for (pagePosition in 0 until pagesJson.length()) {
            val pageJson = pagesJson.getJSONObject(pagePosition)
            val pageIndex = pageJson.getInt("page")

            require(pageIndex >= 0) {
                "Índice de página inválido."
            }

            val strokesJson = pageJson.getJSONArray("strokes")
            val restoredStrokes = mutableListOf<InkStroke>()

            for (strokeIndex in 0 until strokesJson.length()) {
                val strokeJson =
                    strokesJson.getJSONObject(strokeIndex)

                val pointsJson =
                    strokeJson.getJSONArray("points")

                if (pointsJson.length() == 0) continue

                val points = mutableListOf<FloatArray>()
                val path = Path()

                for (pointIndex in 0 until pointsJson.length()) {
                    val pointJson =
                        pointsJson.getJSONArray(pointIndex)

                    val x = pointJson.getDouble(0).toFloat()
                    val y = pointJson.getDouble(1).toFloat()

                    require(x.isFinite() && y.isFinite()) {
                        "Coordenadas inválidas."
                    }

                    points.add(floatArrayOf(x, y))

                    if (pointIndex == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                val width =
                    strokeJson.getDouble("width").toFloat()

                require(width.isFinite() && width > 0f) {
                    "Grosor inválido."
                }

                val paint = Paint(inkPaint).apply {
                    color = strokeJson.getInt("color")
                    strokeWidth = width
                }

                restoredStrokes.add(
                    InkStroke(
                        path = path,
                        paint = paint,
                        points = points
                    )
                )

                strokeCount++
            }

            restoredPages[pageIndex] = restoredStrokes
        }

        // Solo reemplazamos los dibujos si todo se leyó bien.
        pendingStroke = null
        pageStrokes.clear()
        pageStrokes.putAll(restoredPages)

        invalidate()

        return strokeCount
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

                if (
                    point[0] !in 0f..1f ||
                    point[1] !in 0f..1f
                ) {
                    return false
                }

                parent?.requestDisallowInterceptTouchEvent(true)

                val path = Path().apply {
                    moveTo(point[0], point[1])
                }

                pendingStroke = InkStroke(
                    path = path,
                    paint = Paint(inkPaint),
                    points = mutableListOf(
                        floatArrayOf(point[0], point[1])
                    )
                )

                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (pendingStroke != null && event.pointerCount == 1) {
                    val point = normalizedPoint(event) ?: return true

                    pendingStroke?.let { stroke ->
                        stroke.path.lineTo(point[0], point[1])

                        stroke.points.add(
                            floatArrayOf(point[0], point[1])
                        )
                    }

                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                pendingStroke?.let { stroke ->
                    normalizedPoint(event)?.let { point ->
                        stroke.path.lineTo(
                            point[0],
                            point[1]
                        )
                        stroke.points.add(
                            floatArrayOf(point[0], point[1])
                        )
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