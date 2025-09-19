package com.fuse.fuse_printer.usbtool;

import android.util.Log;

import com.aill.androidserialport.ByteUtil;
import com.aill.androidserialport.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialManager {
    //串口命令示例
//    private final String CMD_LIGHT_OPEN = "6a610111";
//    private final String CMD_LIGHT_CLOSE = "6a610110";

    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;

    private boolean isOpened = false;

    private static SerialManager INSTANCE = null;

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

    /**
     * 打开补光灯
     */
//    public void openLight() {
//        openSerial();
//        send(CMD_LIGHT_OPEN);
//        isOpened = true;
//    }

    /**
     * 关闭补光灯
     */
//    public void closeLight() {
//        openSerial();
//        send(CMD_LIGHT_CLOSE);
//        isOpened = false;
//    }

    public boolean isOpened() {
        return isOpened;
    }

    private void init() {
        if (serialPort != null) {
            return;
        }
        try {
            //串口文件(根据设备)、波特率
            serialPort = new SerialPort(new File("/dev/ttyS1"), 9600, 0);
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {
            Log.e("8888888888888888888", "fail");
            e.printStackTrace();
        }
    }

    private void send(String s) {
        try {
            byte[] data = ByteUtil.hexStringToByteArray(s);
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            if (serialPort != null) {
                serialPort.close();
                serialPort = null;
            }
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}