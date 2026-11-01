import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class BoxPdfViewer extends StatelessWidget {
  const BoxPdfViewer({super.key, required this.bytes});

  final Uint8List bytes;

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform != TargetPlatform.android) {
      return const Center(
        child: Text(
          'El visor PDF está implementado solo para Android por ahora.',
        ),
      );
    }

    return AndroidView(
      viewType: 'com.boxcollider/box_pdf_viewer',
      creationParams: <String, Object>{'bytes': bytes},
      creationParamsCodec: const StandardMessageCodec(),
    );
  }
}
