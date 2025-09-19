package com.fuse.fuse_printer.usbtool.usbprinter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * 作者：CaoLiulang
 * ❤
 * Date：2022/5/30
 * ❤
 * 模块：USB广播接收器
 */
public class USBReceiver extends BroadcastReceiver {

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String TAG = "USBReceiver";
    private USBStateListener mListener;

    public interface USBStateListener {
        void onDeviceAttached(UsbDevice device);
        void onDeviceDetached(UsbDevice device);
        void onPermissionGranted(UsbDevice device);
        void onPermissionDenied(UsbDevice device);
    }

    public USBReceiver(USBStateListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (action == null || device == null) {
            return;
        }

        Log.i(TAG, "接收到USB广播：" + action + ", 设备：" + device.getDeviceName());

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            // USB设备连接
            if (mListener != null) {
                mListener.onDeviceAttached(device);
            }
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            // USB设备断开
            if (mListener != null) {
                mListener.onDeviceDetached(device);
            }
            } else if (ACTION_USB_PERMISSION.equals(action)) {
            // USB权限请求结果
            boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
            if (granted) {
                // 权限被授予
                if (mListener != null) {
                    mListener.onPermissionGranted(device);
                }
            } else {
                // 权限被拒绝
                if (mListener != null) {
                    mListener.onPermissionDenied(device);
                }
            }
        }
    }

    /**
     * 设置USB状态监听器
     */
    public void setUSBStateListener(USBStateListener listener) {
        this.mListener = listener;
    }
}