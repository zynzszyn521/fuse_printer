package com.fuse.fuse_printer.usbtool.usbprinter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;

import java.io.IOException;

/**
 * 作者：CaoLiulang
 * ❤
 * Date：2022/5/30
 * ❤
 * 模块：TSC指令工具类
 */
public class TSCUtils {

    private static final String TAG = "TSCUtils";

    // TSC指令常量
    public static final byte ESC = 0x1B;
    public static final byte LF = 0x0A;
    public static final byte CR = 0x0D;
    public static final byte SPACE = 0x20;
    public static final byte FS = 0x1C;
    public static final byte GS = 0x1D;
    public static final byte HT = 0x09;

    /**
     * 初始化打印机
     */
    public static byte[] initPrinter() {
        String command = "INIT" + CR + LF;
        return command.getBytes();
    }

    /**
     * 设置纸张大小
     * width 标签宽度(mm)
     * height 标签高度(mm)
     */
    public static byte[] setLabelSize(int width, int height) {
        String command = "SIZE " + width + "," + height + CR + LF;
        return command.getBytes();
    }

    /**
     * 设置纸张偏移量
     * offset 偏移量(mm)
     */
    public static byte[] setGap(int offset) {
        String command = "GAP " + offset + ",0" + CR + LF;
        return command.getBytes();
    }

    /**
     * 设置打印方向
     * direction 0:正向 1:反向
     */
    public static byte[] setDirection(int direction) {
        String command = "DIRECTION " + direction + CR + LF;
        return command.getBytes();
    }

    /**
     * 设置打印速度
     * speed 打印速度(1-6)
     */
    public static byte[] setPrintSpeed(int speed) {
        String command = "SPEED " + speed + CR + LF;
        return command.getBytes();
    }

    /**
     * 设置打印浓度
     * density 打印浓度(0-15)
     */
    public static byte[] setPrintDensity(int density) {
        String command = "DENSITY " + density + CR + LF;
        return command.getBytes();
    }

    /**
     * 设置参考点
     * x 参考点x坐标
     * y 参考点y坐标
     */
    public static byte[] setReference(int x, int y) {
        String command = "REFERENCE " + x + "," + y + CR + LF;
        return command.getBytes();
    }

    /**
     * 设置撕纸位置
     * position 撕纸位置
     */
    public static byte[] setTearPosition(int position) {
        String command = "TEAR " + position + CR + LF;
        return command.getBytes();
    }

    /**
     * 清除图像缓冲区
     */
    public static byte[] clearBuffer() {
        String command = "CLS" + CR + LF;
        return command.getBytes();
    }

    /**
     * 打印文本
     * x 文本x坐标
     * y 文本y坐标
     * font 字体名称(1-8)
     * rotation 旋转角度(0,90,180,270)
     * x_multi 水平放大倍数(1-10)
     * y_multi 垂直放大倍数(1-10)
     * content 文本内容
     */
    public static byte[] printText(int x, int y, int font, int rotation, int x_multi, int y_multi, String content) {
        String command = "TEXT " + x + "," + y + ",\"TSS24.BF2\"," + rotation + "," + x_multi + "," + y_multi + ",\"" + content + "\"" + CR + LF;
        return command.getBytes();
    }

    /**
     * 打印一维条码
     * x 条码x坐标
     * y 条码y坐标
     * type 条码类型
     * height 条码高度
     * readable 可读性(0:不可读 1:可读)
     * rotation 旋转角度(0,90,180,270)
     * narrow 窄条宽度
     * wide 宽条宽度
     * content 条码内容
     */
    public static byte[] printBarcode(int x, int y, String type, int height, int readable, int rotation, int narrow, int wide, String content) {
        String command = "BARCODE " + x + "," + y + ",\"" + type + "\"," + height + "," + readable + "," + rotation + "," + narrow + "," + wide + ",\"" + content + "\"" + CR + LF;
        return command.getBytes();
    }

    /**
     * 打印二维码
     * x 二维码x坐标
     * y 二维码y坐标
     * level 纠错级别(L,M,Q,H)
     * cell_width 单元格宽度
     * rotation 旋转角度(0,90,180,270)
     * content 二维码内容
     */
    public static byte[] printQRCode(int x, int y, String level, int cell_width, int rotation, String content) {
        String command = "QRCODE " + x + "," + y + ",\"" + level + "\"," + cell_width + "," + rotation + ",\"" + content + "\"" + CR + LF;
        return command.getBytes();
    }

    /**
     * 打印图像
     * x 图像x坐标
     * y 图像y坐标
     * width 图像宽度
     * height 图像高度
     * bitmap 位图数据
     */
    public static byte[] printImage(int x, int y, int width, int height, Bitmap bitmap) {
        try {
            // 将位图转换为黑白
            Bitmap grayBitmap = convertToGrayscale(bitmap);
            // 将位图转换为TSC格式
            byte[] imageData = convertBitmapToTSCData(grayBitmap, width, height);
            // 构造打印图像命令
            String command = "BITMAP " + x + "," + y + "," + (width / 8) + "," + height + ",1,";
            return concatBytes(command.getBytes(), imageData, new byte[]{CR, LF});
        } catch (Exception e) {
            Log.e(TAG, "转换图像失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置打印数量
     * copies 打印份数
     */
    public static byte[] setPrintCopies(int copies) {
        String command = "PRINT " + copies + ",1" + CR + LF;
        return command.getBytes();
    }

    /**
     * 进纸
     */
    public static byte[] feedPaper() {
        String command = "FEED" + CR + LF;
        return command.getBytes();
    }

    /**
     * 回纸
     */
    public static byte[] backFeedPaper() {
        String command = "BACKFEED" + CR + LF;
        return command.getBytes();
    }

    /**
     * 切断纸张
     */
    public static byte[] cutPaper() {
        String command = "CUT" + CR + LF;
        return command.getBytes();
    }

    /**
     * 将位图转换为灰度图
     */
    private static Bitmap convertToGrayscale(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        Bitmap grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayscaleBitmap);
        Paint paint = new Paint();
        
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorFilter);
        
        canvas.drawBitmap(bitmap, 0, 0, paint);
        
        return grayscaleBitmap;
    }

    /**
     * 将位图转换为TSC格式数据
     */
    private static byte[] convertBitmapToTSCData(Bitmap bitmap, int width, int height) throws IOException {
        // 调整位图大小
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        
        int bytesPerLine = (width + 7) / 8; // 每8个像素为一个字节
        byte[] imageData = new byte[bytesPerLine * height];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = resizedBitmap.getPixel(x, y);
                int gray = (pixel >> 16 & 0xff + pixel >> 8 & 0xff + pixel & 0xff) / 3;
                
                // 黑点(0)，白点(1)
                if (gray < 128) {
                    int bitIndex = x % 8;
                    int byteIndex = (y * bytesPerLine) + (x / 8);
                    imageData[byteIndex] |= (1 << (7 - bitIndex));
                }
            }
        }
        
        return imageData;
    }

    /**
     * 合并多个字节数组
     */
    private static byte[] concatBytes(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }
        
        byte[] result = new byte[totalLength];
        int currentIndex = 0;
        
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, currentIndex, array.length);
            currentIndex += array.length;
        }
        
        return result;
    }

    /**
     * 发送TSC命令到打印机
     * usbUtil USB工具类实例
     * commands 命令数组
     */
    public static boolean sendTSCCommands(USBUtil usbUtil, byte[]... commands) {
        if (usbUtil == null || !usbUtil.isConnected()) {
            Log.e(TAG, "USB设备未连接");
            return false;
        }
        
        try {
            for (byte[] command : commands) {
                if (command != null) {
                    boolean success = usbUtil.sendData(command);
                    if (!success) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "发送TSC命令失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}