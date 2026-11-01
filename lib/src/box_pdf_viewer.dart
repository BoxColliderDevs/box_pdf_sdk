// lib/src/box_pdf_viewer.dart
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'pdf_annotation.dart';

class BoxPdfViewer extends StatefulWidget {
  const BoxPdfViewer({
    super.key,
    required this.filePath,
    this.onAnnotationAdded,
    this.onAnnotationUpdated,
  });

  final String filePath;
  final ValueChanged<PdfAnnotation>? onAnnotationAdded;
  final ValueChanged<PdfAnnotation>? onAnnotationUpdated;

  @override
  State<BoxPdfViewer> createState() => _BoxPdfViewerState();
}

class _BoxPdfViewerState extends State<BoxPdfViewer> {
  static const _viewType = 'com.boxcollider/box_pdf_viewer';

  MethodChannel? _channel;
  StreamSubscription<dynamic>? _events;

  @override
  void dispose() {
    _events?.cancel();
    super.dispose();
  }

  void _onViewCreated(int id) {
    _channel = MethodChannel('com.boxcollider/box_pdf_viewer_$id');

    final event = EventChannel('com.boxcollider/box_pdf_viewer_events_$id');
    _events = event.receiveBroadcastStream().listen((data) {
      final map = Map<Object?, Object?>.from(data as Map);
      final annotation = PdfAnnotation.fromMap(map);

      switch (map['event']) {
        case 'annotationAdded':
          widget.onAnnotationAdded?.call(annotation);
        case 'annotationUpdated':
          widget.onAnnotationUpdated?.call(annotation);
      }
    });

    _channel?.invokeMethod<void>('open', {'path': widget.filePath});
  }

  @override
  Widget build(BuildContext context) {
    return AndroidView(
      viewType: _viewType,
      creationParams: {'path': widget.filePath},
      creationParamsCodec: const StandardMessageCodec(),
      onPlatformViewCreated: _onViewCreated,
    );
  }
}
