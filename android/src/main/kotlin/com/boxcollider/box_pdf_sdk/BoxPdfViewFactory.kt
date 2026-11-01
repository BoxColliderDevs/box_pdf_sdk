// android/src/main/kotlin/com/boxcollider/box_pdf_sdk/BoxPdfViewFactory.kt
class BoxPdfViewFactory(
    private val messenger: BinaryMessenger
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, id: Int, args: Any?): PlatformView {
        val params = args as Map<*, *>
        return BoxPdfView(context, id, messenger, params["path"] as String)
    }
}