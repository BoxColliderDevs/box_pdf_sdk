// android/src/main/kotlin/com/boxcollider/box_pdf_sdk/BoxPdfPlugin.kt
class BoxPdfPlugin : FlutterPlugin {
    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        binding.platformViewRegistry.registerViewFactory(
            "com.boxcollider/box_pdf_viewer",
            BoxPdfViewFactory(binding.binaryMessenger)
        )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {}
}