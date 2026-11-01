import 'package:flutter_test/flutter_test.dart';
import 'package:box_pdf_sdk/box_pdf_sdk.dart';
import 'package:box_pdf_sdk/box_pdf_sdk_platform_interface.dart';
import 'package:box_pdf_sdk/box_pdf_sdk_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockBoxPdfSdkPlatform
    with MockPlatformInterfaceMixin
    implements BoxPdfSdkPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final BoxPdfSdkPlatform initialPlatform = BoxPdfSdkPlatform.instance;

  test('$MethodChannelBoxPdfSdk is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelBoxPdfSdk>());
  });

  test('getPlatformVersion', () async {
    BoxPdfSdk boxPdfSdkPlugin = BoxPdfSdk();
    MockBoxPdfSdkPlatform fakePlatform = MockBoxPdfSdkPlatform();
    BoxPdfSdkPlatform.instance = fakePlatform;

    expect(await boxPdfSdkPlugin.getPlatformVersion(), '42');
  });
}
