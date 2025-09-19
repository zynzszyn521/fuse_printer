package com.fuse.fuse_printer.usbtool;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.fuse.fuse_printer.usbtool.usbprinter.USBUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class USBCommunicationPlugin {

    private static final String TAG = USBCommunicationPlugin.class.getSimpleName();

    // 设备信息（示例：需根据实际设备修改）
    private static final int VENDOR_ID = 19267;
    private static final int PRODUCT_ID = 13624;

    private Context mContext;
    private USBStateListener mUSBStateListener;

    private boolean isTsc = true;   // true: TSC, false: ESC

    private USBUtil usbUtil;
    private UsbDevice mUsbDevice;

    private Timer timer;
    private boolean isAutoConnecting = false;

    // 时间格式
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Handler 改为静态内部类，避免内存泄漏
    private static class SafeHandler extends Handler {
        private final WeakReference<USBCommunicationPlugin> pluginRef;

        public SafeHandler(USBCommunicationPlugin plugin) {
            super(Looper.getMainLooper());
            this.pluginRef = new WeakReference<>(plugin);
        }

        @Override
        public void handleMessage(Message msg) {
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

    public void init(Context context, boolean isTSC) {
        this.mContext = context.getApplicationContext();
        this.isTsc = isTSC;

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
        if (usbUtil.isConnected()) {
            mHandler.obtainMessage(USB_STATE_CHANGED, true).sendToTarget();
            return;
        }

        // 查找设备
        mUsbDevice = usbUtil.findUsbDevice(VENDOR_ID, PRODUCT_ID);
        if (mUsbDevice == null) {
            Log.w(TAG, "未找到匹配的USB设备");
            mHandler.obtainMessage(USB_STATE_CHANGED, false).sendToTarget();
            return;
        }

        // 请求权限或初始化
        if (!usbUtil.initUsbDevice(VENDOR_ID, PRODUCT_ID)) {
            // 需要请求权限
            Log.i(TAG, "请求USB权限...");
            ((UsbManager) mContext.getSystemService(Context.USB_SERVICE))
                    .requestPermission(mUsbDevice, PendingIntent.getBroadcast(
                            mContext, 0,
                            new Intent(USBUtil.getInstance(mContext).getPermissionAction()),
                            PendingIntent.FLAG_MUTABLE
                    ));
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
        mContext.registerReceiver(mUsbReceiver, filter);
    }

    public void unRegisterUSBStateReceiver() {
        try {
            mContext.unregisterReceiver(mUsbReceiver);
        } catch (Exception e) {
            Log.e(TAG, "unregisterReceiver failed", e);
        }
    }

    // 打印方法
    public void doPrintUsbTsc(String weight) {
        if (!usbUtil.isConnected()) {
            Log.e(TAG, "USB未连接，无法打印");
            return;
        }

        new Thread(() -> {
            try {
                // 发送初始化命令
                sendCommand("SIZE 54 mm,38 mm\n");
                sendCommand("GAP 2 mm,0 mm\n");
                sendCommand("DIRECTION 1\n");
                sendCommand("CLS\n");

                // 矩形框
                sendCommand("BOX 0,0,360,50,2\n");

                // 图片
                Bitmap bitmap = getImageFromAssetsFile("image/ic_jk.png");
                if (bitmap != null) {
                    tspl_drawGraphic(130, 100, bitmap);
                }

                // 文本
                sendCommand("TEXT 2,10,\"3\",0,1.2,1.2,\"健坤小助手\"\n");
                sendCommand("BAR 0,75,380,3\n");
                sendCommand("TEXT 2,90,\"4\",0,1,1,\"科室：测试\"\n");
                sendCommand("TEXT 2,120,\"2\",0,1,1,\"车号:测试\"\n");
                sendCommand("TEXT 2,150,\"2\",0,1,1,\"类型:测试\"\n");
                sendCommand("TEXT 2,180,\"2\",0,1,1,\"重量:" + weight + "\"\n");
                sendCommand("TEXT 2,210,\"2\",0,1,1,\"护士:测试\"\n");
                sendCommand("TEXT 2,240,\"2\",0,1,1,\"时间:" + df.format(new Date()) + "\"\n");

                // 二维码
                sendCommand("QRCODE 270,110,H,1,M,0,\"二维码内容\"\n");

                // 打印标签
                usbUtil.sendData("PRINT 1,1\n".getBytes(StandardCharsets.UTF_8));

                Log.i(TAG, "打印完成");

            } catch (Exception e) {
                Log.e(TAG, "打印异常", e);
            }
        }).start();
    }

    private void sendCommand(String cmd) {
        usbUtil.sendData(cmd.getBytes(StandardCharsets.UTF_8));
    }

    // 绘图方法（保持不变）
    public boolean tspl_drawGraphic(int start_x, int start_y, Bitmap bmp) {
        int bmp_size_x = bmp.getWidth();
        int bmp_size_y = bmp.getHeight();
        int byteWidth = (bmp_size_x - 1) / 8 + 1;
        int byteHeight = bmp_size_y;
        if (byteWidth <= 0 || byteHeight <= 0) return false;

        int unitHeight = 2048 / byteWidth;
        int unitCount = (byteHeight - 1) / unitHeight + 1;
        int startY, endY;
        byte[] dataByte;
        int color, A, R, G, B;

        for (int n = 0; n < unitCount; n++) {
            startY = n * unitHeight;
            endY = (startY + unitHeight) > bmp_size_y ? bmp_size_y : startY + unitHeight;
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
                        dataByte[(y - startY) * byteWidth + x / 8] |= (0x80 >> (x % 8));
                    }
                }
            }

            int curUnitHeight = n == unitCount - 1 ? (byteHeight - (n * unitHeight)) : unitHeight;
            String cmdHeader = "BITMAP " + start_x + "," + (start_y + startY) + "," + byteWidth + "," + curUnitHeight + ",0,";
            usbUtil.sendData(cmdHeader.getBytes(StandardCharsets.UTF_8));
            usbUtil.sendData(dataByte);
        }
        return true;
    }

    private Bitmap getImageFromAssetsFile(String fileName) {
        try (InputStream is = mContext.getAssets().open(fileName)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "加载图片失败", e);
            return null;
        }
    }
}