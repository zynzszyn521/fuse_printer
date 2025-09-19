import 'fuse_printer_platform_interface.dart';

abstract class FusePrinter {
  //獲取Android版本
  static Future<String?> getPlatformVersion() {
    return FusePrinterPlatform.instance.getPlatformVersion();
  }

  // 打印机相关方法
  /// 初始化打印机
  static Future<bool?> printInit({
    required int vendorId,
    required int productId,
  }) {
    return FusePrinterPlatform.instance.printInit(
      vendorId: vendorId,
      productId: productId,
    );
  }

  /// 打印文本
  static Future<bool?> printCommand({required String command}) {
    return FusePrinterPlatform.instance.printCommand(command: command);
  }

  /// 打印条码
  static Future<bool?> printBarcode({
    required String code,
    String type = 'CODE128',
    int height = 100,
  }) {
    return FusePrinterPlatform.instance.printBarcode(
      code: code,
      type: type,
      height: height,
    );
  }

  /// 打印二维码
  static Future<bool?> printQRCode({required String content, int size = 10}) {
    return FusePrinterPlatform.instance.printQRCode(
      content: content,
      size: size,
    );
  }

  /// 打印图片
  static Future<bool?> printImage({required String imagePath}) {
    return FusePrinterPlatform.instance.printImage(imagePath: imagePath);
  }

  /// 发送TSC命令
  static Future<bool?> printTscCommand({required String command}) {
    return FusePrinterPlatform.instance.printTscCommand(command: command);
  }

  /// 切纸
  static Future<bool?> printCutPaper() {
    return FusePrinterPlatform.instance.printCutPaper();
  }

  /// 进纸
  static Future<bool?> printFeedPaper({int lines = 1}) {
    return FusePrinterPlatform.instance.printFeedPaper(lines: lines);
  }

  /// 关闭打印机连接
  static Future<bool?> printClose() {
    return FusePrinterPlatform.instance.printClose();
  }

  /// 获取打印机状态
  static Future<String?> getPrinterStatus() {
    return FusePrinterPlatform.instance.getPrinterStatus();
  }
}
