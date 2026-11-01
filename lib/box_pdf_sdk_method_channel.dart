import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'box_pdf_sdk_platform_interface.dart';

/// An implementation of [BoxPdfSdkPlatform] that uses method channels.
class MethodChannelBoxPdfSdk extends BoxPdfSdkPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('box_pdf_sdk');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }
}
