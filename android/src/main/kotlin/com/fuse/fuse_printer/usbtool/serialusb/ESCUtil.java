package com.fuse.fuse_printer.usbtool.serialusb;

/**
 * 作者：CaoLiulang
 * ❤
 * Date：2022/5/30
 * ❤
 * 模块：ESC指令工具类
 */
public class ESCUtil {

    // ASCII码定义
    public static final byte ESC = 0x1B; // 转义字符
    public static final byte FS = 0x1C; // 文件分隔符
    public static final byte GS = 0x1D; // 组分隔符
    public static final byte LF = 0x0A; // 换行
    public static final byte CR = 0x0D; // 回车
    public static final byte FF = 0x0C; // 换页
    public static final byte CANCEL = 0x18; // 取消
    public static final byte DLE = 0x10; // 数据链路转义
    public static final byte NUL = 0x00; // 空字符
    public static final byte BEL = 0x07; // 响铃
    public static final byte DC1 = 0x11; // 设备控制1
    public static final byte DC2 = 0x12; // 设备控制2
    public static final byte DC3 = 0x13; // 设备控制3
    public static final byte DC4 = 0x14; // 设备控制4
    public static final byte STX = 0x02; // 正文开始
    public static final byte ETX = 0x03; // 正文结束
    public static final byte EOT = 0x04; // 传输结束
    public static final byte ENQ = 0x05; // 询问字符
    public static final byte ACK = 0x06; // 确认字符
    public static final byte SO = 0x0E; // 移位输出
    public static final byte SI = 0x0F; // 移位输入
    public static final byte DEL = 0x7F; // 删除
    public static final byte SP = 0x20; // 空格
    public static final byte HT = 0x09; // 水平制表符
    public static final byte VT = 0x0B; // 垂直制表符

    /**
     * 打印机初始化
     */
    public static byte[] init_printer() {
        byte[] result = {(byte) 0x1b, (byte) 0x40};
        return result;
    }

    /**
     * 设置打印浓度
     * level 打印浓度，范围0-255，默认127
     */
    public static byte[] setPrintDensity(int level) {
        if (level < 0) {
            level = 0;
        } else if (level > 255) {
            level = 255;
        }
        byte[] result = {(byte) 0x1d, (byte) 0x7a, (byte) level};
        return result;
    }
}