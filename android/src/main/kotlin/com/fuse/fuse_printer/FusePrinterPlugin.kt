package com.fuse.fuse_printer

import android.content.Context
import android.hardware.usb.UsbDeviceConnection
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.fuse.fuse_printer.usbtool.USBCommunicationPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


/** FusePrinterPlugin */
class FusePrinterPlugin: FlutterPlugin, MethodCallHandler {

  private lateinit var context: Context
  private lateinit var channel: MethodChannel
  private val CHANNEL_NAME = "com.fuse.printer/methods"
  private lateinit var mUSBCommunicationPlugin: USBCommunicationPlugin

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_NAME)
    channel.setMethodCallHandler(this)
    mUSBCommunicationPlugin = USBCommunicationPlugin()
    mUSBCommunicationPlugin.init(context, 0,0)
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "printInit") {
      val vendorId = call.argument<Int>("vendorId") ?: 0
      val productId = call.argument<Int>("productId") ?: 0
      try {
        mUSBCommunicationPlugin.init(context, vendorId, productId)
        result.success(true)
      } catch (e: Exception) {
        Log.e("FusePrinterPlugin", "Print init error: ${e.message}")
        result.error("PRINT_INIT_ERROR", "初始化打印机失败: ${e.message}", null)
      }
    } else if (call.method == "printText") {
      // 打印文本
      val text = call.argument<String>("text") ?: ""
      try {
        mUSBCommunicationPlugin.doPrintText(text)
        result.success(true)
      } catch (e: Exception) {
        Log.e("FusePrinterPlugin", "Print text error: ${e.message}")
        result.error("PRINT_TEXT_ERROR", "打印文本失败: ${e.message}", null)
      }
    } else if (call.method == "printTextEx") {
      // 打印文本
      val data = call.argument<ByteArray>("data")
      if (data == null || data.isEmpty()) {
        result.error("INVALID_DATA", "传入的字节数组为空", null)
        return
      }
      try {
        mUSBCommunicationPlugin.doPrintTextEx(data)
        result.success(true)
      } catch (e: Exception) {
        Log.e("FusePrinterPlugin", "Print textEx error: ${e.message}")
        result.error("PRINT_TEXTEX_ERROR", "打印扩展文本失败: ${e.message}", null)
      }
    } else if (call.method == "printBarcode") {
      // 打印条码
      val code = call.argument<String>("code") ?: ""
      val type = call.argument<String>("type") ?: "CODE128"
      val height = call.argument<Int>("height") ?: 100
      try {
        val success = mUSBCommunicationPlugin.doPrintBarcode(code, type, height)
        result.success(success)
      } catch (e: Exception) {
        Log.e("FusePrinterPlugin", "Print barcode error: ${e.message}")
        result.error("PRINT_BARCODE_ERROR", "打印条码失败: ${e.message}", null)
      }
    } else if (call.method == "printQRCode") {
      // 打印二维码
      val content = call.argument<String>("content") ?: ""
      val size = call.argument<Int>("size") ?: 10
      try {
        val success = mUSBCommunicationPlugin.doPrintQRCode(content, size)
        result.success(success)
      } catch (e: Exception) {
        Log.e("FusePrinterPlugin", "Print QR code error: ${e.message}")
        result.error("PRINT_QRCODE_ERROR", "打印二维码失败: ${e.message}", null)
      }
    } else if (call.method == "printImage") {
      // 打印图片
      val imagePath = call.argument<String>("imagePath") ?: ""
      try {
        val success = mUSBCommunicationPlugin.doPrintImage(imagePath)
        result.success(success)
      } catch (e: Exception) {
        Log.e("FusePrinterPlugin", "Print image error: ${e.message}")
        result.error("PRINT_IMAGE_ERROR", "打印图片失败: ${e.message}", null)
      }
    } else if (call.method == "printCutPaper") {
      // 切纸
      try {
        val success = mUSBCommunicationPlugin.doCutPaper()
        result.success(success)
      } catch (e: Exception) {
        Log.e("FusePrinterPlugin", "Cut paper error: ${e.message}")
        result.error("CUT_PAPER_ERROR", "切纸失败: ${e.message}", null)
      }
    } else if (call.method == "printFeedPaper") {
      // 进纸
      val lines = call.argument<Int>("lines") ?: 1
      try {
        val success = mUSBCommunicationPlugin.doFeedPaper(lines)
        result.success(success)
      } catch (e: Exception) {
        Log.e("FusePrinterPlugin", "Feed paper error: ${e.message}")
        result.error("FEED_PAPER_ERROR", "进纸失败: ${e.message}", null)
      }
    } else if (call.method == "printClose") {
      // 关闭打印机
      try {
        mUSBCommunicationPlugin.close()
        result.success(true)
      } catch (e: Exception) {
        Log.e("FusePrinterPlugin", "Close printer error: ${e.message}")
        result.error("CLOSE_PRINTER_ERROR", "关闭打印机失败: ${e.message}", null)
      }
    } else if (call.method == "getPrinterStatus") {
      // 获取打印机状态
      try {
        val status = mUSBCommunicationPlugin.getPrinterStatus()
        result.success(status)
      } catch (e: Exception) {
        Log.e("FusePrinterPlugin", "Get printer status error: ${e.message}")
        result.error("GET_STATUS_ERROR", "获取打印机状态失败: ${e.message}", null)
      }
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    try {
      mUSBCommunicationPlugin.close()
    } catch (e: Exception) {
      Log.e("FusePrinterPlugin", "Error closing USB: ${e.message}")
    }
  }
  // 移除未用的 handler 相关代码
}


