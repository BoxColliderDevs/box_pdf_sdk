import 'dart:typed_data';

import 'package:box_pdf_sdk/box_pdf_sdk.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const PdfExampleApp());
}

class PdfExampleApp extends StatelessWidget {
  const PdfExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(colorSchemeSeed: Colors.blue),
      home: const PdfExamplePage(),
    );
  }
}

class PdfExamplePage extends StatefulWidget {
  const PdfExamplePage({super.key});

  @override
  State<PdfExamplePage> createState() => _PdfExamplePageState();
}

class _PdfExamplePageState extends State<PdfExamplePage> {
  late final Future<Uint8List> _pdf;

  BoxPdfController? _controller;

  @override
  void initState() {
    super.initState();
    _pdf = _loadPdf();
  }

  Future<Uint8List> _loadPdf() async {
    final data = await rootBundle.load('assets/prueba.pdf');

    return data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes);
  }

  Future<void> _execute(Future<void> Function(BoxPdfController) action) async {
    final controller = _controller;

    if (controller == null) return;

    try {
      await action(controller);
    } catch (error) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error al controlar el visor: $error')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final ready = _controller != null;

    return Scaffold(
      appBar: AppBar(title: const Text('Box PDF SDK')),
      body: SafeArea(
        child: Column(
          children: [
            Wrap(
              spacing: 8,
              children: [
                TextButton(
                  onPressed: ready
                      ? () => _execute(
                          (controller) => controller.setDrawingMode(true),
                        )
                      : null,
                  child: const Text('Dibujar'),
                ),
                TextButton(
                  onPressed: ready
                      ? () => _execute(
                          (controller) => controller.setDrawingMode(false),
                        )
                      : null,
                  child: const Text('Navegar'),
                ),
                IconButton(
                  tooltip: 'Deshacer',
                  onPressed: ready
                      ? () => _execute((controller) => controller.undo())
                      : null,
                  icon: const Icon(Icons.undo),
                ),
                IconButton(
                  tooltip: 'Limpiar página',
                  onPressed: ready
                      ? () => _execute(
                          (controller) => controller.clearCurrentPage(),
                        )
                      : null,
                  icon: const Icon(Icons.delete_outline),
                ),
              ],
            ),
            Expanded(
              child: FutureBuilder<Uint8List>(
                future: _pdf,
                builder: (context, snapshot) {
                  if (snapshot.hasError) {
                    return Center(
                      child: Text('Error al cargar PDF: ${snapshot.error}'),
                    );
                  }

                  final bytes = snapshot.data;

                  if (bytes == null) {
                    return const Center(child: CircularProgressIndicator());
                  }

                  return BoxPdfViewer(
                    bytes: bytes,
                    onCreated: (controller) {
                      if (!mounted) return;

                      setState(() {
                        _controller = controller;
                      });
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
