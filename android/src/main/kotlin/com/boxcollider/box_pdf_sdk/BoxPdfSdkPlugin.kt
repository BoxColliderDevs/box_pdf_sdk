package com.boxcollider.box_pdf_sdk

import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class BoxPdfSdkPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {

    private var channel: MethodChannel? = null

    override fun onAttachedToEngine(
        binding: FlutterPlugin.FlutterPluginBinding
    ) {
        channel = MethodChannel(
            binding.binaryMessenger,
            "box_pdf_sdk"
        )

        channel?.setMethodCallHandler(this)

        binding.platformViewRegistry.registerViewFactory(
            "com.boxcollider/box_pdf_viewer",
            BoxPdfViewFactory()
        )
    }

    override fun onMethodCall(
        call: MethodCall,
        result: MethodChannel.Result
    ) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${Build.VERSION.RELEASE}")
            }

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(
        binding: FlutterPlugin.FlutterPluginBinding
    ) {
        channel?.setMethodCallHandler(null)
        channel = null
    }
}