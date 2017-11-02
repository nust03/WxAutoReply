package com.thbyg.wxautoreply;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
/**
 * Created by myxiang on 2017/10/6.
 */



public class BaseAccessibilityService extends AccessibilityService {

    private AccessibilityManager mAccessibilityManager;
    private Context mContext;
    private static BaseAccessibilityService mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        LogToFile.init(AppContext.getContext());
        LogToFile.write("onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogToFile.write("onDestroy");
    }

    public void init(Context context) {
        mContext = context.getApplicationContext();
        mAccessibilityManager = (AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        //List<AccessibilityServiceInfo> list =  mAccessibilityManager.
        LogToFile.write("BaseAccessibilityService init...");
    }

    public static BaseAccessibilityService getInstance() {
        if (mInstance == null) {
            mInstance = new BaseAccessibilityService();
        }
        return mInstance;
    }

    /**
     * Check当前辅助服务是否启用
     *
     * @param serviceName serviceName
     * @return 是否启用
     */
    public boolean checkAccessibilityEnabled(String serviceName) {
        boolean f = false;
        List<AccessibilityServiceInfo> accessibilityServices =
                mAccessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            LogToFile.write("info.getId():" + info.getId() + ",serviceName:" + serviceName);
            if (info.getId().equalsIgnoreCase(serviceName)) {
                f = true;
                break;
            }
        }
        return f;
    }

    //判断服务是否打开
    public boolean isAccessibilitySettingsOn(Context mContext,String service) {
        int accessibilityEnabled = 0;
        //final String service = getPackageName() + "/" + AutomationService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            LogToFile.write("accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            LogToFile.write("Error finding setting, default accessibility to not found: " + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            LogToFile.write("***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                LogToFile.write("获取系统当前已打开辅助功能的服务,settingValue=" + settingValue);
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    LogToFile.write("-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        LogToFile.write("We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            LogToFile.write("***ACCESSIBILITY IS DISABLED***");
        }
        return false;
    }
    /**
     * 前往开启辅助服务界面
     */
    public void goAccess() {
        if(checkAccessibilityEnabled("com.thbyg.wxautoreply/.AutomationService") == false) {
            LogToFile.write("com.thbyg.wxautoreply/.AutomationService 辅助服务未开启，准备开启...");
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
        else
        {
            LogToFile.write("com.thbyg.wxautoreply/.AutomationService 辅助服务已开启。");
        }
    }

    /**
     * 遍历所有节点并打印
     */
    public void printNodeInfo(List<AccessibilityNodeInfo> nodeList) {
        AccessibilityNodeInfo rowNode = this.getRootInActiveWindow();
        LogToFile.write("==============================================开始打印");
        if (rowNode == null) {
            LogToFile.write("noteInfo is　null");

        } else {
            nodeList.clear();
            recycle(rowNode,nodeList,true,0);
        }
        LogToFile.write("==============================================结束打印===node count = " + String.valueOf(nodeList.size()));
    }
    /**
     * 遍历所有节点并打印
     */
    public void printNodeInfo(AccessibilityNodeInfo Node) {
        AccessibilityNodeInfo rowNode = Node;
        List<AccessibilityNodeInfo> nodeList = new ArrayList<AccessibilityNodeInfo>();
        LogToFile.write("==============================================开始打印");
        if (rowNode == null) {
            LogToFile.write("noteInfo is　null");

        } else {
            nodeList.clear();
            recycle(rowNode,nodeList,true,0);
        }
        LogToFile.write("==============================================结束打印===node count = " + String.valueOf(nodeList.size()));
    }
    /**
     * 遍历所有节点并打印
     */
    public void printNodeInfo(AccessibilityNodeInfo rowNode,List<AccessibilityNodeInfo> nodeList) {
        LogToFile.write("==============================================开始打印");
        if (rowNode == null) {
            LogToFile.write("noteInfo is　null");

        } else {
            nodeList.clear();
            recycle(rowNode,nodeList,true,0);
        }
        LogToFile.write("==============================================结束打印===node count = " + String.valueOf(nodeList.size()) );
    }
    /**
     * 遍历所有节点并打印
     */
    public void printNodeInfo(AccessibilityNodeInfo rowNode,List<AccessibilityNodeInfo> nodeList,boolean isPrint) {
        LogToFile.write("==============================================开始打印");
        if (rowNode == null) {
            LogToFile.write("noteInfo is　null");

        } else {
            nodeList.clear();
            recycle(rowNode,nodeList,isPrint,0);
        }
        LogToFile.write("==============================================结束打印===node count = " + String.valueOf(nodeList.size()));
    }

    public void recycle(AccessibilityNodeInfo info,List<AccessibilityNodeInfo> nodeList,boolean isPrint,int level) {
        if(isPrint && (info.getText() != null || info.getContentDescription() != null)) LogToFile.write("Level: " + String.valueOf(level) + " ,node isVisibleToUser=" + String.valueOf(info.isVisibleToUser()) + ";info:" + info.toString());
        nodeList.add(info);
        if (info.getChildCount() == 0) {

        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    recycle(info.getChild(i),nodeList,isPrint,level++);
                }
            }
        }
    }
    /**
     * 模拟点击事件
     *
     * @param nodeInfo nodeInfo
     */
    public void performViewClick(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return;
        }
        while (nodeInfo != null) {
            if (nodeInfo.isClickable()) {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
            nodeInfo = nodeInfo.getParent();
        }
    }

    /**
     * 模拟返回操作
     */
    public void performBackClick(int delay) {
        performGlobalAction(GLOBAL_ACTION_BACK);
        FuncTools.delay(delay);
    }

    /**
     * 模拟返回HOME操作
     */
    public void performHomeClick(int delay) {
        performGlobalAction(GLOBAL_ACTION_HOME);
        FuncTools.delay(delay);
    }

    /**
     * 模拟下滑操作
     */
    public void performScrollBackward() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        performGlobalAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    /**
     * 模拟上滑操作
     */
    public void performScrollForward() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        performGlobalAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
    }



    /**
     * 查找对应文本的View
     *
     * @param text text
     * @return View
     */
    public AccessibilityNodeInfo findViewByText(String text) {
        return findViewByText(text, false);
    }

    /**
     * 查找对应文本的View
     *
     * @param text      text
     * @param clickable 该View是否可以点击
     * @return View
     */
    public AccessibilityNodeInfo findViewByText(String text, boolean clickable) {
        AccessibilityNodeInfo accessibilityNodeInfo = getRootInActiveWindow();
        if (accessibilityNodeInfo == null) {
            LogToFile.write("fail!");
            return null;
        }
        List<AccessibilityNodeInfo> nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText(text);
        LogToFile.write(String.valueOf(nodeInfoList.size()));
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
                LogToFile.write(nodeInfo.toString());
                if (nodeInfo != null && (nodeInfo.isClickable() == clickable)) {
                    return nodeInfo;
                }
            }
        }
        return null;
    }

    /**
     * 查找对应ID的View
     *
     * @param id id
     * @return View
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public AccessibilityNodeInfo findViewByID(String id) {
        AccessibilityNodeInfo accessibilityNodeInfo = getRootInActiveWindow();
        if (accessibilityNodeInfo == null) {LogToFile.write("fail!");
            return null;
        }
        List<AccessibilityNodeInfo> nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(id);
        LogToFile.write(String.valueOf(nodeInfoList.size()));
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
                LogToFile.write(nodeInfo.toString());
                if (nodeInfo != null) {
                    return nodeInfo;
                }
            }
        }
        return null;
    }

    public void clickTextViewByText(String text) {
        AccessibilityNodeInfo accessibilityNodeInfo = getRootInActiveWindow();
        if (accessibilityNodeInfo == null) {
            return;
        }
        List<AccessibilityNodeInfo> nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText(text);
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
                if (nodeInfo != null) {
                    performViewClick(nodeInfo);
                    break;
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void clickTextViewByID(String id) {
        AccessibilityNodeInfo accessibilityNodeInfo = getRootInActiveWindow();
        if (accessibilityNodeInfo == null) {
            return;
        }
        List<AccessibilityNodeInfo> nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(id);
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
                if (nodeInfo != null) {
                    performViewClick(nodeInfo);
                    break;
                }
            }
        }
    }

    /**
     * 模拟输入
     *
     * @param nodeInfo nodeInfo
     * @param text     text
     */
    public void inputText(AccessibilityNodeInfo nodeInfo, String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("label", text);
            clipboard.setPrimaryClip(clip);
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }
}

