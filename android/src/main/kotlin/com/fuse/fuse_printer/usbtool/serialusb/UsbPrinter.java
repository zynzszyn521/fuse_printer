package com.fuse.fuse_printer.usbtool.serialusb;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 作者：CaoLiulang
 * ❤
 * Date：2022/5/30
 * ❤
 * 模块：USB打印机管理类
 */
public class UsbPrinter {

    private static final String TAG = "UsbPrinter";
    private static UsbPrinter instance;
    private UsbManager mUsbManager;
    private Context mContext;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbEndpoint mUsbEndpointIn;
    private UsbEndpoint mUsbEndpointOut;
    private UsbDevice mUsbDevice;
    private UsbInterface mUsbInterface;
    private ConcurrentLinkedQueue<byte[]> mDataQueue;
    private boolean mIsPrinting = false;
    private boolean mIsRunning = false;
    private static final int VENDOR_ID = 0x4B55;
    private static final int PRODUCT_ID = 0x3558;

    private UsbPrinter(Context context) {
        this.mContext = context.getApplicationContext();
        this.mDataQueue = new ConcurrentLinkedQueue<>();
    }

    public static synchronized UsbPrinter getInstance(Context context) {
        if (instance == null) {
            instance = new UsbPrinter(context);
        }
        return instance;
    }

    /**
     * 初始化USB设备
     */
    public boolean initUsbDevice() {
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        // 寻找USB设备
        mUsbDevice = findUsbDevice();
        if (mUsbDevice == null) {
            Log.e(TAG, "未找到USB设备");
            return false;
        }

        // 获取USB接口
        mUsbInterface = mUsbDevice.getInterface(0);

        // 打开设备，获取 UsbDeviceConnection 对象，连接 USB 设备
        mUsbDeviceConnection = mUsbManager.openDevice(mUsbDevice);
        if (mUsbDeviceConnection == null) {
            // 未获取到USB权限，需要请求权限
            Log.e(TAG, "未获取到USB设备权限");
            return false;
        }

        // 打开接口
        if (!mUsbDeviceConnection.claimInterface(mUsbInterface, true)) {
            Log.e(TAG, "无法打开USB接口");
            return false;
        }

        // 寻找可用的端点
        for (int i = 0; i < mUsbInterface.getEndpointCount(); i++) {
            UsbEndpoint ep = mUsbInterface.getEndpoint(i);
            if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                mUsbEndpointIn = ep;
            } else {
                mUsbEndpointOut = ep;
            }
        }

        Log.i(TAG, "USB设备初始化成功");
        return true;
    }

    /**
     * 查找USB设备
     */
    private UsbDevice findUsbDevice() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            int vendorId = device.getVendorId();
            int productId = device.getProductId();
            Log.i(TAG, "找到USB设备：vendorId=" + vendorId + ", productId=" + productId + ", deviceName=" + device.getDeviceName());

            // 这里可以根据需要过滤特定的设备
            if (vendorId == VENDOR_ID && productId == PRODUCT_ID) {
                return device;
            }
        }
        return null;
    }

    /**
     * 发送数据到USB打印机
     */
    public boolean sendData(byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }

        if (mUsbDeviceConnection == null || mUsbEndpointOut == null) {
            Log.e(TAG, "USB设备未连接或初始化失败");
            return false;
        }

        try {
            int ret = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, data, data.length, 3000);
            if (ret < 0) {
                Log.e(TAG, "发送数据失败：" + ret);
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "发送数据异常：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 发送数据队列到USB打印机
     */
    public void sendDataQueue(byte[] data) {
        if (data != null && data.length > 0) {
            mDataQueue.add(data);
            if (!mIsRunning) {
                startPrintThread();
            }
        }
    }

    /**
     * 开始打印线程
     */
    private void startPrintThread() {
        mIsRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mIsRunning) {
                    if (mDataQueue.isEmpty()) {
                        mIsPrinting = false;
                        break;
                    }
                    byte[] data = mDataQueue.poll();
                    if (data != null && data.length > 0) {
                        mIsPrinting = true;
                        sendData(data);
                    }
                }
                mIsRunning = false;
            }
        }).start();
    }

    /**
     * 关闭USB连接
     */
    public void close() {
        mIsRunning = false;
        if (mUsbDeviceConnection != null) {
            mUsbDeviceConnection.close();
            mUsbDeviceConnection = null;
        }
        mUsbEndpointIn = null;
        mUsbEndpointOut = null;
        mUsbInterface = null;
        mUsbDevice = null;
    }

    /**
     * 检查USB连接状态
     */
    public boolean isConnected() {
        return mUsbDeviceConnection != null;
    }

    /**
     * 获取USB设备信息
     */
    public UsbDevice getUsbDevice() {
        return mUsbDevice;
    }

    /**
     * 检查是否正在打印
     */
    public boolean isPrinting() {
        return mIsPrinting;
    }

    /**
     * 清空打印队列
     */
    public void clearPrintQueue() {
        if (mDataQueue != null) {
            mDataQueue.clear();
        }
    }
}