
import 'dart:typed_data';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'fuse_printer_method_channel.dart';

abstract class FusePrinterPlatform extends PlatformInterface {
  FusePrinterPlatform() : super(token: _token);

  static final Object _token = Object();

  static FusePrinterPlatform _instance = MethodChannelFusePrinter();

  static FusePrinterPlatform get instance => _instance;

  static set instance(FusePrinterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  // 打印机相关方法
  Future<bool?> printInit({required int vendorId, required int productId}) {
    throw UnimplementedError('printInit() has not been implemented.');
  }

  Future<bool?> printText({required String text}) {
    throw UnimplementedError('printText() has not been implemented.');
  }

  Future<bool?> printTextEx({required Uint8List data}) {
    throw UnimplementedError('printTextEx() has not been implemented.');
  }

  Future<bool?> printBarcode({
    required String code,
    String type = 'CODE128',
    int height = 100,
  }) {
    throw UnimplementedError('printBarcode() has not been implemented.');
  }

  Future<bool?> printQRCode({required String content, int size = 10}) {
    throw UnimplementedError('printQRCode() has not been implemented.');
  }

  Future<bool?> printImage({required String imagePath}) {
    throw UnimplementedError('printImage() has not been implemented.');
  }

  Future<bool?> printTscCommand({required String command}) {
    throw UnimplementedError('printTscCommand() has not been implemented.');
  }

  Future<bool?> printCutPaper() {
    throw UnimplementedError('printCutPaper() has not been implemented.');
  }

  Future<bool?> printFeedPaper({int lines = 1}) {
    throw UnimplementedError('printFeedPaper() has not been implemented.');
  }

  Future<bool?> printClose() {
    throw UnimplementedError('printClose() has not been implemented.');
  }

  Future<bool?> getPrinterStatus() {
    throw UnimplementedError('getPrinterStatus() has not been implemented.');
  }
}
