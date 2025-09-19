import 'package:flutter_test/flutter_test.dart';
import 'package:fuse_printer/fuse_printer.dart';
import 'package:fuse_printer/fuse_printer_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockPlusTestPlatform
    with MockPlatformInterfaceMixin
    implements FusePrinterPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
  
  @override
  Future<String?> getPrinterStatus() {
    // TODO: implement getPrinterStatus
    throw UnimplementedError();
  }
  
  @override
  Future<bool?> printBarcode({required String code, String type = 'CODE128', int height = 100}) {
    // TODO: implement printBarcode
    throw UnimplementedError();
  }
  
  @override
  Future<bool?> printClose() {
    // TODO: implement printClose
    throw UnimplementedError();
  }
  
  @override
  Future<bool?> printCutPaper() {
    // TODO: implement printCutPaper
    throw UnimplementedError();
  }
  
  @override
  Future<bool?> printFeedPaper({int lines = 1}) {
    // TODO: implement printFeedPaper
    throw UnimplementedError();
  }
  
  @override
  Future<bool?> printImage({required String imagePath}) {
    // TODO: implement printImage
    throw UnimplementedError();
  }
  
  @override
  Future<bool?> printInit({required int vendorId, required int productId}) {
    // TODO: implement printInit
    throw UnimplementedError();
  }
  
  @override
  Future<bool?> printQRCode({required String content, int size = 10}) {
    // TODO: implement printQRCode
    throw UnimplementedError();
  }
  
  @override
  Future<bool?> printText({required String text, String align = 'left', int size = 1}) {
    // TODO: implement printText
    throw UnimplementedError();
  }
  
  @override
  Future<bool?> printTscCommand({required String command}) {
    // TODO: implement printTscCommand
    throw UnimplementedError();
  }
}

void main() {
  test('getPlatformVersion', () async {
    expect(await FusePrinter.getPlatformVersion(), '42');
  });
}
