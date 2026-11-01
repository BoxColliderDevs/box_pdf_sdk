// ios/Classes/BoxPdfView.swift
import PDFKit
import Flutter

class BoxPdfView: NSObject, FlutterPlatformView {
    private let pdfView = PDFView()

    init(frame: CGRect, viewId: Int64, args: Any?, messenger: FlutterBinaryMessenger) {
        super.init()

        if let path = (args as? [String: Any])?["path"] as? String,
           let document = PDFDocument(url: URL(fileURLWithPath: path)) {
            pdfView.document = document
        }

        pdfView.autoScales = true
    }

    func view() -> UIView { pdfView }
}