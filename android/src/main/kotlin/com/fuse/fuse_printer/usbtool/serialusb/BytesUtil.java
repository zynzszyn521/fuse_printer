package com.fuse.fuse_printer.usbtool.serialusb;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Hashtable;

/**
 * 作者：CaoLiulang
 * ❤
 * Date：2022/5/30
 * ❤
 * 模块：BytesUtil工具类
 */
public class BytesUtil {

    /**
     * 字节流转16进制字符串
     */
    public static String getHexStringFromBytes(byte[] data) {
        if (data == null || data.length <= 0) {
            return null;
        }
        String hexString = "0123456789ABCDEF";
        int size = data.length * 2;
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < data.length; i++) {
            sb.append(hexString.charAt((data[i] & 0xF0) >> 4));
            sb.append(hexString.charAt((data[i] & 0x0F) >> 0));
        }
        return sb.toString();
    }

    /**
     * 单字符转字节
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 16进制字符串转字节数组
     */
    @SuppressLint("DefaultLocale")
    public static byte[] getBytesFromHexString(String hexString) {
        if (hexString == null || "".equals(hexString)) {
            return null;
        }
        hexString = hexString.replace(" ", "");
        hexString = hexString.toUpperCase();
        int size = hexString.length() / 2;
        char[] hexArray = hexString.toCharArray();
        byte[] rv = new byte[size];
        for (int i = 0; i < size; i++) {
            int pos = i * 2;
            rv[i] = (byte) (charToByte(hexArray[pos]) << 4 | charToByte(hexArray[pos + 1]));
        }
        return rv;
    }

    /**
     * 十进制字符串转字节数组
     */
    @SuppressLint("DefaultLocale")
    public static byte[] getBytesFromDecString(String decString) {
        if (decString == null || "".equals(decString)) {
            return null;
        }
        decString = decString.replace(" ", "");
        int size = decString.length() / 2;
        char[] decarray = decString.toCharArray();
        byte[] rv = new byte[size];
        for (int i = 0; i < size; i++) {
            int pos = i * 2;
            rv[i] = (byte) (charToByte(decarray[pos]) * 10 + charToByte(decarray[pos + 1]));
        }
        return rv;
    }

    /**
     * 字节数组组合操作1
     */
    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }


    /**
     * 字节数组组合操作2
     */
    public static byte[] byteMerger(byte[][] byteList) {
        if (byteList == null || byteList.length == 0) {
            return null;
        }
        int length = 0;
        for (byte[] bytes : byteList) {
            if (bytes != null) {
                length += bytes.length;
            }
        }
        byte[] result = new byte[length];
        int index = 0;
        for (byte[] bytes : byteList) {
            if (bytes != null) {
                System.arraycopy(bytes, 0, result, index, bytes.length);
                index += bytes.length;
            }
        }
        return result;
    }

    /**
     * 字符转字节
     */
    public static byte[] chars2Bytes(char[] chars) {
        byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }

    /**
     * 生成QRCode二维码
     */
    public static Bitmap createQRCode(String content, int width, int height) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        Hashtable<EncodeHintType, String> hints = new Hashtable<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitMatrix.get(x, y)) {
                    pixels[y * width + x] = 0xff000000;
                } else {
                    pixels[y * width + x] = 0xffffffff;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /**
     * 将数据转为十六进制字符串
     */
    public static String byte2hex(byte[] buffer) {
        String h = "";
        for (int i = 0; i < buffer.length; i++) {
            String temp = Integer.toHexString(buffer[i] & 0xFF);
            if (temp.length() == 1) {
                temp = "0" + temp;
            }
            h = h + temp;
        }
        return h;
    }

    /**
     * 十六进制字符串转ASCII字符串
     */
    public static String hex2ASCII(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            sb.append((char) Integer.parseInt(str, 16));
        }
        return sb.toString();
    }

    /**
     * 数据转换
     */
    public static byte[] str2bytes(String str) {
        if (str == null || str.length() <= 0) {
            return null;
        }
        int size = str.length();
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) str.charAt(i);
        }
        return data;
    }

    /**
     * 转字节数组
     */
    public static byte[] toBytes(String str) {
        if (str == null) {
            return null;
        }
        return str.getBytes();
    }

    /**
     * 字节数组转十六进制字符串
     */
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * 字符串转十六进制字符串
     */
    public static String stringToHexString(String strPart) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < strPart.length(); i++) {
            int ch = (int) strPart.charAt(i);
            String strHex = Integer.toHexString(ch);
            hexString.append(strHex);
        }
        return hexString.toString();
    }

    /**
     * 十六进制字符串转字节数组
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * 截取字节数组
     */
    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

    /**
     * 复制字节数组
     */
    public static byte[] copyBytes(byte[] src) {
        if (src == null) {
            return null;
        }
        int len = src.length;
        byte[] bs = new byte[len];
        System.arraycopy(src, 0, bs, 0, len);
        return bs;
    }

    /**
     * 复制数组
     */
    public static void copyArray(byte[] src, byte[] dest, int srcPos, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
    }

    /**
     * 将整数转成字节数组
     */
    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

    /**
     * 将字节数组转成整数
     */
    public static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (bytes[i] & 0x000000FF) << shift;
        }
        return value;
    }

    /**
     * 把16进制字符串转换成字节数组
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}