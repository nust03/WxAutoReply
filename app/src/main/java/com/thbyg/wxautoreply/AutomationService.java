package com.thbyg.wxautoreply;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Created by Administrator on 2017/2/15.
 */

public class AutomationService extends AccessibilityService{

    private static final String TAG = "AutomationService";
    private static AutomationService mService = null;

    public static AutomationService getService(){
        return mService;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //接收事件,如触发了通知栏变化、界面变化等
        Log.i("mService", "AccessibilityEvent按钮点击变化");
        //performClick();
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG,"onServiceConnected");
        mService = this;

    }



}