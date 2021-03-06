package com.thbyg.wxautoreply;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.OutputStream;

import static android.content.ContentValues.TAG;


/**
 * Created by myxiang on 2017/10/5.

 /**
 * 用root权限执行Linux下的Shell指令
 */

public class RootShellCmd {
    private static OutputStream os;
    private static final String TAG = "RootShellCmd";
    public void RootShellCmd() {
        LogToFile.init(AppContext.getContext());
    }
    /**
     * 执行shell指令 * * @param cmd * 指令
     */
    public static void exec(String cmd) {
        try {
            if (os == null) {
                os = Runtime.getRuntime().exec("su").getOutputStream();
            }
            os.write(cmd.getBytes());
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 执行shell命令
     *
     * @param cmd
     */
    public static void execShellCmd(String cmd) {

        try {
            if (isRooted()) {
                LogToFile.write("已Root");
            }
            else {
                // 申请获取root权限，这一步很重要，不然会没有作用
                LogToFile.write("正在获取ROOT权限");
                Process process = Runtime.getRuntime().exec("su");
            }
            // 获取输出流
            OutputStream outputStream = Runtime.getRuntime().exec("su").getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(
                    outputStream);
            dataOutputStream.writeBytes(cmd);
            dataOutputStream.flush();
            dataOutputStream.close();
            outputStream.close();
            LogToFile.write("cmd=" + cmd + " 执行完毕！");
        } catch (Exception e) {
            LogToFile.write("execShellCmd Fail!Error=" + e.getMessage());
        }
    }
    /**
     * 通过keyCode模拟点击
     *
     * @param keyCode
     */
    public static void simulateKey(int keyCode) {
        exec("input keyevent " + keyCode + "\n");
    }

    /**
     * 通过坐标模拟点击
     *
     * @param x
     * @param y
     */
    public static void simulateKey(int x, int y) {
        //利用ProcessBuilder执行shell命令//利用ProcessBuilder执行shell命令
        exec("input tap " + x + " " + y + "\n");
    }

    private static boolean isRooted() {
        return findBinary("su");
    }

    public static boolean findBinary(String binaryName) {
        boolean found = false;
        if (!found) {
            String[] places = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
                    "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
            for (String where : places) {
                Log.d(TAG, "DATA where = " + where);
                if (new File(where + binaryName).exists()) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }
}
