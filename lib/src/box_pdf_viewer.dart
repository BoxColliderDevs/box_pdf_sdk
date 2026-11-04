import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'box_pdf_controller.dart';

class BoxPdfViewer extends StatelessWidget {
  const BoxPdfViewer({super.key, required this.bytes, this.onCreated});

  final Uint8List bytes;
  final ValueChanged<BoxPdfController>? onCreated;

  @override
  Widget build(BuildContext context) {
    if (kIsWeb || defaultTargetPlatform != TargetPlatform.android) {
      return const Center(
        child: Text('El visor está implementado solo para Android por ahora.'),
      );
    }

    return AndroidView(
      viewType: 'com.boxcollider/box_pdf_viewer',
      creationParams: <String, Object>{'bytes': bytes},
      creationParamsCodec: const StandardMessageCodec(),
      onPlatformViewCreated: (viewId) {
        onCreated?.call(BoxPdfController.forView(viewId));
      },
    );
  }
}
