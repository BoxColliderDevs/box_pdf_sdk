// ios/Classes/BoxPdfPlugin.swift
public class BoxPdfPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        registrar.register(
            BoxPdfViewFactory(messenger: registrar.messenger()),
            forView: "com.boxcollider/box_pdf_viewer"
        )
    }
}