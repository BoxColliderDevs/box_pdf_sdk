import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'box_pdf_sdk_method_channel.dart';

abstract class BoxPdfSdkPlatform extends PlatformInterface {
  /// Constructs a BoxPdfSdkPlatform.
  BoxPdfSdkPlatform() : super(token: _token);

  static final Object _token = Object();

  static BoxPdfSdkPlatform _instance = MethodChannelBoxPdfSdk();

  /// The default instance of [BoxPdfSdkPlatform] to use.
  ///
  /// Defaults to [MethodChannelBoxPdfSdk].
  static BoxPdfSdkPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [BoxPdfSdkPlatform] when
  /// they register themselves.
  static set instance(BoxPdfSdkPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
