package com.boxcollider.box_pdf_sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.View
import android.widget.Button
//import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.flutter.plugin.platform.PlatformView
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class BoxPdfView(
    private val context: Context,
    private val pdfBytes: ByteArray?
) : PlatformView {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var disposed = false

    private var currentPage = 0
    private var totalPages = 0
    private var currentBitmap: Bitmap? = null

    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.rgb(230, 230, 230))
    }

    private val status = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 16f
        setTextColor(Color.BLACK)
        setPadding(12, 12, 12, 12)
        text = "Preparando PDF..."
    }

    private val image = ZoomPdfImageView(context)
    
    private val previousButton = Button(context).apply {
        text = "Anterior"
        isEnabled = false
    }

    private val nextButton = Button(context).apply {
        text = "Siguiente"
        isEnabled = false
    }

    init {
        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        controls.addView(
            previousButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        controls.addView(
            nextButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        root.addView(status)

        root.addView(
            image,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(controls)

        previousButton.setOnClickListener {
            if (currentPage > 0) {
                renderPage(currentPage - 1)
            }
        }

        nextButton.setOnClickListener {
            if (currentPage + 1 < totalPages) {
                renderPage(currentPage + 1)
            }
        }

        renderPage(0)
    }

    private fun renderPage(index: Int) {
        if (disposed) return

        previousButton.isEnabled = false
        nextButton.isEnabled = false
        status.text = "Cargando página ${index + 1}..."

        executor.execute {
            var temporaryFile: File? = null
            var renderedBitmap: Bitmap? = null

            try {
                val bytes = pdfBytes
                    ?: throw IllegalArgumentException(
                        "No se recibieron los bytes del PDF."
                    )

                if (bytes.isEmpty()) {
                    throw IllegalArgumentException("El PDF está vacío.")
                }

                val file = File.createTempFile(
                    "box_pdf_",
                    ".pdf",
                    context.cacheDir
                )

                temporaryFile = file
                file.writeBytes(bytes)

                val descriptor = ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )

                val renderer = try {
                    PdfRenderer(descriptor)
                } catch (error: Exception) {
                    descriptor.close()
                    throw error
                }

                var pageCount = 0

                renderer.use { pdf ->
                    pageCount = pdf.pageCount

                    require(index in 0 until pageCount) {
                        "Número de página fuera de rango."
                    }

                    pdf.openPage(index).use { page ->
                        val width = context.resources
                            .displayMetrics.widthPixels
                            .coerceIn(1, 1600)

                        val height = (
                            width.toDouble() *
                                page.height / page.width
                            ).roundToInt().coerceAtLeast(1)

                        require(height <= 12000) {
                            "La página es demasiado alta para esta prueba."
                        }

                        val bitmap = Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                        )

                        renderedBitmap = bitmap
                        bitmap.eraseColor(Color.WHITE)

                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                    }
                }

                val bitmap = checkNotNull(renderedBitmap)

                mainHandler.post {
                    if (disposed) {
                        bitmap.recycle()
                    } else {
                        val oldBitmap = currentBitmap

                        image.setImageBitmap(bitmap)
                        currentBitmap = bitmap
                        oldBitmap?.recycle()

                        currentPage = index
                        totalPages = pageCount

                        status.text =
                            "Página ${currentPage + 1} de $totalPages"

                        updateButtons()
                    }
                }
            } catch (error: Exception) {
                renderedBitmap?.recycle()

                mainHandler.post {
                    if (!disposed) {
                        status.text =
                            "No se pudo abrir el PDF: ${error.message}"

                        updateButtons()
                    }
                }
            } finally {
                temporaryFile?.delete()
            }
        }
    }

    private fun updateButtons() {
        previousButton.isEnabled = currentPage > 0

        nextButton.isEnabled =
            totalPages > 0 && currentPage < totalPages - 1
    }

    override fun getView(): View = root

    override fun dispose() {
        disposed = true
        executor.shutdown()

        image.setImageDrawable(null)
        currentBitmap?.recycle()
        currentBitmap = null
    }
}