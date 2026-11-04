package com.boxcollider.box_pdf_sdk

import android.content.Context
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class BoxPdfViewFactory(
    private val messenger: BinaryMessenger
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(
        context: Context,
        viewId: Int,
        args: Any?
    ): PlatformView {
        val params = args as? Map<*, *>
        val bytes = params?.get("bytes") as? ByteArray

        return BoxPdfView(context, bytes).apply {
            bindControlChannel(messenger, viewId)
        }
    }
}