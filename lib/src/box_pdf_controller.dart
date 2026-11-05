import 'dart:ui';

import 'package:flutter/services.dart';

class BoxPdfController {
  BoxPdfController.forView(int viewId)
    : _channel = MethodChannel('com.boxcollider/box_pdf_viewer_$viewId');

  final MethodChannel _channel;

  Future<void> setDrawingMode(bool enabled) {
    return _channel.invokeMethod<void>('setDrawingMode', {'enabled': enabled});
  }

  Future<void> setColor(Color color) {
    return _channel.invokeMethod<void>('setColor', {'color': color.toARGB32()});
  }

  /// Grosor relativo al documento, no píxeles de pantalla.
  ///
  /// Valores sugeridos:
  /// fino: 0.0015
  /// medio: 0.003
  /// grueso: 0.006
  Future<void> setStrokeWidth(double width) {
    if (!width.isFinite || width < 0.001 || width > 0.02) {
      throw ArgumentError.value(
        width,
        'width',
        'Debe estar entre 0.001 y 0.02.',
      );
    }

    return _channel.invokeMethod<void>('setStrokeWidth', {'width': width});
  }

  Future<void> undo() {
    return _channel.invokeMethod<void>('undo');
  }

  Future<void> clearCurrentPage() {
    return _channel.invokeMethod<void>('clearCurrentPage');
  }

  Future<void> nextPage() {
    return _channel.invokeMethod<void>('nextPage');
  }

  Future<void> previousPage() {
    return _channel.invokeMethod<void>('previousPage');
  }
}
