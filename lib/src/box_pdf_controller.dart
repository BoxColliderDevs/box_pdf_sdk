import 'package:flutter/services.dart';

class BoxPdfController {
  BoxPdfController.forView(int viewId)
    : _channel = MethodChannel('com.boxcollider/box_pdf_viewer_$viewId');

  final MethodChannel _channel;

  Future<void> setDrawingMode(bool enabled) {
    return _channel.invokeMethod<void>('setDrawingMode', {'enabled': enabled});
  }

  Future<void> undo() {
    return _channel.invokeMethod<void>('undo');
  }

  Future<void> clearCurrentPage() {
    return _channel.invokeMethod<void>('clearCurrentPage');
  }
}
