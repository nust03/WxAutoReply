package com.thbyg.wxautoreply;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.util.List;
import java.util.Random;

import static java.lang.Thread.sleep;

/**常用工具类
 * Created by myxiang on 2017/10/9.
 */

public class FuncTools {
    public static String Wx_PackageName = "com.tencent.mm";
    public static void Delay(int second) {
        int delay = 10;
        int count = second / delay;
        for (int i = 0; i < count; i++) {
            try {
                sleep(delay);
            } catch (Exception e) {
                LogToFile.write("Error msg:" + e.getMessage());
            }
        }
        return;
    }

    //在一定范围内生成随机数.
    //比如此处要求在[0 - n)内生成随机数.
    //注意:包含0不包含n
    public static int getRandom(int max) {
        Random random = new Random();
        int intret_value = random.nextInt(max);
        return intret_value;
    }

    /**
     * 判断指定的应用是否在前台运行
     *
     * @param packageName
     * @return
     */
    public static  boolean isAppForeground(String packageName) {
        ActivityManager am = (ActivityManager) AppContext.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if (!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(packageName)) {
            return true;
        }

        return false;
    }


    /**
     * 将当前应用运行到前台
     */
    public static  void bring2Front(String PackageName) {
        ActivityManager activtyManager = (ActivityManager) AppContext.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activtyManager.getRunningTasks(3);
        for (ActivityManager.RunningTaskInfo runningTaskInfo : runningTaskInfos) {
            if (PackageName.equals(runningTaskInfo.topActivity.getPackageName())) {
                activtyManager.moveTaskToFront(runningTaskInfo.id, ActivityManager.MOVE_TASK_WITH_HOME);
                return;
            }
        }
    }

    /**
     * 回到系统桌面
     */
    public static  void back2Home() {
        Intent home = new Intent(Intent.ACTION_MAIN);

        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        home.addCategory(Intent.CATEGORY_HOME);

        AppContext.getContext().startActivity(home);
    }
}
