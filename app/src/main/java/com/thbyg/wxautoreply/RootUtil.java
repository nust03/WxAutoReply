package com.thbyg.wxautoreply;
import android.content.Context;

import java.io.File;
/**
 * Created by myxiang on 2017/10/5.

 * root权限判断
 */
public class RootUtil  {
    private static final String TAG = "RootUtil";
    public void RootUtil() {
        LogToFile.init(AppContext.getContext());
    }
    // 获取ROOT权限
    public static void get_root() {

        if (is_root()) {
            LogToFile.write("已Root");
        } else {
            try {
                LogToFile.write("正在获取ROOT权限");
                Runtime.getRuntime().exec("su");
            } catch (Exception e) {
                LogToFile.write("获取ROOT权限时出错");
            }
        }

    }

    // 判断是否具有ROOT权限
    public static boolean is_root() {
        boolean res = false;
        try {
            if ((!new File("/system/bin/su").exists()) &&
                    (!new File("/system/xbin/su").exists())) {
                res = false;
            } else {
                res = true;
            }
        } catch (Exception e) {

        }
        return res;
    }
}
