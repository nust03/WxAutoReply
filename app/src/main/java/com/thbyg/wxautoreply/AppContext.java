package com.thbyg.wxautoreply;

import android.app.Application;
import android.content.Context;

/**
 * Created by myxiang on 2017/10/5.
 */

public class AppContext extends Application {

    private static Context instance;

    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
        //instance = getApplicationContext();
    }

    public static Context getContext()
    {
        return instance;
    }

}