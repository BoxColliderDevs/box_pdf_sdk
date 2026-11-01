import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:box_pdf_sdk/box_pdf_sdk_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelBoxPdfSdk platform = MethodChannelBoxPdfSdk();
  const MethodChannel channel = MethodChannel('box_pdf_sdk');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
          return '42';
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
