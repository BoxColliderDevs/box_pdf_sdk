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
  Color _selectedColor = Colors.red;
  double _selectedWidth = 0.006;

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

  Future<void> _changeColor(Color color) async {
    final controller = _controller;
    if (controller == null) return;

    try {
      await controller.setColor(color);

      if (!mounted) return;

      setState(() {
        _selectedColor = color;
      });
    } catch (error) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('No se pudo cambiar el color: $error')),
      );
    }
  }

  Future<void> _changeWidth(double width) async {
    final controller = _controller;
    if (controller == null) return;

    try {
      await controller.setStrokeWidth(width);

      if (!mounted) return;

      setState(() {
        _selectedWidth = width;
      });
    } catch (error) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('No se pudo cambiar el grosor: $error')),
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
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              child: Wrap(
                spacing: 16,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text('Color: '),
                      DropdownButton<Color>(
                        value: _selectedColor,
                        onChanged: ready
                            ? (color) {
                                if (color != null) {
                                  _changeColor(color);
                                }
                              }
                            : null,
                        items: const [
                          DropdownMenuItem(
                            value: Colors.red,
                            child: Text('Rojo'),
                          ),
                          DropdownMenuItem(
                            value: Colors.blue,
                            child: Text('Azul'),
                          ),
                          DropdownMenuItem(
                            value: Colors.black,
                            child: Text('Negro'),
                          ),
                          DropdownMenuItem(
                            value: Colors.green,
                            child: Text('Verde'),
                          ),
                        ],
                      ),
                    ],
                  ),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text('Grosor: '),
                      DropdownButton<double>(
                        value: _selectedWidth,
                        onChanged: ready
                            ? (width) {
                                if (width != null) {
                                  _changeWidth(width);
                                }
                              }
                            : null,
                        items: const [
                          DropdownMenuItem(value: 0.003, child: Text('Fino')),
                          DropdownMenuItem(value: 0.006, child: Text('Medio')),
                          DropdownMenuItem(value: 0.012, child: Text('Grueso')),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
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
