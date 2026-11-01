// lib/src/pdf_annotation.dart
enum PdfAnnotationType { ink, highlight, text, square, underline }

class PdfAnnotation {
  const PdfAnnotation({
    required this.id,
    required this.type,
    required this.page,
    required this.points,
    this.color = '#FF0000',
    this.text,
  });

  final String id;
  final PdfAnnotationType type;
  final int page;
  final List<List<double>> points;
  final String color;
  final String? text;

  Map<String, dynamic> toMap() => {
    'id': id,
    'type': type.name,
    'page': page,
    'points': points,
    'color': color,
    'text': text,
  };

  factory PdfAnnotation.fromMap(Map<Object?, Object?> map) => PdfAnnotation(
    id: map['id'] as String,
    type: PdfAnnotationType.values.byName(map['type'] as String),
    page: map['page'] as int,
    points: (map['points'] as List)
        .map((p) => (p as List).map((e) => (e as num).toDouble()).toList())
        .toList(),
    color: map['color'] as String? ?? '#FF0000',
    text: map['text'] as String?,
  );
}
