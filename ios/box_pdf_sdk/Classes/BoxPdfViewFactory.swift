// ios/Classes/BoxPdfViewFactory.swift
class BoxPdfViewFactory: NSObject, FlutterPlatformViewFactory {
    private let messenger: FlutterBinaryMessenger

    init(messenger: FlutterBinaryMessenger) {
        self.messenger = messenger
    }

    func create(
        withFrame frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?
    ) -> FlutterPlatformView {
        BoxPdfView(frame: frame, viewId: viewId, args: args, messenger: messenger)
    }
}