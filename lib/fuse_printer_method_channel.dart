import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'fuse_printer_platform_interface.dart';

class MethodChannelFusePrinter extends FusePrinterPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('com.fuse.printer/methods');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }

  @override
  Future<bool?> printInit({
    required int vendorId,
    required int productId,
  }) async {
    final result = await methodChannel.invokeMethod<bool>('printInit', {
      'vendorId': vendorId,
      'productId': productId,
    });
    return result;
  }

  @override
  Future<bool?> printCommand({required String command}) async {
    final result = await methodChannel.invokeMethod<bool>('printCommand', {
      'command': command,
    });
    return result;
  }

  @override
  Future<bool?> printBarcode({
    required String code,
    String type = 'CODE128',
    int height = 100,
  }) async {
    final result = await methodChannel.invokeMethod<bool>('printBarcode', {
      'code': code,
      'type': type,
      'height': height,
    });
    return result;
  }

  @override
  Future<bool?> printQRCode({required String content, int size = 10}) async {
    final result = await methodChannel.invokeMethod<bool>('printQRCode', {
      'content': content,
      'size': size,
    });
    return result;
  }

  @override
  Future<bool?> printImage({required String imagePath}) async {
    final result = await methodChannel.invokeMethod<bool>('printImage', {
      'imagePath': imagePath,
    });
    return result;
  }

  @override
  Future<bool?> printTscCommand({required String command}) async {
    final result = await methodChannel.invokeMethod<bool>('printTscCommand', {
      'command': command,
    });
    return result;
  }

  @override
  Future<bool?> printCutPaper() async {
    final result = await methodChannel.invokeMethod<bool>('printCutPaper');
    return result;
  }

  @override
  Future<bool?> printFeedPaper({int lines = 1}) async {
    final result = await methodChannel.invokeMethod<bool>('printFeedPaper', {
      'lines': lines,
    });
    return result;
  }

  @override
  Future<bool?> printClose() async {
    final result = await methodChannel.invokeMethod<bool>('printClose');
    return result;
  }

  @override
  Future<String?> getPrinterStatus() async {
    final result = await methodChannel.invokeMethod<String>('getPrinterStatus');
    return result;
  }
}
