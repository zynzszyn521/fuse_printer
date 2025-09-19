package com.fuse.fuse_printer.usbtool;

import android.util.Log;

import android.serialport.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.fuse.fuse_printer.usbtool.serialusb.BytesUtil;

public class SerialManager {

    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isOpened = false;

    private static volatile SerialManager INSTANCE = null;

    public static SerialManager getInstance() {
        if (INSTANCE == null) {
            synchronized (SerialManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SerialManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 打开串口
     */
    public void openSerial() {
        init();
    }

    /**
     * 关闭串口
     */
    public void closeSerial() {
        close();
    }

    public SerialPort getSerialPort() {
        return serialPort;
    }

    public boolean isOpened() {
        return isOpened;
    }

    private void init() {
        if (serialPort != null) {
            return;
        }
        try {
            // ✅ 使用 Builder 构建串口，这才是正确方式！
            serialPort = SerialPort.newBuilder(new File("/dev/ttyS1"), 9600)
                    .dataBits(8)      // 数据位：5/6/7/8
                    .parity(0)        // 校验位：0=无, 1=奇, 2=偶
                    .stopBits(1)      // 停止位：1 或 2
                    .build();         // 自动调用私有构造函数

            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            isOpened = true;
            Log.i("SerialManager", "串口打开成功");

        } catch (SecurityException e) {
            Log.e("SerialManager", "串口权限不足，请确保有读写权限", e);
            isOpened = false;
        } catch (IOException e) {
            Log.e("SerialManager", "打开串口失败", e);
            isOpened = false;
        }
    }

    private void send(String hex) {
        if (outputStream == null || !isOpened) {
            Log.e("SerialManager", "串口未打开，无法发送数据");
            return;
        }
        try {
            byte[] data = BytesUtil.hexStringToByteArray(hex);
            outputStream.write(data);
            outputStream.flush();
            Log.d("SerialManager", "发送数据: " + hex);
        } catch (IOException e) {
            Log.e("SerialManager", "发送数据失败", e);
        }
    }

    private void close() {
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            if (serialPort != null) {
                serialPort.close(); // 会自动关闭 fd 和流
                serialPort = null;
            }
            isOpened = false;
            Log.i("SerialManager", "串口已关闭");
        } catch (IOException e) {
            Log.e("SerialManager", "关闭串口异常", e);
        }
    }
}