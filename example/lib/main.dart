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

  @override
  void initState() {
    super.initState();
    _pdf = _loadPdf();
  }

  Future<Uint8List> _loadPdf() async {
    final data = await rootBundle.load('assets/prueba.pdf');

    return data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Box PDF SDK')),
      body: SafeArea(
        child: FutureBuilder<Uint8List>(
          future: _pdf,
          builder: (context, snapshot) {
            if (snapshot.hasError) {
              return Center(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Text(
                    'No se pudo cargar el archivo:\n${snapshot.error}',
                  ),
                ),
              );
            }

            final bytes = snapshot.data;

            if (bytes == null) {
              return const Center(child: CircularProgressIndicator());
            }

            return BoxPdfViewer(bytes: bytes);
          },
        ),
      ),
    );
  }
}
