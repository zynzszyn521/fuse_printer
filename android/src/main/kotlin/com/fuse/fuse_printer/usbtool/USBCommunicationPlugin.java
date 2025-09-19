package com.fuse.fuse_printer.usbtool;

import static com.fuse.fuse_printer.usbtool.serialusb.UsbPrinter.ACTION_USB_STATE;
import static com.fuse.fuse_printer.usbtool.usbprinter.USBReceiver.ACTION_USB_PERMISSION;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aill.androidserialport.SerialPort;
import com.kung.usbtool.serialusb.UsbPrinter;
import com.kung.usbtool.usbprinter.USBUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class USBCommunicationPlugin {
    private static String TAG = USBCommunicationPlugin.class.getSimpleName();
    private Context mContext;
    public static final int REC_DATA = 2;
    private USBStateListener mUSBStateListener;

    private boolean isTsc = true;   //tsc 指令 ;  esc

    private UsbPrinter usbPrinter;//USB打印

    Timer timer = new Timer(true);

    private USBUtil usbUtil;
    private UsbDevice mUsbDevice;

    private ConnectedThread mConnectedThread;


    public String testAndroidSerialPort() {
        String result = "usbtool: " + new Random().nextInt(100);
        return result;
    }


    public void init(Context context, boolean isTSC) {
        mContext = context;
        this.isTsc = isTSC;
        // 注册USB设备状态监听器
        registerUSBStateReceiver();
        // 初始化usb串口的连接
        if (isTSC) {
            USBUtil.getInstance().init(mContext);
        } else {
            usbPrinter = UsbPrinter.getInstance();
            usbPrinter.initPrinter(mContext);
        }
        aotoConnectBluetoothDevice();
    }

    public void close() {
        timer.cancel();
        if (mUsbDeviceReceiver != null) {
            unRegisterUSBStateReceiver();
        }

        if (SerialManager.getInstance().isOpened()) {
            SerialManager.getInstance().closeSerial();
        }
        usbUtil.closeport(500);
    }

    // 注册USB设备状态监听器
    private void registerUSBStateReceiver() {
        // 注册广播监听usb设备
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(ACTION_USB_STATE);
        mContext.registerReceiver(mUsbDeviceReceiver, filter);
    }

    // 取消注册USB设备状态监听器
    public void unRegisterUSBStateReceiver() {
        mContext.unregisterReceiver(mUsbDeviceReceiver);
    }

    // 设置USB设备状态监听器
    public void setUSBStateListener(USBStateListener listener) {
        mUSBStateListener = listener;
    }

    // USB设备状态监听器接口
    public interface USBStateListener {
        void onUSBDeviceDetached();
        void onUSBDeviceAttached();
        void onUSBPrintStateChanged(boolean connected);
        void onUSBDevicePermissionGranted();
        void onUSBReceiveWeightData(String data);
    }

    // USB设备状态监听器的具体实现
    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && mUsbDevice != null) {

                    } else {
                        if (mUSBStateListener != null) {
                            mUSBStateListener.onUSBDevicePermissionGranted();
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (mUsbDevice != null) {
                    if (mUSBStateListener != null) {
                        mUSBStateListener.onUSBDeviceDetached();
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                if (mUSBStateListener != null) {
                    mUSBStateListener.onUSBDeviceAttached();
                }
                usbUtil = USBUtil.getInstance();
                if (mUsbDevice == null) {
                    mUsbDevice = usbUtil.getUsbDevice(19267, 13624);
                }

                if (mUsbDevice != null) {
                    usbUtil.openPort(mUsbDevice);
                }
            } else if (ACTION_USB_STATE.equals(action)) {

                boolean connected = true;
                try {
                    /**  true - USB连接；false - USB未连接 或 电源充电  */
                    connected = intent.getBooleanExtra("connected", false);
////                    ToastUtils.showShort( "有设备" + connected);
////                    kungViewModel.getmIsBlePrinterConnect().setValue(connected);
                    usbUtil = USBUtil.getInstance();
                    if (mUsbDevice == null) {
                        mUsbDevice = usbUtil.getUsbDevice(19267, 13624);
                    }

                    if (mUsbDevice != null) {
                        usbUtil.openPort(mUsbDevice);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    };


    /**
     * 自动连接任务
     */
    private void aotoConnectBluetoothDevice() {
        //定时任务，连接秤
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                SerialManager.getInstance().openSerial();   //打开串口
                // 开启已连接线程
                if (mConnectedThread == null) {
                    mConnectedThread = new ConnectedThread(SerialManager.getInstance().getSerialPort());
                    mConnectedThread.start();
                }

                //显示USB打印机连接状态
                if (isTsc && USBUtil.getInstance().isConnected()) {
                    if (mUSBStateListener != null) {
                        mUSBStateListener.onUSBPrintStateChanged(true);
                    }
                }

            }
        },100,2000);
    }

    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  //设置日期格式
    /**
     * 打印
     */
    public void doPrintUsbTsc(String weight){
        usbUtil = USBUtil.getInstance();
        if (mUsbDevice == null) {
            mUsbDevice = usbUtil.getUsbDevice(19267, 13624);
        }

        if (mUsbDevice != null && usbUtil.openPort(mUsbDevice)) {

            Bitmap bitmap = getImageFromAssetsFile("image/ic_jk.png");
            String s;
            s = "INTIALPRINTER\n" +
                    "SIZE 54 mm,38 mm\n" +
                    "GAP 2 mm,0 mm\n" +
                    "DIRECTION 1 \n" +
                    "CLS\n";
            usbUtil.sendMessage(s.getBytes(StandardCharsets.UTF_8));

            s = "BOX 0,0,360,50,2\n"; //x1,y1,x2,y2,宽度  ：矩形
            usbUtil.sendMessage(s.getBytes(StandardCharsets.UTF_8));

            //打印图片
            if (bitmap != null) {
                tspl_drawGraphic(130,100,bitmap);
            }

            s = "TEXT 2,10,\"3\",0,1.2,1.2,\"" + "健坤小助手" + "\"\n"; //医院名称
            usbUtil.sendMessage(s.getBytes(StandardCharsets.UTF_8));

            s = "BAR 0,75,380,3\n";    //横线
            usbUtil.sendMessage(s.getBytes(StandardCharsets.UTF_8));

            s = "TEXT 2,90,\"4\",0,1,1,\"科室：" + "测试" + "\"\n"; //科室
            usbUtil.sendMessage(s.getBytes(StandardCharsets.UTF_8));

            s = "TEXT 2,120,\"2\",0,1,1,\"车号:" + "测试" + "\"\n"; //类型
            usbUtil.sendMessage(s.getBytes(StandardCharsets.UTF_8));
            s = "TEXT 2,150,\"2\",0,1,1,\"类型:" + "测试" + "\"\n"; //类型
            usbUtil.sendMessage(s.getBytes(StandardCharsets.UTF_8));
            s = "TEXT 2,180,\"2\",0,1,1,\"重量:" + weight + "\"\n"; //类型
            usbUtil.sendMessage(s.getBytes(StandardCharsets.UTF_8));
            s = "TEXT 2,210,\"2\",0,1,1,\"护士:" + "测试" + "\"\n"; //护士
            usbUtil.sendMessage(s.getBytes(StandardCharsets.UTF_8));
            s = "TEXT 2,240,\"2\",0,1,1,\"时间:" + df.format(new Date())  + "\"\n"; //日期
            usbUtil.sendMessage(s.getBytes(StandardCharsets.UTF_8));
            //二维码
            s = "QRCODE 270,110,H,1,M,0,\"" + "二维码内容" + "\"\n";
            usbUtil.sendMessage(s.getBytes(StandardCharsets.UTF_8));

            usbUtil.sendMessage(printlabel(1,1));
        }
    }

    ///绘制图片
    ///1. 算法中对图片以2kb大小进行了切割，防止打印机内存不足，可以自行修改此值
    ///2. 算法中用经验公式对图片进行了二值化处理，每8个点为1mm
    public boolean tspl_drawGraphic(int start_x, int start_y, Bitmap bmp) {
        int bmp_size_x = bmp.getWidth();
        int bmp_size_y = bmp.getHeight();
        int byteWidth = (bmp_size_x - 1) / 8 + 1; //8个点一个byte,所以要先计算byteWidth
        int byteHeight = bmp_size_y;
        if (byteWidth <= 0) return false;
        if (byteHeight <= 0) return false;
        int unitHeight = 2048 / byteWidth;//分包规则，算法可无需过多理解，一个包就unitHeight*byteWidth个点
        int unitCount = (byteHeight - 1) / unitHeight + 1;
        int startY, endY;//每个包y方向起始的位置
        byte[] dataByte;
        int color, A, R, G, B;
        for (int n = 0; n < unitCount; n++) {
//            JXLog.d(TAG, "drawGraphic unit=" + (n + 1) + "/" + unitCount);
            startY = n * unitHeight;
            if ((startY + unitHeight) > bmp_size_y) {
                endY = bmp_size_y;
            } else {
                endY = startY + unitHeight;
            }
            int byteLen = (endY - startY) * byteWidth;//每个包数据的字节个数
            dataByte = new byte[byteLen];
            for (int y = startY; y < endY; y++) {
                for (int x = 0; x < bmp_size_x; x++) {
                    color = bmp.getPixel(x, y);
                    A = color >> 24 & 0xFF;
                    R = color >> 16 & 0xFF;
                    G = color >> 8 & 0xFF;
                    B = color & 0xFF;
                    //经验公式二值化，透明视为白色
                    if (A == 0 || R * 0.3 + G * 0.59 + B * 0.11 > 127) {
                        dataByte[(y - startY) * byteWidth + x / 8] |= (0x80 >> (x % 8));
                    }
                }
            }
            int curUnitHeight = n == unitCount - 1 ? (byteHeight - (n * unitHeight)) : unitHeight;
            String cmdHeader = "BITMAP " + start_x + "," + (start_y + startY) + "," + byteWidth + "," + curUnitHeight + "," + 0 + ",";
            usbUtil.sendMessage(cmdHeader.getBytes(StandardCharsets.UTF_8));
            usbUtil.sendMessage(dataByte);
        }
        return true;
    }

    public byte[] barcode(int x, int y, String type, int height, int human_readable, int rotation, int narrow, int wide, String string) {
        String message = "";
        String barcode = "BARCODE ";
        String position = x + "," + y;
        String mode = '"' + type + '"';
        String height_value = "" + height;
        String human_value = "" + human_readable;
        String rota = "" + rotation;
        String narrow_value = "" + narrow;
        String wide_value = "" + wide;
        String string_value = '"' + string + '"';
        message = barcode + position + " ," + mode + " ," + height_value + " ," + human_value + " ," + rota + " ," + narrow_value + " ," + wide_value + " ," + string_value + "\r\n";
        return message.getBytes();
    }

    public byte[] printlabel(int quantity, int copy) {
        String message = "";
        message = "PRINT " + quantity + ", " + copy + "\r\n";
        return message.getBytes();
    }

    private Bitmap getImageFromAssetsFile(String fileName) {
        Bitmap image = null;
        AssetManager am = mContext.getResources().getAssets();
        try
        {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return image;
    }

    /**
     * 已连接的相关处理线程
     */
    private class ConnectedThread extends Thread {
        private final SerialPort serialPort;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        public ConnectedThread(SerialPort socket) {
            serialPort = socket;
            InputStream is;
            OutputStream os;
            // 获取输入输出流
            is = socket.getInputStream();
            os = socket.getOutputStream();

            mInputStream = is;
            mOutputStream = os;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            DataInputStream in = new DataInputStream(mInputStream);
            byte[] check = new byte[1024];
            // 监听输入流以备获取数据
            while (true) {
                try {
                    if (mInputStream.available() > 0) {
                        //当接收到数据时，sleep 500毫秒（sleep时间自己把握）
                        Thread.sleep(50);
                        //sleep过后，再读取数据，基本上都是完整的数据
//                        buffer = new byte[mInputStream.available()];
//                        size = mInputStream.read(buffer);
                        int l;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        while ((l = in.read(buffer)) != -1) {
                            out.write(buffer, 0, l);
                            break;
                        }
                        //转换为16进制字符串-健坤专用，别的称可能不支持
                        String temp_data = HexUtil.formatHexString(out.toByteArray(), false).toUpperCase();
                        mHandler.obtainMessage(REC_DATA, temp_data).sendToTarget();
                    }
                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, "connection break", e);
                    break;
                }
                try {
                    //线程睡眠20ms以避免过于频繁工作  50ms->20ms 2017.12.2
                    //导致UI处理发回的数据不及时而阻塞
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    // 用于从线程获取信息的Handler对象
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler(){
        String msgResult;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REC_DATA:
                    try{
                        msgResult = (String) msg.obj;
                        //进行解密
                        if (mUSBStateListener != null) {
                            mUSBStateListener.onUSBReceiveWeightData(msgResult);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };
}