package com.fuse.fuse_printer.usbtool.usbprinter;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 作者：CaoLiulang
 * ❤
 * Date：2022/5/30
 * ❤
 * 模块：USB设备工具类
 */
public class USBUtil {

    private static final String TAG = "USBUtil";
    private static USBUtil instance;
    private UsbManager mUsbManager;
    private Context mContext;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbEndpoint mUsbEndpointIn;
    private UsbEndpoint mUsbEndpointOut;
    private UsbDevice mUsbDevice;
    private UsbInterface mUsbInterface;

    private USBUtil(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static synchronized USBUtil getInstance(Context context) {
        if (instance == null) {
            instance = new USBUtil(context);
        }
        return instance;
    }

    /**
     * 初始化USB设备
     */
    public boolean initUsbDevice(int vendorId, int productId) {
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        // 寻找USB设备
        mUsbDevice = findUsbDevice(vendorId, productId);
        if (mUsbDevice == null) {
            Log.e(TAG, "未找到USB设备: vendorId=" + vendorId + ", productId=" + productId);
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
    private UsbDevice findUsbDevice(int vendorId, int productId) {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            int devVendorId = device.getVendorId();
            int devProductId = device.getProductId();
            Log.i(TAG, "找到USB设备：vendorId=" + devVendorId + ", productId=" + devProductId + ", deviceName=" + device.getDeviceName());

            // 这里可以根据需要过滤特定的设备
            if (devVendorId == vendorId && devProductId == productId) {
                return device;
            }
        }
        return null;
    }

    /**
     * 查找USB设备（仅根据vendorId）
     */
    public UsbDevice findUsbDeviceByVendorId(int vendorId) {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            int devVendorId = device.getVendorId();
            int devProductId = device.getProductId();
            Log.i(TAG, "找到USB设备：vendorId=" + devVendorId + ", productId=" + devProductId + ", deviceName=" + device.getDeviceName());

            // 这里可以根据需要过滤特定的设备
            if (devVendorId == vendorId) {
                return device;
            }
        }
        return null;
    }

    /**
     * 获取所有USB设备列表
     */
    public List<UsbDevice> getAllUsbDevices() {
        List<UsbDevice> deviceList = new ArrayList<>();
        HashMap<String, UsbDevice> devices = mUsbManager.getDeviceList();
        if (devices != null && !devices.isEmpty()) {
            deviceList.addAll(devices.values());
        }
        return deviceList;
    }

    /**
     * 发送数据到USB设备
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
     * 从USB设备接收数据
     */
    public byte[] receiveData(int length) {
        if (length <= 0) {
            return null;
        }

        if (mUsbDeviceConnection == null || mUsbEndpointIn == null) {
            Log.e(TAG, "USB设备未连接或初始化失败");
            return null;
        }

        try {
            byte[] buffer = new byte[length];
            int ret = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, buffer, buffer.length, 3000);
            if (ret < 0) {
                Log.e(TAG, "接收数据失败：" + ret);
                return null;
            }
            // 如果实际接收的字节数小于请求的字节数，截取数组
            if (ret < buffer.length) {
                byte[] actualData = new byte[ret];
                System.arraycopy(buffer, 0, actualData, 0, ret);
                return actualData;
            }
            return buffer;
        } catch (Exception e) {
            Log.e(TAG, "接收数据异常：" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 关闭USB连接
     */
    public void close() {
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
     * 获取USB接口信息
     */
    public UsbInterface getUsbInterface() {
        return mUsbInterface;
    }

    /**
     * 获取USB设备连接对象
     */
    public UsbDeviceConnection getUsbDeviceConnection() {
        return mUsbDeviceConnection;
    }
}