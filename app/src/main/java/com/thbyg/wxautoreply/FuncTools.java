package com.thbyg.wxautoreply;

import android.app.Application;

import java.util.Random;

import static java.lang.Thread.sleep;

/**常用工具类
 * Created by myxiang on 2017/10/9.
 */

public class FuncTools {
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
}
