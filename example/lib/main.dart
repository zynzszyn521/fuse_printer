import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:fuse_printer/fuse_printer.dart';

void main() {
  runApp(const PrinterApp());
}

class PrinterApp extends StatelessWidget {
  const PrinterApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Fuse Printer 示例',
      debugShowCheckedModeBanner: false,
      home: MyHomePage(), // 把原来的 MyApp 改名为 MyHomePage
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String _platformVersion = 'Unknown';
  String _printerStatus = '未连接';
  bool _isPrinterConnected = false;
  TextEditingController _textController = TextEditingController();
  TextEditingController _barcodeController = TextEditingController();
  TextEditingController _qrCodeController = TextEditingController();

  // 常见打印机的Vendor ID和Product ID
  final Map<String, Map<String, int>> _printerDevices = {
    'Zebra ZTC 110Xi4-600dpi': {'vendorId': 4611, 'productId': 304},
    '小米米家喷墨打印一体机': {'vendorId': 12332, 'productId': 5393},
  };

  String _selectedPrinter = 'Zebra ZTC 110Xi4-600dpi';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  Future<void> initPlatformState() async {
    String platformVersion;
    try {
      platformVersion =
          await FusePrinter.getPlatformVersion() ?? 'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    if (!mounted) return;
    setState(() {
      _platformVersion = platformVersion;
    });
  }

  // 连接打印机
  Future<void> connectPrinter() async {
    try {
      final device = _printerDevices[_selectedPrinter];
      final success = await FusePrinter.printInit(
        vendorId: device!['vendorId']!,
        productId: device['productId']!,
      );
      if (success != null && success) {
        setState(() {
          _isPrinterConnected = true;
          _printerStatus = '已连接';
        });
        showSnackbar('打印机连接成功');
        // 连接成功后获取打印机状态
        getPrinterStatus();
      } else {
        setState(() {
          _isPrinterConnected = false;
          _printerStatus = '连接失败';
        });
        showSnackbar('打印机连接失败');
      }
    } on PlatformException catch (e) {
      setState(() {
        _isPrinterConnected = false;
        _printerStatus = '连接异常: ${e.message}';
      });
      showSnackbar('连接异常: ${e.message}');
    }
  }

  // 断开打印机连接
  Future<void> disconnectPrinter() async {
    try {
      final success = await FusePrinter.printClose();
      setState(() {
        _isPrinterConnected = false;
        _printerStatus = '已断开';
      });
      showSnackbar(success != null && success ? '打印机已断开' : '断开失败');
    } on PlatformException catch (e) {
      showSnackbar('断开异常: ${e.message}');
    }
  }

  // 获取打印机状态
  Future<void> getPrinterStatus() async {
    try {
      final status = await FusePrinter.getPrinterStatus();
      setState(() {
        _printerStatus = status ?? '未知状态';
      });
    } on PlatformException catch (e) {
      showSnackbar('获取状态异常: ${e.message}');
    }
  }

  // 打印文本
  Future<void> printText() async {
    if (!_isPrinterConnected) {
      showSnackbar('请先连接打印机');
      return;
    }

    try {
      final text =
          _textController.text.isEmpty ? '测试打印文本' : _textController.text;
      String command = '''
SIZE 54 mm,38 mm
GAP 2 mm,0 mm
DIRECTION 1
CLS
BOX 0,0,360,50,2
TEXT 2,10,"3",0,1.2,1.2,"$text"
BAR 0,75,380,3
TEXT 2,90,"4",0,1,1,"科室：测试"
TEXT 2,120,"2",0,1,1,"车号:测试"
TEXT 2,150,"2",0,1,1,"类型:测试"
TEXT 2,180,"2",0,1,1,"重量:123kg"
TEXT 2,210,"2",0,1,1,"护士:测试"
TEXT 2,240,"2",0,1,1,"时间:2025-09-20"
QRCODE 270,110,H,1,M,0,"二维码内容"
PRINT 1,1
''';
      final success = await FusePrinter.printCommand(command: command);
      if (success != null && success) {
        await FusePrinter.printFeedPaper(lines: 3);
        showSnackbar('文本打印成功');
      } else {
        showSnackbar('文本打印失败');
      }
    } on PlatformException catch (e) {
      showSnackbar('打印异常: ${e.message}');
    }
  }

  // 打印条码
  Future<void> printBarcode() async {
    if (!_isPrinterConnected) {
      showSnackbar('请先连接打印机');
      return;
    }

    try {
      final code =
          _barcodeController.text.isEmpty
              ? '1234567890'
              : _barcodeController.text;
      final success = await FusePrinter.printBarcode(
        code: code,
        type: 'CODE128',
        height: 100,
      );
      if (success != null && success) {
        await FusePrinter.printFeedPaper(lines: 3);
        showSnackbar('条码打印成功');
      } else {
        showSnackbar('条码打印失败');
      }
    } on PlatformException catch (e) {
      showSnackbar('打印异常: ${e.message}');
    }
  }

  // 打印二维码
  Future<void> printQRCode() async {
    if (!_isPrinterConnected) {
      showSnackbar('请先连接打印机');
      return;
    }

    try {
      final content =
          _qrCodeController.text.isEmpty
              ? 'https://example.com'
              : _qrCodeController.text;
      final success = await FusePrinter.printQRCode(content: content, size: 10);
      if (success != null && success) {
        await FusePrinter.printFeedPaper(lines: 3);
        showSnackbar('二维码打印成功');
      } else {
        showSnackbar('二维码打印失败');
      }
    } on PlatformException catch (e) {
      showSnackbar('打印异常: ${e.message}');
    }
  }

  // 打印测试页面
  Future<void> printTestPage() async {
    if (!_isPrinterConnected) {
      showSnackbar('请先连接打印机');
      return;
    }

    try {
      // 打印标题
      await FusePrinter.printCommand(command: '=== 打印测试页面 ===');
      await FusePrinter.printFeedPaper();

      // 打印文本
      await FusePrinter.printCommand(command: '这是一段测试文本');
      await FusePrinter.printFeedPaper();

      // 打印条码
      await FusePrinter.printBarcode(
        code: 'TEST123456',
        type: 'CODE128',
        height: 80,
      );
      await FusePrinter.printFeedPaper();

      // 打印二维码
      await FusePrinter.printQRCode(
        content: 'https://example.com/test',
        size: 8,
      );
      await FusePrinter.printFeedPaper(lines: 3);

      // 切纸
      await FusePrinter.printCutPaper();

      showSnackbar('测试页面打印成功');
    } on PlatformException catch (e) {
      showSnackbar('打印异常: ${e.message}');
    }
  }

  // 显示提示信息
  void showSnackbar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), duration: const Duration(seconds: 2)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Fuse Printer 测试应用')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('运行在: $_platformVersion\n'),
            Text(
              '打印机状态: $_printerStatus',
              style: TextStyle(
                color: _isPrinterConnected ? Colors.green : Colors.red,
              ),
            ),

            const SizedBox(height: 20),

            // 打印机选择
            DropdownButtonFormField<String>(
              value: _selectedPrinter,
              decoration: const InputDecoration(
                labelText: '选择打印机型号',
                border: OutlineInputBorder(),
              ),
              items:
                  _printerDevices.keys.map((String device) {
                    return DropdownMenuItem<String>(
                      value: device,
                      child: Text(device),
                    );
                  }).toList(),
              onChanged: (String? newValue) {
                setState(() {
                  _selectedPrinter = newValue!;
                });
              },
            ),

            const SizedBox(height: 10),

            // 连接和断开按钮
            Row(
              children: [
                Expanded(
                  child: ElevatedButton(
                    onPressed: connectPrinter,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.blue,
                    ),
                    child: const Text('连接打印机'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: ElevatedButton(
                    onPressed: disconnectPrinter,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.grey,
                    ),
                    child: const Text('断开连接'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: ElevatedButton(
                    onPressed: getPrinterStatus,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.purple,
                    ),
                    child: const Text('获取状态'),
                  ),
                ),
              ],
            ),

            const SizedBox(height: 20),

            // 文本打印
            TextField(
              controller: _textController,
              decoration: const InputDecoration(
                labelText: '输入要打印的文本',
                hintText: '默认为"测试打印文本"',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: printText,
              style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
              child: const Text('打印文本'),
            ),

            const SizedBox(height: 20),

            // 条码打印
            TextField(
              controller: _barcodeController,
              decoration: const InputDecoration(
                labelText: '输入条码内容',
                hintText: '默认为"1234567890"',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: printBarcode,
              style: ElevatedButton.styleFrom(backgroundColor: Colors.orange),
              child: const Text('打印条码'),
            ),

            const SizedBox(height: 20),

            // 二维码打印
            TextField(
              controller: _qrCodeController,
              decoration: const InputDecoration(
                labelText: '输入二维码内容',
                hintText: '默认为"https://example.com"',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: printQRCode,
              style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
              child: const Text('打印二维码'),
            ),

            const SizedBox(height: 20),

            // 测试页面打印
            ElevatedButton(
              onPressed: printTestPage,
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.teal,
                padding: const EdgeInsets.symmetric(vertical: 15),
              ),
              child: const Text('打印测试页面', style: TextStyle(fontSize: 18)),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _textController.dispose();
    _barcodeController.dispose();
    _qrCodeController.dispose();
    super.dispose();
  }
}
