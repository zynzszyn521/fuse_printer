package com.fuse.fuse_printer.usbtool;

import androidx.annotation.NonNull;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.fuse.fuse_printer.usbtool.usbprinter.USBUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class USBCommunicationPlugin {

    private static final String TAG = USBCommunicationPlugin.class.getSimpleName();

    // 设备信息由外部传入
    private int mVendorId = 0;
    private int mProductId = 0;

    private Context mContext;
    private USBStateListener mUSBStateListener;

    private USBUtil usbUtil;

    private Timer timer;
    private boolean isAutoConnecting = false;

    // 时间格式
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    // Handler 改为静态内部类，避免内存泄漏
    private static class SafeHandler extends Handler {
        private final WeakReference<USBCommunicationPlugin> pluginRef;

        public SafeHandler(USBCommunicationPlugin plugin) {
            super(Looper.getMainLooper());
            this.pluginRef = new WeakReference<>(plugin);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            USBCommunicationPlugin plugin = pluginRef.get();
            if (plugin == null || plugin.mUSBStateListener == null) return;

            switch (msg.what) {
                case REC_DATA:
                    String data = (String) msg.obj;
                    plugin.mUSBStateListener.onUSBReceiveWeightData(data);
                    break;
                case USB_STATE_CHANGED:
                    boolean connected = (boolean) msg.obj;
                    plugin.mUSBStateListener.onUSBPrintStateChanged(connected);
                    break;
            }
        }
    }

    private static final int REC_DATA = 2;
    private static final int USB_STATE_CHANGED = 1001;

    private final Handler mHandler = new SafeHandler(this);

    public interface USBStateListener {
        void onUSBDeviceDetached();
        void onUSBDeviceAttached();
        void onUSBPrintStateChanged(boolean connected);
        void onUSBReceiveWeightData(String data);
    }

    public void setUSBStateListener(USBStateListener listener) {
        this.mUSBStateListener = listener;
    }

    public void init(Context context, int vendorId, int productId) {
        this.mContext = context.getApplicationContext();
        this.mVendorId = vendorId;
        this.mProductId = productId;
        // true: TSC, false: ESC

        // 初始化 USBUtil
        usbUtil = USBUtil.getInstance(mContext);

        // 注册广播
        registerUSBStateReceiver();

        // 自动连接
        startAutoConnect();
    }

    public void close() {
        stopAutoConnect();
        unRegisterUSBStateReceiver();
        if (usbUtil != null) {
            usbUtil.close();
        }
    }

    private void startAutoConnect() {
        if (isAutoConnecting) return;
        isAutoConnecting = true;

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                connectUSBPrinter();
            }
        }, 100, 2000);
    }

    private void stopAutoConnect() {
        isAutoConnecting = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * 尝试连接USB打印机
     */
    private void connectUSBPrinter() {
        Log.i(TAG, "检查连接的USBPrinter...");
        if (usbUtil.isConnected()) {
            mHandler.obtainMessage(USB_STATE_CHANGED, true).sendToTarget();
            return;
        }

        // 查找设备
        UsbDevice mUsbDevice = usbUtil.findUsbDevice(mVendorId, mProductId);
        if (mUsbDevice == null) {
            Log.w(TAG, "未找到匹配的USB设备:VendorID=" + mVendorId + ", ProductID=" + mProductId);
            mHandler.obtainMessage(USB_STATE_CHANGED, false).sendToTarget();
            return;
        }

        // 请求权限或初始化
        if (!usbUtil.initUsbDevice(mVendorId, mProductId)) {
            // 需要请求权限
            Log.i(TAG, "请求USB权限...");

            String action = USBUtil.getInstance(mContext).getPermissionAction();
            Intent intent = new Intent(action);
            intent.setPackage(mContext.getPackageName()); // 显式 Intent

            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 推荐使用 IMMUTABLE，除非你需要修改 Intent
                pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            } else {
                // Android 12 以下仍可用 MUTABLE
                pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_MUTABLE);
            }
            ((UsbManager) mContext.getSystemService(Context.USB_SERVICE)).requestPermission(mUsbDevice, pendingIntent);
        } else {
            Log.i(TAG, "USB设备已连接");
            mHandler.obtainMessage(USB_STATE_CHANGED, true).sendToTarget();
        }
    }

    // 广播接收器
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            Log.i(TAG, "接收到Action:"+action);
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device == null) return;

            if (USBUtil.getInstance(context).getPermissionAction().equals(action)) {
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (granted) {
                    Log.i(TAG, "USB权限已授予，尝试初始化");
                    connectUSBPrinter(); // 重新尝试连接
                } else {
                    Log.e(TAG, "用户拒绝了USB权限");
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (mUSBStateListener != null) {
                    mUSBStateListener.onUSBDeviceDetached();
                    mHandler.obtainMessage(USB_STATE_CHANGED, false).sendToTarget();
                }
                usbUtil.close();
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                if (mUSBStateListener != null) {
                    mUSBStateListener.onUSBDeviceAttached();
                }
                connectUSBPrinter();
            }
        }
    };

    private void registerUSBStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(USBUtil.getInstance(mContext).getPermissionAction());
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.registerReceiver(mUsbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            mContext.registerReceiver(mUsbReceiver, filter);
        }
    }

    public void unRegisterUSBStateReceiver() {
        try {
            mContext.unregisterReceiver(mUsbReceiver);
        } catch (Exception e) {
            Log.e(TAG, "unregisterReceiver failed", e);
        }
    }

    private void sendCommand(String cmd) {
        usbUtil.sendData(cmd.getBytes(StandardCharsets.UTF_8));
    }

    // 打印文本(ESC)
    public void doPrintText(String text) {
        if (!usbUtil.isConnected()) {
            Log.e(TAG, "USB未连接，无法打印");
            return;
        }
        new Thread(() -> {
            try {
                Log.i(TAG, "打印内容:"+text);
                byte[] init = new byte[] {0x10, (byte)0xFF, (byte)0xFE, 0x01, 0x1B, 0x40}; // 初始化
                byte[] data = text.getBytes("GBK");
                usbUtil.sendData(init);
                usbUtil.sendData(data);

//                File file = new File(mContext.getFilesDir(), "123.bin");
//                byte[] data = null;
//                try {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        data = Files.readAllBytes(file.toPath());
//                    }else{
//                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                        try (InputStream is = new FileInputStream(file)) {
//                            byte[] buffer = new byte[1024];
//                            int read;
//                            while ((read = is.read(buffer)) != -1) {
//                                baos.write(buffer, 0, read);
//                            }
//                        }
//                        data = baos.toByteArray();
//                    }
//                    if (data != null && data.length > 0) {
//                        // 截取最后 200 字节，如果不足 200，则从头开始
//                        int start = Math.max(0, data.length - 200);
//                        byte[] tail = Arrays.copyOfRange(data, start, data.length);
//                        // 打印十六进制
//                        StringBuilder sb = new StringBuilder();
//                        for (byte b : tail) {
//                            sb.append(String.format("%02X ", b));
//                        }
//                        Log.i("UsbPrinter", "123.bin 最后 200 字节: " + sb.toString());
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                usbUtil.sendData(data);

                Log.i(TAG, "打印完成");
            } catch (Exception e) {
                Log.e(TAG, "打印异常", e);
            }
        }).start();
    }

    // 打印扩展文本
    public void doPrintTextEx(byte[] data) {
        if (!usbUtil.isConnected()) {
            Log.e(TAG, "USB未连接，无法打印");
            return;
        }
        new Thread(() -> {
            try {
                usbUtil.sendData(data);
                Log.i(TAG, "打印完成");
            } catch (Exception e) {
                Log.e(TAG, "打印异常", e);
            }
        }).start();
    }

    public byte[] printlabel(int quantity, int copy) {
        String message = "";
        message = "PRINT " + quantity + ", " + copy + "\r\n";
        return message.getBytes();
    }

    // 绘图方法
    public void tspl_drawGraphic(int start_x, int start_y, Bitmap bmp) {
        int bmp_size_x = bmp.getWidth();
        int bmp_size_y = bmp.getHeight();
        int byteWidth = (bmp_size_x - 1) / 8 + 1;
        if (byteWidth <= 0 || bmp_size_y <= 0) return;

        int unitHeight = 2048 / byteWidth;
        int unitCount = (bmp_size_y - 1) / unitHeight + 1;
        int startY, endY;
        byte[] dataByte;
        int color, A, R, G, B;

        for (int n = 0; n < unitCount; n++) {
            startY = n * unitHeight;
            endY = Math.min((startY + unitHeight), bmp_size_y);
            int byteLen = (endY - startY) * byteWidth;
            dataByte = new byte[byteLen];

            for (int y = startY; y < endY; y++) {
                for (int x = 0; x < bmp_size_x; x++) {
                    color = bmp.getPixel(x, y);
                    A = color >>> 24;
                    R = color >>> 16 & 0xFF;
                    G = color >>> 8 & 0xFF;
                    B = color & 0xFF;
                    if (A == 0 || R * 0.3 + G * 0.59 + B * 0.11 > 127) {
                        dataByte[(y - startY) * byteWidth + x / 8] |= (byte) (0x80 >> (x % 8));
                    }
                }
            }

            int curUnitHeight = n == unitCount - 1 ? (bmp_size_y - (n * unitHeight)) : unitHeight;
            String cmdHeader = "BITMAP " + start_x + "," + (start_y + startY) + "," + byteWidth + "," + curUnitHeight + ",0,";
            usbUtil.sendData(cmdHeader.getBytes(StandardCharsets.UTF_8));
            usbUtil.sendData(dataByte);
        }
    }

    private Bitmap getImageFromAssetsFile(String fileName) {
        try (InputStream is = mContext.getAssets().open(fileName)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "加载图片失败", e);
            return null;
        }
    }
    // 打印条码
    public boolean doPrintBarcode(String code, String type, int height) {
        if (!usbUtil.isConnected()) {
            Log.e(TAG, "USB未连接，无法打印条码");
            return false;
        }
        try {
            // 这里只实现TSC模式下的常见条码命令
            String cmd = "BARCODE 50,50,\"" + type + "\",80,1,0,2,2,\"" + code + "\"\n";
            sendCommand("CLS\n");
            sendCommand(cmd);
            sendCommand("PRINT 1,1\n");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "打印条码异常", e);
            return false;
        }
    }

    // 打印二维码
    public boolean doPrintQRCode(String content, int size) {
        if (!usbUtil.isConnected()) {
            Log.e(TAG, "USB未连接，无法打印二维码");
            return false;
        }
        try {
            String cmd = "QRCODE 50,50,H," + size + ",A,0,\"" + content + "\"\n";
            sendCommand("CLS\n");
            sendCommand(cmd);
            sendCommand("PRINT 1,1\n");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "打印二维码异常", e);
            return false;
        }
    }

    // 打印图片（假设imagePath为assets路径）
    public boolean doPrintImage(String imagePath) {
        if (!usbUtil.isConnected()) {
            Log.e(TAG, "USB未连接，无法打印图片");
            return false;
        }
        try {
            Bitmap bitmap = getImageFromAssetsFile(imagePath);
            if (bitmap == null) {
                Log.e(TAG, "图片加载失败: " + imagePath);
                return false;
            }
            sendCommand("CLS\n");
            tspl_drawGraphic(50, 50, bitmap);
            sendCommand("PRINT 1,1\n");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "打印图片异常", e);
            return false;
        }
    }

    // 切纸（ESC模式常见命令，TSC部分机型支持）
    public boolean doCutPaper() {
        if (!usbUtil.isConnected()) {
            Log.e(TAG, "USB未连接，无法切纸");
            return false;
        }
        try {
            // ESC/POS常用切纸命令
            byte[] cut = new byte[]{0x1D, 0x56, 0x00};
            usbUtil.sendData(cut);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "切纸异常", e);
            return false;
        }
    }

    // 进纸
    public boolean doFeedPaper(int lines) {
        if (!usbUtil.isConnected()) {
            Log.e(TAG, "USB未连接，无法进纸");
            return false;
        }
        try {
            // ESC/POS进纸命令
            byte[] feed = new byte[]{0x1B, 0x64, (byte) lines};
            usbUtil.sendData(feed);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "进纸异常", e);
            return false;
        }
    }

    // 获取打印机状态（简单实现，返回true表示连接）
    public boolean getPrinterStatus() {
        return usbUtil.isConnected();
    }
}