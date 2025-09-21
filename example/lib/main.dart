import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_esc_pos_utils/flutter_esc_pos_utils.dart';
import 'package:fuse_printer/fuse_printer.dart';
import 'package:gbk_codec/gbk_codec.dart';

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
  static const EventChannel _printerEventChannel = EventChannel('com.fuse.printer/events');
  StreamSubscription<dynamic>? _printerEventSubscription;
  final TextEditingController _textController = TextEditingController(
    text: 'Hello, Printer!',
  );
  final TextEditingController _barcodeController = TextEditingController();
  final TextEditingController _qrCodeController = TextEditingController();
  List<int> bytes = [];

  // 常见打印机的Vendor ID和Product ID
  final Map<String, Map<String, int>> _printerDevices = {
    'Zebra ZTC 110Xi4-600dpi': {'vendorId': 4611, 'productId': 304},
    '小米米家喷墨打印一体机': {'vendorId': 12332, 'productId': 5393},
    '爱立熊迷你打印机A2': {'vendorId': 2501, 'productId': 512},
  };

  String _selectedPrinter = 'Zebra ZTC 110Xi4-600dpi';

  @override
  void initState() {
    super.initState();
    initPlatformState();
    _startPrinterEventListener();
  }

  void _startPrinterEventListener() {
    // Subscribe to native events from the plugin
    _printerEventSubscription = _printerEventChannel
        .receiveBroadcastStream()
        .listen((dynamic event) {
      if (event == null) return;
      final Map<dynamic, dynamic> map = Map<dynamic, dynamic>.from(event);
      final String? ev = map['event'] as String?;
      if (ev == null) return;
      if (ev == 'state') {
        final bool connected = map['connected'] as bool? ?? false;
        setState(() {
          _isPrinterConnected = connected;
          _printerStatus = connected ? '已连接' : '未连接';
        });
      } else if (ev == 'attached') {
        setState(() {
          _printerStatus = '设备已插入';
        });
      } else if (ev == 'detached') {
        setState(() {
          _isPrinterConnected = false;
          _printerStatus = '已拔出';
        });
      } else if (ev == 'data') {
        final dynamic data = map['data'];
        // you can handle incoming data here if needed
        debugPrint('Printer data event: $data');
      }
    }, onError: (dynamic err) {
      debugPrint('Printer event error: $err');
    });
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
        });
        showSnackbar('打印机连接成功');
      } else {
        setState(() {
          _isPrinterConnected = false;
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

  // 打印文本
  Future<void> printText() async {
    if (!_isPrinterConnected) {
      showSnackbar('请先连接打印机');
      return;
    }

    try {
      final text =
          _textController.text.isEmpty ? '测试打印文本' : _textController.text;
      final success = await FusePrinter.printText(text: "$text\n");
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

  // 打印扩展文本
  Future<void> printTextEx() async {
    if (!_isPrinterConnected) {
      showSnackbar('请先连接打印机');
      return;
    }

    try {
      // final text =
      // _textController.text.isEmpty ? '测试打印文本' : _textController.text;
      await testEscPos();
      final success = await FusePrinter.printTextEx(
        data: Uint8List.fromList(bytes),
      );
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

  Future<void> testEscPos() async {
  final CapabilityProfile profile = await CapabilityProfile.load();
  profile.codePages = [];
    bytes.clear();

    // // 标题
    // final title = gbk.encode('*** Test(测试) ***\r\n');
    // bytes += generator.textEncoded(
    //   Uint8List.fromList(title),
    //   styles: PosStyles(align: PosAlign.center, bold: true),
    // );
    // // 分割线
    // bytes += generator.hr();
    // // 一维码
    // bytes += generator.barcode(Barcode.code128('123456'.codeUnits));
    // // 二维码
    // bytes += generator.qrcode(
    //   '123456',
    //   size: QRSize.size8,
    //   cor: QRCorrection.L,
    // );
    // // 分割线
    // bytes += generator.hr();
    bytes.addAll('123456\r\n'.codeUnits);
    // bytes += generator.row([
    //   PosColumn(width:9, text: '炒饼', styles: PosStyles(align: PosAlign.left), containsChinese: true),
    //   PosColumn(width:3, text: '￥1,990', styles: PosStyles(align: PosAlign.right), containsChinese: true),
    // ]);
    // bytes +=[13, 10];
    //时间
    // final timeStr = 'Time(时间): ${DateTime.now()}\r\n';
    // final timeBytes = gbk.encode(timeStr);
    // bytes += generator.textEncoded(Uint8List.fromList(timeBytes));

    //切纸
    // bytes += generator.cut();
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
      await FusePrinter.printText(text: '=== 打印测试页面 ===');
      await FusePrinter.printFeedPaper();

      // 打印文本
      await FusePrinter.printText(text: '这是一段测试文本');
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
                    child: const Text('连接设备'),
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
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: printTextEx,
              style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
              child: const Text('打印文本扩展'),
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
    _printerEventSubscription?.cancel();
    super.dispose();
  }
}
