package com.thbyg.wxautoreply;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import org.w3c.dom.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.lang.Thread.sleep;


public class AutomationService extends BaseAccessibilityService {
    //region 全局参数定义
    private final static String MM_PNAME = "com.tencent.mm";
    private String currentActivityName = "";
    private String pre_notification_time = null;
    private boolean hasNotification = false;//有新通知信息
    private String notification_text = null;
    private boolean hasAction = false;//自动回复信息
    private boolean hasFriendRequest = false;//接受好友请求
    private int hasFriendRequestCount = 0;
    private boolean hasReadMPAticle = false;//阅读公众号文章标示
    private String MP_Account_Name = "";//拟阅读公众号账号
     private String MP_Article_Date = "";//拟阅读公众号信息日期
    private boolean hasMsgSend = false;//有待发送的信息
    private List<String> MsgReadySend = new ArrayList<String>();//第一个参数是接受人，其余的参数是待发送的信息
    private List<AccessibilityNodeInfo> Readed_Text_List = new ArrayList<AccessibilityNodeInfo>();//已读公众号文章Text
    private boolean hasAttentionGZH = false;//有待关注的微信公众号任务
    private String Bottom_Menu_Name = "";//底栏菜单按钮所在页面
    private boolean locked = false;
    private boolean background = false;
    private String  stay_input_time = "";//在输入界面停留的循环次数
    private String runcontent = "";//上下文
    private int runcount = 3;//上下文
    private String sname;
    private String scontent;
    private AccessibilityNodeInfo itemNodeinfo;
    private KeyguardManager.KeyguardLock kl;
    private Handler handler = new Handler();
    private static AutomationService mService = null;
    AccessibilityNodeInfo focused_node = null;
    List<AccessibilityNodeInfo> nodelist = new ArrayList<AccessibilityNodeInfo>();
    private static String[] autoplay_msg = new String[]{"[微笑][微笑][微笑]", "[玫瑰][玫瑰][玫瑰][玫瑰][玫瑰][玫瑰][玫瑰][玫瑰][玫瑰]", "[强][强][强]",
            "[拥抱][拥抱]", "[握手][握手]", "[拳头][拳头]", "[OK]", "OK", "ok", "好的", "HI", "好","Hello","你好"};
    private String ChatName = "";
    private String ChatRecord = "";
    private String VideoSecond = "";
    //endregion
    //region 服务初始化函数
    @Override
    public void onCreate() {
        super.onCreate();
        LogToFile.init(AppContext.getContext());
        LogToFile.write("AutomationService is onCreate.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogToFile.write("AutomationService is onDestroy.");
        LogToFile.toast("AutomationService is onDestroy.");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mService = this;
        AccessibilityServiceInfo serviceInfo = new AccessibilityServiceInfo();
        serviceInfo.flags = AccessibilityServiceInfo.DEFAULT |AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        //serviceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ;
        serviceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED | AccessibilityEvent.TYPE_VIEW_SCROLLED | AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        //serviceInfo.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        serviceInfo.packageNames = new String[]{NodeFunc.Wx_PackageName};
        serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        serviceInfo.notificationTimeout = 500;
        setServiceInfo(serviceInfo);
        LogToFile.write("AutomationService is onServiceConnected.");
        LogToFile.toast("AutomationService is onServiceConnected.");
    }
    //endregion
    //region * 处理服务事件函数 onAccessibilityEvent
    @Override
    //endregion
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        int eventType = event.getEventType();
        if(event.getPackageName().toString().equalsIgnoreCase(NodeFunc.Wx_PackageName) == false) return;
        String s = String.format("=====处理 event=%s 开始...", AccessibilityEvent.eventTypeToString(eventType)) + ",UI_Title=" + NodeFunc.getContentDescription(this.getRootInActiveWindow());
        //s +=  ",PackageName=" + event.getPackageName().toString() + ",ClassName=" + event.getClassName().toString() + ",CurrentActivityName=" + setCurrentActivityName(event);
        LogToFile.write(s);
        if(locked == true) {
            LogToFile.write("重复进入！");
        }
        else locked = true;
        int default_run_count =  0;
        try {
            switch (eventType) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                    //region 处理 TYPE_NOTIFICATION_STATE_CHANGED 事件
                    wakeUpAndUnlock();//唤醒手机屏幕并解锁
                    if(hasAction == false && hasFriendRequest == false  && hasReadMPAticle == false) {
                        pre_notification_time = DateUtils.getCurrentTime();
                        dealNotificationEvent(event);//处理通知消息
                    }
                    else{
                        String current_time = DateUtils.getCurrentTime();
                        int second = DateUtils.compareTime(pre_notification_time,current_time,3);
                        if(second > 300){
                            LogToFile.write("距离上次处理通知消息已超过300秒，强制复位运行标示。second=" + String.valueOf(second));
                            hasReadMPAticle = false;
                            hasAction = false;
                            hasFriendRequest = false;
                        }
                    }
                    break;
                    //endregion
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    if(event.getPackageName().equals("com.tencent.mm") ) {
                        currentActivityName = setCurrentActivityName(event);
                        LogToFile.write("TYPE_WINDOW_STATE_CHANGED:currentActivityName=" + currentActivityName);
                        LogToFile.toast("TYPE_WINDOW_STATE_CHANGED:currentActivityName=" + currentActivityName);
                    }
                    //break;
                default:
                    if(hasNotification && default_run_count == 0){
                        dealNotificationMsg(event);//处理通知消息
                        default_run_count++;
                    }
                    if(hasAction && default_run_count == 0){
                        dealAutoReplay(event);//处理自动回复消息
                        default_run_count++;
                    }
                    if(hasFriendRequest  && default_run_count == 0){
                        dealNewfriendRequest(event);//处理好友请求
                        default_run_count++;
                    }
                    if(hasAttentionGZH  && default_run_count == 0){
                        dealAttentionGZHRequest(event);//处理加公众号关注请求
                        default_run_count++;
                    }
                    if(hasReadMPAticle && default_run_count == 0){//处理阅读公众号文章
                        //dealReadGzhHistoryArticle(event);
                        dealReadMPArticle(event);
                        default_run_count++;
                    }
                    if(hasMsgSend && default_run_count == 0){//处理待发送信息
                        dealMsgReadySend(event);
                        default_run_count++;
                    }
                    if(hasAction == false && hasFriendRequest == false && hasReadMPAticle == false && hasMsgSend == false  && hasAttentionGZH == false && hasNotification == false && default_run_count == 0){
                        Bottom_Menu_Name = "";
                        switch2Keyboard(event);//判断是否有“切换到键盘”按钮，如有切换到输入界面
                        //this.printNodeInfo(this.getRootInActiveWindow());
                    }
                    else{
                        stay_input_time = DateUtils.getCurrentTime("yyyy-MM-dd HH:mm:ss");
                        //LogToFile.write("初始化时间stay_input_time=" + stay_input_time);
                    }
                    //this.printNodeInfo(this.getRootInActiveWindow());

            }
        }
        catch (Exception e){
            LogToFile.write("onAccessibilityEvent Fail!Error=" + e.getMessage());
            LogToFile.toast("onAccessibilityEvent Fail!Error=" + e.getMessage());
        }
        locked = false;
        LogToFile.write("locked:" + String.valueOf(locked) + ",hasAction=" + String.valueOf(hasAction)  + ",hasReadMPAticle=" + String.valueOf(hasReadMPAticle)
                + ",hasAttentionGZH=" + String.valueOf(hasAttentionGZH) + ",hasNotification=" + String.valueOf(hasNotification)
                + ",hasFriendRequest=" + String.valueOf(hasFriendRequest) + ",hasFriendRequestCount=" + String.valueOf(hasFriendRequestCount) + ",hasMsgSend=" + String.valueOf(hasMsgSend));
        LogToFile.write(String.format("=====处理 event=%s 结束！default_run_count=%d", AccessibilityEvent.eventTypeToString(eventType),default_run_count) + ",UI_Title=" + NodeFunc.getContentDescription(this.getRootInActiveWindow()));
    }
    //region 设置当前页面名称
    /**
     * 设置当前页面名称
     *
     * @param event
     */
    //endregion
    private String setCurrentActivityName(AccessibilityEvent event) {
        String activityname = "";
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return activityname;
        }
        try {
            ComponentName componentName = new ComponentName(event.getPackageName().toString(), event.getClassName().toString());
            getPackageManager().getActivityInfo(componentName, 0);
            activityname = componentName.flattenToShortString();
        } catch (Exception e) {
            activityname = "";
            LogToFile.write("setCurrentActivityName Fail!Error=" + e.getMessage());
            LogToFile.toast("setCurrentActivityName Fail!Error=" + e.getMessage());
        }finally {
            if(activityname.isEmpty()) activityname = currentActivityName;
            return activityname;
        }
    }
    //region 跳转到 聊天 页面 goGroupChatUI(AccessibilityEvent event)
    //endregion
    public void goGroupChatUI(String GroupName){
        LogToFile.write("跳转到 聊天 " + GroupName + " 页面开始运行,goGroupChatUI is running...");
        int success_click_btn_count = 0;
        int delay_after_click = 1000;//click后延迟 1000 毫秒
        int find_text_count = 0;
        List<AccessibilityNodeInfo> chat_record_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> all_node_list = new ArrayList<AccessibilityNodeInfo>();

        try{
            //this.printNodeInfo(find_node_list);
            int node_count = NodeFunc.findNodebyID_Class(this.getRootInActiveWindow(),NodeFunc.Wx_ChatRecord_Node_ID,NodeFunc.Wx_ChatRecord_Node_Class,chat_record_node_list);

            if(node_count > 0){

                for(AccessibilityNodeInfo node : chat_record_node_list){
                    if(node.getText() != null) {
                        if(GroupName.equalsIgnoreCase(NodeFunc.getText(node))){
                            find_text_count++;
                            AccessibilityNodeInfo click_find_node = NodeFunc.getClickableParentNode(node);
                            if (click_find_node != null) {
                                if (click_find_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    LogToFile.write("进入“" + GroupName + "” 聊天页面成功。");
                                    FuncTools.delay(delay_after_click);
                                    success_click_btn_count++;
                                    this.printNodeInfo(this.getRootInActiveWindow(),all_node_list,false);
                                } else {
                                    LogToFile.write("进入“" + GroupName + "”聊天页面失败。");
                                }
                            }
                        }
                    }
                }
            }

        }catch (Exception e){
            LogToFile.write("goGroupChatUI Fail!Error=" + e.getMessage());
            LogToFile.toast("goGroupChatUI Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write("跳转到 聊天 " + GroupName + " 页面 运行结束,goGroupChatUI run over.success_click_btn_count=" + String.valueOf(success_click_btn_count));
        }
    }
    //region 唤醒手机屏幕并解锁
    /**
     * 唤醒手机屏幕并解锁
     */
    //endregion
    public static void wakeUpAndUnlock() {
        // 获取电源管理器对象
        PowerManager pm = (PowerManager) AppContext.getContext().getSystemService(Context.POWER_SERVICE);
        boolean screenOn = pm.isScreenOn();
        if (!screenOn) {
            // 获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
            wl.acquire(10000); // 点亮屏幕
            wl.release(); // 释放
        }
        // 屏幕解锁
        KeyguardManager keyguardManager = (KeyguardManager) AppContext.getContext().getSystemService(KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("unLock");
        // 屏幕锁定
        keyguardLock.reenableKeyguard();
        keyguardLock.disableKeyguard(); // 解锁
    }
    //region 设置到“订阅号”页面 goDYHUI(AccessibilityEvent event)
    //endregion
    public void goDYHUI(AccessibilityEvent event){
        LogToFile.write("设置到“订阅号”页面开始运行,goDYHUI is running...");
        int success_click_btn_count = 0;
        int delay_after_click = 1000;//click后延迟 1000 毫秒
        int find_text_count = 0;
        List<AccessibilityNodeInfo> find_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> all_node_list = new ArrayList<AccessibilityNodeInfo>();
        int line = 0;
        try{
            //this.printNodeInfo(find_node_list);
            int node_count = NodeFunc.findNodebyID_Class(this.getRootInActiveWindow(),NodeFunc.Wx_MPDYH_ID,NodeFunc.Wx_MPDYH_Class,find_node_list);
            LogToFile.write(String.valueOf(line++));
            if(node_count > 0){
                //LogToFile.write("node_count=" + String.valueOf(node_count));
                for(AccessibilityNodeInfo node : find_node_list){
                    if(node.getText() != null) {
                        if("订阅号".equalsIgnoreCase(NodeFunc.getText(node))){
                            find_text_count++;
                            AccessibilityNodeInfo click_find_node = NodeFunc.getClickableParentNode(node);
                            if (click_find_node != null) {
                                if (click_find_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    LogToFile.write("点击“订阅号”按钮成功。");
                                    Bottom_Menu_Name = "订阅号";
                                    FuncTools.delay(delay_after_click);
                                    success_click_btn_count++;
                                    this.printNodeInfo(this.getRootInActiveWindow(),all_node_list,false);
                                } else {
                                    LogToFile.write("点击“订阅号”按钮失败。");
                                }
                            }
                        }
                    }
                }
            }
            LogToFile.write(String.valueOf(line++));
        }catch (Exception e){
            LogToFile.write("goDYHUI Fail!Error=" + e.getMessage());
            LogToFile.toast("goDYHUI Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write("设置到“订阅号”页面 运行结束,goDYHUI run over.success_click_btn_count=" + String.valueOf(success_click_btn_count));
        }
    }


    //region 设置到“设置”页面 goSettingUI(AccessibilityEvent event)
    //endregion
    public void goSettingUI(AccessibilityEvent event){
        LogToFile.write("设置到“设置”页面开始运行,goSettingUI is running...");
        int success_click_btn_count = 0;
        int delay_after_click = 200;//click后延迟 500 毫秒
        int find_text_count = 0;
        List<AccessibilityNodeInfo> find_node_list = new ArrayList<AccessibilityNodeInfo>();
        int line = 0;
        try{
            int node_count = NodeFunc.findNodebyID_Class(this.getRootInActiveWindow(),NodeFunc.Wx_FindUI_Node_ID,NodeFunc.Wx_FindUI_Node_Class,find_node_list);
            LogToFile.write(String.valueOf(line++));
            if(node_count > 0){
                //LogToFile.write("node_count=" + String.valueOf(node_count));
                for(AccessibilityNodeInfo node : find_node_list){
                    if(node.getText() != null) {
                        if (NodeFunc.Wx_WoUI_Node_Text.contains(NodeFunc.getText(node))) {
                            find_text_count++;
                        }
                        if("设置".equalsIgnoreCase(NodeFunc.getText(node))){
                            AccessibilityNodeInfo click_find_node = NodeFunc.getClickableParentNode(node);
                            if (click_find_node != null) {
                                if (click_find_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    LogToFile.write("点击“设置”按钮成功。");
                                    Bottom_Menu_Name = "设置";
                                    FuncTools.delay(delay_after_click);
                                } else {
                                    LogToFile.write("点击“设置”按钮失败。");
                                }
                            }
                        }
                    }
                }
            }
            LogToFile.write(String.valueOf(line++));
            if(find_text_count >=4 ){
                LogToFile.write("当前页面已设置在“我”页面。find_text_count=" + String.valueOf(find_text_count));
            }
            else{
                //“发现”菜单
                AccessibilityNodeInfo find_node = NodeFunc.findNodebyID_Class_Text(this.getRootInActiveWindow(),NodeFunc.Wx_BottomMenu_Btn_ID,NodeFunc.Wx_BottomMenu_Text[3],NodeFunc.Wx_BottomMenu_Btn_Class);
                AccessibilityNodeInfo click_find_node = null;
                if(find_node != null) {
                    LogToFile.write(find_node.toString());
                    click_find_node = NodeFunc.getClickableParentNode(find_node);
                    if (click_find_node != null) {
                        if (click_find_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            LogToFile.write("点击“我”按钮成功。");
                            FuncTools.delay(delay_after_click);
                        } else {
                            LogToFile.write("点击“我”按钮失败。");
                        }
                    } else {
                        this.performBackClick(200);
                    }
                }
            }
            LogToFile.write(String.valueOf(line++));
        }catch (Exception e){
            LogToFile.write("goSettingUI Fail!Error=" + e.getMessage());
            LogToFile.toast("goSettingUI Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write("设置到“设置”页面 运行结束,goSettingUI run over.success_click_btn_count=" + String.valueOf(success_click_btn_count));
        }
    }

    //region 设置到“朋友圈”页面 goMoments(AccessibilityEvent event)
    //endregion
    public void goMoments(AccessibilityEvent event){
        LogToFile.write("设置到“朋友圈”页面开始运行,goMoments is running...");
        int success_click_btn_count = 0;
        int delay_after_click = 1000;//click后延迟 500 毫秒
        int max_run_count = 3;
        boolean isMoments = false;
        int node_count = 0;
        List<AccessibilityNodeInfo> node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> all_node_list = new ArrayList<AccessibilityNodeInfo>();
        int line = 0;
        try{
            for(int i=0;i<max_run_count;i++) {
                node_count = NodeFunc.findNodebyClass_Desc(this.getRootInActiveWindow(), NodeFunc.Wx_MomentsUI_Class, NodeFunc.Wx_MomentsUI__Desc, node_list);
                LogToFile.write("i=" + String.valueOf(i) + ",node_count=" + String.valueOf(node_count) + ",node_list.size()=" + String.valueOf(node_list.size()));
                if (node_count == 0) {
                    this.printNodeInfo(this.getRootInActiveWindow(), all_node_list, true);
                    for (AccessibilityNodeInfo node : all_node_list) {
                        if (node.getText() != null) {
                            if ("朋友圈".equalsIgnoreCase(NodeFunc.getText(node))) {
                                AccessibilityNodeInfo click_find_node = NodeFunc.getClickableParentNode(node);
                                if (click_find_node != null) {
                                    if (click_find_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                        LogToFile.write("点击“朋友圈”按钮成功。nodeinfo=" + node.toString());
                                        FuncTools.delay(delay_after_click);
                                        success_click_btn_count = 1;
                                        break;
                                    } else {
                                        LogToFile.write("点击“朋友圈”按钮失败。nodeinfo=" + node.toString());
                                    }
                                }
                            }

                        }
                    }
                    if (success_click_btn_count == 0) {
                        node_count = NodeFunc.findNodebyClass_Desc(this.getRootInActiveWindow(), NodeFunc.Wx_BackBtn_Class, NodeFunc.Wx_BackBtn__Desc, node_list);
                        if (node_count > 0) {
                            LogToFile.write("发现“返回”按钮，node_count=" + String.valueOf(node_count));
                            this.performBackClick(delay_after_click);
                        }
                    }
                }
                else break;
            }
        }catch (Exception e){
            LogToFile.write("goMoments Fail!Error=" + e.getMessage());
            LogToFile.toast("goMoments Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write("设置到“朋友圈”页面 运行结束,goMoments run over.success_click_btn_count=" + String.valueOf(success_click_btn_count));
        }
    }
    //region  处理通知消息
    //endregion
    public void dealNotificationEvent(AccessibilityEvent event){
        //LogToFile.write("eventType=" + String.format("0x%x,%s,event=%s", eventType, AccessibilityEvent.eventTypeToString(eventType), event.toString()));
        LogToFile.write("处理通知消息开始运行,dealNotificationEvent is running...");
        List<CharSequence> texts = null;
        int pri = -1;
        try {
            if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                Notification notification = (Notification) event.getParcelableData();
                pri = notification.priority;
                if (pri == Notification.PRIORITY_MAX) {//加好友信息 PRIORITY_MAX = 2
                }
            }
            texts = event.getText();
            if (!texts.isEmpty()) {
                for (CharSequence text : texts) {
                    String content = text.toString();
                    LogToFile.write("TYPE_NOTIFICATION_STATE_CHANGED:content:" + content + ",pri=" + String.valueOf(pri));
                    if (!TextUtils.isEmpty(content) ) {//&& (content.indexOf("史言兵") == 0 || content.indexOf("优惠券搬运工") == 0)
                        background = true;
                        notifyWechat(event);
                        LogToFile.write("TYPE_NOTIFICATION_STATE_CHANGED:sname=" + sname + ",scontent=" + scontent);
                    }
                }
            }
        }
        catch (Exception e){
            LogToFile.write("dealNotificationEvent Fail!Error=" + e.getMessage());
            LogToFile.toast("dealNotificationEvent Fail!Error=" + e.getMessage());
        }
        LogToFile.write("处理通知消息运行结束,dealNotificationEvent run over.");
    }
    //region 根据通知消息，判断下一步命令
    //endregion
    public void dealNotificationMsg(AccessibilityEvent event){
        LogToFile.write("根据通知消息，判断下一步命令 开始运行,dealNotificationMsg is running...notification_text=" + notification_text);
        int delay_after_click = 200;//click后延迟200毫秒
        AccessibilityNodeInfo linearLayout_node = null;
        AccessibilityNodeInfo root_node = null;
        AccessibilityNodeInfo top_left_node = null;
        AccessibilityNodeInfo chat_node = null;
        AccessibilityNodeInfo switch2keyboard_node = null;
        AccessibilityNodeInfo switch2talk_node = null;
        String ui_title = "";
        String top_left_text = "";
        String package_name = "";
        int success_click_btn_count = 0;
        try {
            root_node = this.getRootInActiveWindow();
            ui_title = NodeFunc.getContentDescription(root_node);//取当前页面描述，标题
            top_left_text = NodeFunc.getTopLeftText(ui_title);
            linearLayout_node = NodeFunc.getLinearLayoutNodeinfo(root_node);
            package_name = NodeFunc.getPackageName(root_node);

            if (notification_text.contains("请求添加你为") && success_click_btn_count == 0) {//加好友信息 PRIORITY_MAX = 2
                String[] cc = notification_text.split("请求添加你为");
                sname = cc[0].trim();scontent = cc[1].trim();hasFriendRequest = true;success_click_btn_count++;
            }
            if (notification_text.contains("向你推荐了") && success_click_btn_count == 0) {//加好友信息 PRIORITY_MAX = 2
                String[] cc = notification_text.split("向你推荐了");
                sname = cc[0].trim();scontent = cc[1].trim();hasAttentionGZH = true;success_click_btn_count++;
                if(sname.contains(":")){
                    cc = sname.split("\\:"); sname = cc[0].trim();
                }
            }
            if(success_click_btn_count == 0)
            {
                String[] cc = notification_text.split("\\:");
                if(cc.length >= 2) {
                    sname = cc[0].trim();scontent = cc[1].trim();
                }
                else{
                    scontent = notification_text;
                }
                hasAction = true;
            }

        }
        catch (Exception e){
            LogToFile.write("dealNotificationMsg Fail!Error=" + e.getMessage());
            LogToFile.toast("dealNotificationMsg Fail!Error=" + e.getMessage());
        }finally {
            hasNotification = false;
            LogToFile.write("根据通知消息，判断下一步命令 运行结束,dealNotificationMsg run over.sname=" + sname + ",scontent=" + scontent);
        }

    }

    //region 处理自动回复消息
    //endregion
    public void dealAutoReplay(AccessibilityEvent event){
        LogToFile.write("处理自动回复消息开始运行,dealAutoReplay is running...");
        //region 参数定义
        int delay_after_click = 200;//click后延迟200毫秒
        AccessibilityNodeInfo linearLayout_node = null;
        AccessibilityNodeInfo root_node = null;
        AccessibilityNodeInfo top_left_node = null;
        AccessibilityNodeInfo chat_node = null;
        AccessibilityNodeInfo switch2keyboard_node = null;
        AccessibilityNodeInfo switch2talk_node = null;
        String ui_title = "";
        String top_left_text = "";
        int success_click_btn_count = 0;
        //endregion
        try {
            //region 参数初始化
            itemNodeinfo = null;
            root_node = this.getRootInActiveWindow();
            ui_title = NodeFunc.getContentDescription(root_node);//取当前页面描述，标题
            top_left_text = NodeFunc.getTopLeftText(ui_title);
            linearLayout_node = NodeFunc.getLinearLayoutNodeinfo(root_node);
            top_left_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/h2",top_left_text,"android.widget.TextView");
            chat_node = NodeFunc.findNodebyClass_Desc(linearLayout_node,"android.widget.TextView","聊天信息");
            switch2keyboard_node = NodeFunc.findNodebyID_Class_Desc(linearLayout_node,"com.tencent.mm:id/a6z","android.widget.ImageButton","切换到键盘");
            switch2talk_node = NodeFunc.findNodebyID_Class_Desc(linearLayout_node,"com.tencent.mm:id/a6z","android.widget.ImageButton","切换到按住说话");
            String className = event.getClassName() == null ? "" : event.getClassName().toString();
            String contentDesc = event.getContentDescription() == null ? "" : event.getContentDescription().toString();
            //endregion
            if(switch2keyboard_node != null && success_click_btn_count == 0){
                LogToFile.write("检测到“切换到键盘”按钮，点击。");
                clickNode(switch2keyboard_node,delay_after_click);success_click_btn_count++;
            }
            if (!top_left_text.isEmpty() && top_left_node != null && chat_node != null && switch2talk_node != null && success_click_btn_count == 0 ) {
                String msg = autoplay_msg[FuncTools.getRandom(autoplay_msg.length)] + ",@" + sname;
                if (sname.equalsIgnoreCase("史言兵") || sname.equalsIgnoreCase("优惠券搬运工")) {
                    if (scontent.indexOf("command") == 0) {
                        String[] command = scontent.trim().split("-");
                        if(command.length >= 2) {
                            if(command[1].trim().equalsIgnoreCase("阅读公众号文章")){
                                hasReadMPAticle = true;
                                MP_Account_Name = command[2];
                                if(command.length >= 4) MP_Article_Date =  command[3];
                                else MP_Article_Date = DateUtils.getCurrentTime("yyyy年M月d日");
                                Readed_Text_List.clear();
                            }
                            LogToFile.write("接到 " + sname + " 返回微信主界面的命令,command=" + command[1] + ",MP_Account_Name:" + MP_Account_Name + ",hasReadMPAticle=" + String.valueOf(hasReadMPAticle));
                            msg= sname + ",接到您 " + command[1] + " 命令，即可执行!";
                        }
                    }
                    sendMsg(NodeFunc.getLinearLayoutNodeinfo(this.getRootInActiveWindow()),msg);
                    LogToFile.write("自动回复信息:" + msg);
                    //if(!hasReadMPAticle) this.performHomeClick(200);
                }
                else{
                    int replay_percent = FuncTools.getRandom(100);
                    if(replay_percent > 90) {//10%的概率回复其他人的信息
                        msg = autoplay_msg[FuncTools.getRandom(autoplay_msg.length)] + "," + sname + ", " + String.valueOf(replay_percent) + "%";
                        sendMsg(NodeFunc.getLinearLayoutNodeinfo(this.getRootInActiveWindow()),msg);
                        LogToFile.write("自动回复信息:" + msg);
                    }
                    //this.performHomeClick(200);
                }
                hasAction = false;
                clickNode(chat_node,delay_after_click);success_click_btn_count++;
            }
            if(success_click_btn_count == 0){
                LogToFile.write("微信窗口尚未激活，等待 " + String.valueOf(delay_after_click)+ " 毫秒。ui_title=" + ui_title);
                FuncTools.delay(delay_after_click);
            }
        }
        catch (Exception e){
            LogToFile.write("dealAutoReplay Fail!Error=" + e.getMessage());
            LogToFile.toast("dealAutoReplay Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write("处理自动回复消息运行结束,dealAutoReplay run over.");
        }

    }
    //region 处理好友请求
    //endregion
    public void dealNewfriendRequest(AccessibilityEvent event){
        LogToFile.write("处理好友请求开始运行,dealNewfriendRequest is running...");
        int success_click_btn_count = 0;
        int delay_after_click = 200;//click后延迟200毫秒
        int node_count = 0;
        List<AccessibilityNodeInfo> all_node_list = new ArrayList<AccessibilityNodeInfo>();
        AccessibilityNodeInfo linearLayout_node = null;
        AccessibilityNodeInfo backbtn_node = null;
        AccessibilityNodeInfo address_node = null;//“通讯录”node
        AccessibilityNodeInfo newFriend_node = null;//“新的朋友”node
        AccessibilityNodeInfo recvBtn_node = null;//“接受”node
        AccessibilityNodeInfo finishBtn_node = null;//“完成”node
        AccessibilityNodeInfo SendMsgBtn_node = null;//“发消息”node
        AccessibilityNodeInfo waiting_node = null;//“请稍候...”node
        AccessibilityNodeInfo dealing_node = null;//“正在处理...”node
        List<String> ui_Title_List = new ArrayList<>(Arrays.asList("当前所在页面,新的朋友","当前所在页面,朋友验证","当前所在页面,详细资料"));
        String run_msg = "";
        String ui_title = "";
        //List<String> conten_list = new ArrayList<>(Arrays.asList("获取通讯录按钮并点击","获取新的朋友按钮并点击","获取接受按钮并点击","获取完成按钮并点击"));
        //根据上下文进行运行，1、获取通讯录按钮；2、
        try{
            AccessibilityNodeInfo root_node = this.getRootInActiveWindow();
            linearLayout_node = NodeFunc.getLinearLayoutNodeinfo(root_node);
            address_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_BottomMenu_Btn_ID,NodeFunc.Wx_BottomMenu_Text[1],NodeFunc.Wx_BottomMenu_Btn_Class);//“通讯录”node
            newFriend_node = NodeFunc.findNodebyText_Class(linearLayout_node,NodeFunc.Wx_NewFriend_Text,NodeFunc.Wx_NewFriend_Class);//“新的朋友”node
            recvBtn_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_NewFriend_RecvBtn_ID,NodeFunc.Wx_NewFriend_RecvBtn_Text,NodeFunc.Wx_NewFriend_RecvBtn_Class);//“接受”node
            finishBtn_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_NewFriend_FinishBtn_ID,NodeFunc.Wx_NewFriend_FinishBtn_Text,NodeFunc.Wx_NewFriend_FinishBtn_Class);//“完成”node
            SendMsgBtn_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_NewFriend_SendMsgBtn_ID,NodeFunc.Wx_NewFriend_SendMsgBtn_Text,NodeFunc.Wx_NewFriend_SendMsgBtn_Class);//“发消息”node
            backbtn_node = NodeFunc.findNodebyClass_Desc(linearLayout_node,NodeFunc.Wx_BackBtn_Class,NodeFunc.Wx_BackBtn__Desc);
            dealing_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_DealingUI_ID,NodeFunc.Wx_DealingUI_Text,NodeFunc.Wx_DealingUI_Class);//“正在处理...”node
            waiting_node = NodeFunc.findNodebyText_Class(linearLayout_node,NodeFunc.Wx_DealingUI_Text,NodeFunc.Wx_DealingUI_Class);//“请稍候...”node
            ui_title = NodeFunc.getContentDescription(root_node);
            if(ui_title.isEmpty() && success_click_btn_count == 0){
                //当前所在页面标题为“空”判断是等待窗口，等待200毫秒
                FuncTools.delay(delay_after_click);
                success_click_btn_count++;
                this.printNodeInfo(linearLayout_node);
                run_msg = "当前所在页面标题为“空”判断是等待窗口，等待200毫秒";
            }
            if(ui_title.equalsIgnoreCase("当前所在页面,新的朋友") && success_click_btn_count == 0){
                //在“当前所在页面,新的朋友”，取“接受”数量如果==0，说明已经全部接受请求，设置hasFriendRequest = false
                node_count = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_NewFriend_RecvBtn_ID,NodeFunc.Wx_NewFriend_RecvBtn_Text,NodeFunc.Wx_NewFriend_RecvBtn_Class,all_node_list);//“接受”node
                if(node_count == 0 ){
                    hasFriendRequest = false;
                    success_click_btn_count++;
                }
                run_msg = "在“当前所在页面,新的朋友”，取“接受”数量如果==0，说明已经全部接受请求，设置hasFriendRequest = false,node_count=" + String.valueOf(node_count) + ",success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(address_node != null && newFriend_node == null && success_click_btn_count == 0){
                //检测到“通讯录”node，但没有检测到“新的朋友”node，应该是在非“通讯录”页面，故点击“通讯录”node
                clickNode(address_node,delay_after_click);
                success_click_btn_count++;
                run_msg = "检测到“通讯录”node，但没有检测到“新的朋友”node，应该是在非“通讯录”页面，故点击“通讯录”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(address_node != null && newFriend_node != null && success_click_btn_count == 0){
                //检测到“通讯录”node 和 “新的朋友”node，应该是在“通讯录”页面，故点击“新的朋友”node
                clickNode(newFriend_node,delay_after_click);
                success_click_btn_count++;
                //FuncTools.delay(1000);
                run_msg = "检测到“通讯录”node 和 “新的朋友”node，应该是在“通讯录”页面，故点击“新的朋友”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(recvBtn_node != null && success_click_btn_count == 0){
                //检测到“接受”node 应该是在“新的朋友”页面，故点击“接受”node
                clickNode(recvBtn_node,delay_after_click);
                success_click_btn_count++;
                run_msg = "检测到“接受”node 应该是在“新的朋友”页面，故点击“接受”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(finishBtn_node != null && success_click_btn_count == 0){
                //检测到“完成”node 应该是在“详细资料”页面，故点击“完成”node
                clickNode(finishBtn_node,delay_after_click);
                //FuncTools.delay(1000);
                success_click_btn_count++;
                run_msg = "检测到“完成”node 应该是在“详细资料”页面，故点击“完成”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(SendMsgBtn_node != null && success_click_btn_count == 0){
                //检测到“发消息”node 应该是在“详细资料”页面，故点击“发消息”node
                clickNode(SendMsgBtn_node,delay_after_click);
                success_click_btn_count++;
                run_msg = "检测到“发消息”node 应该是在“详细资料”页面，故点击“发消息”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(ui_Title_List.contains(ui_title) && success_click_btn_count == 0){
                //当前所在页面是“当前所在页面,新的朋友”,“当前所在页面,朋友验证”,“当前所在页面,详细资料”中的一个，但未执行到任何符合条件的判断，等待200毫秒
                FuncTools.delay(delay_after_click);
                success_click_btn_count++;
                run_msg = "当前所在页面是“当前所在页面,新的朋友”,“当前所在页面,朋友验证”,“当前所在页面,详细资料”中的一个，但未执行到任何符合条件的判断，等待200毫秒";
            }
            if(success_click_btn_count == 0){
                //未执行到任何符合条件的判断，查找“返回”node并点击或者直接执行全局“返回”
                boolean f = sendMsg(linearLayout_node,autoplay_msg[FuncTools.getRandom(autoplay_msg.length)] + "，" + NodeFunc.getTopLeftText(NodeFunc.getContentDescription(root_node)) );//发送消息
                int flags = clickBackNode(backbtn_node,delay_after_click);//点击“返回”按钮
                run_msg = "未执行到任何符合条件的判断，查找“返回”node并点击或者直接执行全局“返回”.检测是否存在Edit输入框，如有发送信息，发送标示f=" + String.valueOf(f);
            }
        }
        catch (Exception e){
            LogToFile.write("dealNewfriendRequest Fail!Error=" + e.getMessage());
            LogToFile.toast("dealNewfriendRequest Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write(run_msg);
            LogToFile.write("处理好友请求运行结束,dealNewfriendRequest run over.success_click_btn_count=" + String.valueOf(success_click_btn_count) + ",ui_title="+ ui_title);
        }
    }
    //region 处理加公众号关注请求
    //endregion
    public void dealAttentionGZHRequest(AccessibilityEvent event){
        LogToFile.write("处理加公众号关注请求 开始运行,dealAttentionGZHRequest is running...");
        int delay_after_click = 200;//click后延迟200毫秒
        AccessibilityNodeInfo linearLayout_node = null;
        AccessibilityNodeInfo root_node = null;
        AccessibilityNodeInfo top_left_node = null;
        AccessibilityNodeInfo chat_node = null;
        AccessibilityNodeInfo attention_node = null;
        AccessibilityNodeInfo entry_node = null;
        AccessibilityNodeInfo switch2keyboard_node = null;
        AccessibilityNodeInfo gzh_card_node = null;
        List<AccessibilityNodeInfo> gzh_card_node_list = new ArrayList<AccessibilityNodeInfo>();
        int gzh_card_node_count = 0;
        AccessibilityNodeInfo gzh_card_parent_node = null;
        String ui_title = "";
        String gzh_card_node_text = "";
        String top_left_text = "";
        int success_click_btn_count = 0;
        boolean run_flag = false;
        try {
            itemNodeinfo = null;
            root_node = this.getRootInActiveWindow();
            ui_title = NodeFunc.getContentDescription(root_node);//取当前页面描述，标题
            top_left_text = NodeFunc.getTopLeftText(ui_title);
            linearLayout_node = NodeFunc.getLinearLayoutNodeinfo(root_node);
            top_left_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/h2",top_left_text,"android.widget.TextView");
            chat_node = NodeFunc.findNodebyClass_Desc(NodeFunc.getLinearLayoutNodeinfo(this.getRootInActiveWindow()),"android.widget.TextView","聊天信息");
            switch2keyboard_node = NodeFunc.findNodebyID_Class_Desc(linearLayout_node,"com.tencent.mm:id/a6z","android.widget.ImageButton","切换到键盘");
            attention_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"android:id/title","关注","android.widget.TextView");
            entry_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"android:id/title","进入公众号","android.widget.TextView");
            //region 从聊天界面提取符合条件“公众号名片” gzh_card_node
            gzh_card_node_count = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/a_t","公众号名片","android.widget.TextView",gzh_card_node_list);//公众号名片
            for(AccessibilityNodeInfo node : gzh_card_node_list){
                gzh_card_parent_node = node.getParent();
                AccessibilityNodeInfo card_name_node = NodeFunc.findNodebyID_Class(gzh_card_parent_node,"com.tencent.mm:id/a_r","android.widget.TextView");
                if(card_name_node != null){
                    if(scontent.equalsIgnoreCase(NodeFunc.getText(card_name_node))){
                        gzh_card_node = card_name_node;gzh_card_node_text = NodeFunc.getText(card_name_node);break;
                    }
                }

            }
            //endregion
            if(switch2keyboard_node != null && success_click_btn_count == 0){
                run_flag = clickNode(switch2keyboard_node,delay_after_click);success_click_btn_count++;
                LogToFile.write("检测到“切换到键盘”按钮，点击。" + ",run_flag=" + String.valueOf(run_flag));
            }
            if(gzh_card_node != null && success_click_btn_count == 0){
                run_flag = clickNode(gzh_card_node,delay_after_click);success_click_btn_count++;
                LogToFile.write("发现“公众号名片”，点击，等待 " + String.valueOf(delay_after_click)+ " 毫秒。gzh_card_node_text=" + gzh_card_node_text + ",run_flag=" + String.valueOf(run_flag));
            }
            if(ui_title.equalsIgnoreCase("当前所在页面,详细资料") && attention_node != null && success_click_btn_count == 0){
                LogToFile.write("发现“关注”按钮，点击，等待 " + String.valueOf(delay_after_click)+ " 毫秒。gzh_card_node_text=" + gzh_card_node_text);
                if(clickNode(attention_node,delay_after_click)){
                    hasAttentionGZH = false;
                    hasMsgSend = true;
                    MsgReadySend.add(sname);//第一个参数是接受人，其余的参数是待发送的信息
                    MsgReadySend.add("关注公众号“" + scontent + "”成功！");
                }
                success_click_btn_count++;
            }
            if(ui_title.equalsIgnoreCase("当前所在页面," + scontent) && entry_node != null && success_click_btn_count == 0){
                run_flag = clickNode(entry_node,delay_after_click);
                hasAttentionGZH = false;
                hasMsgSend = true;
                MsgReadySend.add(sname);//第一个参数是接受人，其余的参数是待发送的信息
                MsgReadySend.add("已关注公众号“" + scontent + "”！");
                success_click_btn_count++;
                LogToFile.write("发现“进入公众号”按钮，点击，等待 " + String.valueOf(delay_after_click)+ " 毫秒。gzh_card_node_text=" + gzh_card_node_text + ",run_flag=" + String.valueOf(run_flag));
            }
            if(success_click_btn_count == 0){
                LogToFile.write("微信窗口尚未激活，等待 " + String.valueOf(delay_after_click)+ " 毫秒。ui_title=" + ui_title);
                FuncTools.delay(delay_after_click);
            }
        }
        catch (Exception e){
            LogToFile.write("dealAttentionGZHRequest Fail!Error=" + e.getMessage());
            LogToFile.toast("dealAttentionGZHRequest Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write("处理加公众号关注请求 运行结束,dealAttentionGZHRequest run over.");
        }
    }
    // region 接到阅读公众号命令，阅读公众号文章--老方法
    //endregion
    public void dealReadMPArticle_Old(AccessibilityEvent event){
        LogToFile.write("接到阅读公众号命令，阅读公众号文章开始运行,dealReadMPArticle is running...");
        int success_click_btn_count = 0;
        int delay_after_click = 200;//click后延迟 500 毫秒
        int delay_after_back_click = 1000;//back click后延迟 1000 毫秒
        int delay_after_read_click = 6000;//阅读click后延迟 6000  毫秒
        AccessibilityNodeInfo linearLayout_node = null;
        AccessibilityNodeInfo root_node = null;
        AccessibilityNodeInfo wechat_node = null;
        AccessibilityNodeInfo mp_node = null;//订阅号node
        AccessibilityNodeInfo mp_account_node = null;//公众号node
        AccessibilityNodeInfo DYHUI_dyhWord_node = null;//订阅号"三个字的参数
        AccessibilityNodeInfo DYHUI_back_node = null;//"订阅号界面  "返回"的参数
        AccessibilityNodeInfo TuWenUI_GZH_node = null;// 特定公众号图文界面，特定“公众号”参数
        AccessibilityNodeInfo TuWenReadUI_GZH_node = null;// 特定公众号图文阅读界面，特定“公众号”参数
        AccessibilityNodeInfo TuWenReadUI_WebPage_node = null;//特定公众号图文阅读界面，“网页由 mp.weixin.qq.com 提供”参数
        List<AccessibilityNodeInfo> account_node_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> single_article_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> mularticle_header_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> mularticle_other_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> back_node_list = new ArrayList<AccessibilityNodeInfo>();
        int account_node_count = 0;
        int tuwen_count = 0;
        int single_article_node_count = 0;
        int mularticle_header_node_count = 0;
        int mularticle_other_node_count = 0;
        int back_node_count = 0;
        String info = "";
        String ui_title = "";

        try{
            try {
                root_node = this.getRootInActiveWindow();
                linearLayout_node = NodeFunc.getLinearLayoutNodeinfo(linearLayout_node);
                ui_title = NodeFunc.getContentDescription(root_node);//取当前页面描述，标题
                wechat_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_BottomMenu_Btn_ID,NodeFunc.Wx_BottomMenu_Text[0],NodeFunc.Wx_BottomMenu_Btn_Class);//主界面  “微信”node
                mp_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_MPDYH_ID,NodeFunc.Wx_MPDYH_Text,NodeFunc.Wx_MPDYH_Class);//"主界面的“订阅号”"三个字的参数
                DYHUI_dyhWord_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_DYHUI_dyhWord_ID,NodeFunc.Wx_DYHUI_dyhWord_Text,NodeFunc.Wx_DYHUI_dyhWord_Class);//"订阅号界面  “订阅号”"三个字的参数
                DYHUI_back_node = NodeFunc.findNodebyID_Class_Desc(linearLayout_node,NodeFunc.Wx_DYHUI_back_ID,NodeFunc.Wx_DYHUI_back_Class,NodeFunc.Wx_DYHUI_back_Desc);//"订阅号界面  "返回"的参数
                TuWenUI_GZH_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_TuWenUI_GZH_ID,this.MP_Account_Name,NodeFunc.Wx_TuWenUI_GZH_Class);// 特定公众号图文界面，特定“公众号”参数
                account_node_count = NodeFunc.findNodebyID_Class(linearLayout_node,NodeFunc.Wx_MP_Account_ID,NodeFunc.Wx_MP_Account_Class,account_node_node_list);
                single_article_node_count = NodeFunc.findNodebyID_Class(linearLayout_node,NodeFunc.Wx_Single_Article_ID,NodeFunc.Wx_Single_Article_Class,single_article_node_list);
                mularticle_header_node_count = NodeFunc.findNodebyID_Class(linearLayout_node,NodeFunc.Wx_MulArticle_Header_ID,NodeFunc.Wx_MulArticle_Header_Class,mularticle_header_node_list);
                mularticle_other_node_count = NodeFunc.findNodebyID_Class(linearLayout_node,NodeFunc.Wx_MulArticle_Other_ID,NodeFunc.Wx_MulArticle_Other_Class,mularticle_other_node_list);
                //公众号文章阅读界面，“返回”按钮的参数
                back_node_count = NodeFunc.findNodebyID_Class(linearLayout_node,NodeFunc.Wx_ReadArticle_Back_ID,NodeFunc.Wx_ReadArticle_Back_Class,back_node_list);
                //特定公众号图文阅读界面，“网页由 mp.weixin.qq.com 提供”参数
                TuWenReadUI_WebPage_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_TuWenReadUI_WebPage_ID,NodeFunc.Wx_TuWenReadUI_WebPage_Text,NodeFunc.Wx_TuWenReadUI_WebPage_Class);
                info = "single_article_node_count=" + String.valueOf(single_article_node_count) + ",mularticle_header_node_count=" + String.valueOf(mularticle_header_node_count);
                info += ",mularticle_other_node_count=" + String.valueOf(mularticle_other_node_count)+ ",back_node_count=" + String.valueOf(back_node_count);
                info += ",account_node_count=" + String.valueOf(account_node_count);
                if (mp_node != null) info += ",订阅号mp_node=" + NodeFunc.getText(mp_node);
                if (DYHUI_dyhWord_node != null)
                    info += ",DYHUI_dyhWord_node=" + NodeFunc.getText(DYHUI_dyhWord_node);
                if (TuWenUI_GZH_node != null)
                    info += ",TuWenUI_GZH_node=" + NodeFunc.getText(TuWenUI_GZH_node);
            }catch (Exception e){
                LogToFile.write("dealReadMPArticle Fail!Error=" + e.getMessage());
                LogToFile.write(info);
            }
            tuwen_count = single_article_node_count + mularticle_header_node_count + mularticle_other_node_count;
            LogToFile.write(info);
            if(wechat_node != null && mp_node == null && success_click_btn_count == 0){
                LogToFile.write("在“主界面”，但不在“微信”界面，点击微信按钮");
                clickNode(wechat_node,delay_after_click);
                success_click_btn_count++;
            }
            if(wechat_node != null && mp_node != null && success_click_btn_count == 0){
                LogToFile.write("在“主界面”的“微信”界面，点击“订阅号”按钮");
                clickNode(mp_node,delay_after_click);
                success_click_btn_count++;
            }
            if(account_node_count > 0 && DYHUI_dyhWord_node != null && DYHUI_back_node != null && tuwen_count == 0 && success_click_btn_count == 0){
                LogToFile.write("在“订阅号”界面查找需需阅读的公众号：" + this.MP_Account_Name);
                for(AccessibilityNodeInfo node : account_node_node_list){
                    if(this.MP_Account_Name.trim().equalsIgnoreCase(NodeFunc.getText(node))){
                        mp_account_node = NodeFunc.getClickableParentNode(node);
                        if(mp_account_node != null ) {
                            mp_account_node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            FuncTools.delay(delay_after_click);
                            success_click_btn_count++;
                            LogToFile.write("发现公众号：" + this.MP_Account_Name + " ,点解进入阅读界面。account_node_count=" + String.valueOf(account_node_count));
                            break;
                        }
                    }
                }
            }
            if(account_node_count > 0 && DYHUI_dyhWord_node != null && DYHUI_back_node != null ){//测试语句
                //LogToFile.write("图文信息数量 tuwen_count=" + String.valueOf(tuwen_count));
                for(AccessibilityNodeInfo node : account_node_node_list){
                    //LogToFile.write(node.toString());
                }
            }
            if(TuWenReadUI_WebPage_node != null) LogToFile.write("TuWenReadUI_WebPage_node=" + NodeFunc.getText(TuWenReadUI_WebPage_node) );//测试语句

            if(back_node_count > 0 && TuWenReadUI_WebPage_node != null && success_click_btn_count == 0){//发现返回按钮
                LogToFile.write("在图文阅读界面点击“返回”按钮。");
                for(AccessibilityNodeInfo node : back_node_list){
                    if(NodeFunc.Wx_ReadArticle_Back_Desc.trim().equalsIgnoreCase(NodeFunc.getContentDescription(node))){
                        AccessibilityNodeInfo parent_node = NodeFunc.getClickableParentNode(node);
                        if(parent_node != null && !Readed_Text_List.contains(node)) {
                            parent_node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            FuncTools.delay(delay_after_back_click);
                            LogToFile.write("公众号：" + this.MP_Account_Name + " ,点击“返回”,等待 " + String.valueOf(delay_after_read_click) + " 毫秒。");
                            success_click_btn_count++;
                            break;
                        }
                    }

                }
            }
            if(single_article_node_count > 0 && TuWenUI_GZH_node != null && success_click_btn_count == 0){//发现有单图文，开始点击阅读单图文
                LogToFile.write("在图文界面发现单图文 single_article_node_count=" +  String.valueOf(single_article_node_count));
                for(AccessibilityNodeInfo node : single_article_node_list){
                    if(NodeFunc.Wx_Single_Article_Text.trim().equalsIgnoreCase(NodeFunc.getText(node))){
                        AccessibilityNodeInfo parent_node = NodeFunc.getClickableParentNode(node);
                        if(parent_node != null && !Readed_Text_List.contains(node)) {
                            parent_node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            FuncTools.delay(delay_after_read_click);
                            LogToFile.write("公众号：" + this.MP_Account_Name + " ,阅读单图文：" + NodeFunc.getText(node) + ",等待 " + String.valueOf(delay_after_read_click) + " 毫秒。");
                            success_click_btn_count++;
                            Readed_Text_List.add(node);
                            break;
                        }
                    }

                }
            }
            if(mularticle_header_node_count > 0 && TuWenUI_GZH_node != null  && success_click_btn_count == 0){//发现有多图文，开始点击阅读多图文的头条
                LogToFile.write("在图文界面发现多图文,多图文的头条 mularticle_header_node_count=" +  String.valueOf(mularticle_header_node_count));
                for(AccessibilityNodeInfo node : mularticle_header_node_list){
                    if(!NodeFunc.getText(node).isEmpty()){
                        AccessibilityNodeInfo parent_node = NodeFunc.getClickableParentNode(node);
                        if(parent_node != null && !Readed_Text_List.contains(node)) {
                            parent_node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            FuncTools.delay(delay_after_read_click);
                            LogToFile.write("公众号：" + this.MP_Account_Name + " ,阅读多图文的头条：" + NodeFunc.getText(node) + ",等待 " + String.valueOf(delay_after_read_click) + " 毫秒。");
                            success_click_btn_count++;
                            Readed_Text_List.add(node);
                            break;
                        }
                    }

                }
            }
            if(mularticle_other_node_count > 0 && TuWenUI_GZH_node != null  && success_click_btn_count == 0){//发现有多图文，开始点击阅读多图文的其他条
                LogToFile.write("在图文界面发现多图文,多图文的其他条 mularticle_other_node_count=" +  String.valueOf(mularticle_other_node_count));
                for(AccessibilityNodeInfo node : mularticle_other_node_list){
                    if(!NodeFunc.getText(node).isEmpty()){
                        AccessibilityNodeInfo parent_node = NodeFunc.getClickableParentNode(node);
                        if(parent_node != null && !Readed_Text_List.contains(node)) {
                            parent_node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            FuncTools.delay(delay_after_read_click);
                            LogToFile.write("公众号：" + this.MP_Account_Name + " ,阅读多图文的其他条：" + NodeFunc.getText(node) + ",等待 " + String.valueOf(delay_after_read_click) + " 毫秒。");
                            success_click_btn_count++;
                            Readed_Text_List.add(node);
                            break;
                        }
                    }

                }
            }
            if(back_node_count > 0 && TuWenReadUI_WebPage_node != null && success_click_btn_count == 0){//发现返回按钮
                LogToFile.write("在图文阅读界面点击“返回”按钮。");
                for(AccessibilityNodeInfo node : back_node_list){
                    if(NodeFunc.Wx_ReadArticle_Back_Desc.trim().equalsIgnoreCase(NodeFunc.getContentDescription(node))){
                        AccessibilityNodeInfo parent_node = NodeFunc.getClickableParentNode(node);
                        if(parent_node != null && !Readed_Text_List.contains(node)) {
                            parent_node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            FuncTools.delay(delay_after_back_click);
                            LogToFile.write("公众号：" + this.MP_Account_Name + " ,点击“返回”,等待 " + String.valueOf(delay_after_read_click) + " 毫秒。");
                            success_click_btn_count++;
                            break;
                        }
                    }

                }
            }
            if(Readed_Text_List.size() == tuwen_count && tuwen_count > 0 && success_click_btn_count == 0){
                String msg = "阅读公众号=" + this.MP_Account_Name + " 图文信息结束，共阅读 " + String.valueOf(tuwen_count) + " 条。";
                LogToFile.write(msg);
                LogToFile.toast(msg);
                this.performBackClick(delay_after_back_click);
                this.performBackClick(delay_after_back_click);
                ChatWith("内部管理群",msg);//跳转到聊天界面发送信息
                int i = 1;
                for(AccessibilityNodeInfo node : Readed_Text_List){
                    LogToFile.write("标题" + String.valueOf(i++) + ":" + NodeFunc.getText(node));
                }

                hasReadMPAticle = false;
                success_click_btn_count++;
            }
            if(success_click_btn_count == 0){
                //this.performBackClick(delay_after_click);
            }
        }
        catch (Exception e){
            LogToFile.write("dealReadMPArticle Fail!Error=" + e.getMessage());
            LogToFile.toast("dealReadMPArticle Fail!Error=" + e.getMessage());
        }
        LogToFile.write("接到阅读公众号命令，阅读公众号文章 运行结束,dealReadMPArticle run over.success_click_btn_count=" + String.valueOf(success_click_btn_count));
    }
    // region 接到阅读公众号命令，阅读公众号文章--重写的方法
    //endregion
    public void dealReadMPArticle(AccessibilityEvent event){
        String gzh_name = "这里是连云港";
        LogToFile.write("接到阅读公众号命令，阅读公众号文章开始运行,（重写的方法）dealReadMPArticle is running...");
        //region 参数定义
        int success_click_btn_count = 0;
        int delay_after_click = 200;//click后延迟 500 毫秒
        int delay_after_back_click = 1000;//back click后延迟 1000 毫秒
        int delay_after_read_click = 6000;//阅读click后延迟 6000  毫秒
        AccessibilityNodeInfo linearLayout_node = null;
        AccessibilityNodeInfo root_node = null;
        AccessibilityNodeInfo top_left_node = null;
        AccessibilityNodeInfo switch2keyboard_node = null;
        AccessibilityNodeInfo msg_button_node = null;
        AccessibilityNodeInfo gzh_edittext_node = null;
        AccessibilityNodeInfo search_node = null;
        AccessibilityNodeInfo edit_search_node = null;
        AccessibilityNodeInfo gzh_account_node = null;
        AccessibilityNodeInfo no_result_node = null;
        AccessibilityNodeInfo more_node = null;
        AccessibilityNodeInfo history_msg_node = null;
        List<AccessibilityNodeInfo> all_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> single_article_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> mularticle_header_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> mularticle_other_node_list = new ArrayList<AccessibilityNodeInfo>();
        AccessibilityNodeInfo backbtn_node = null;
        AccessibilityNodeInfo address_node = null;//“通讯录”node
        AccessibilityNodeInfo gzh_node = null;//“公众号”node
        List<String> ui_Title_List = new ArrayList<>(Arrays.asList("当前所在页面,新的朋友","当前所在页面,朋友验证","当前所在页面,详细资料"));
        int tuwen_count = 0;
        int single_article_node_count = 0;
        int mularticle_header_node_count = 0;
        int mularticle_other_node_count = 0;
        String ui_title = "";
        String top_left_text = "";
        String run_msg = "";
        String info = "";
        //endregion
        try{
            //region 参数初始化
            root_node = this.getRootInActiveWindow();
            ui_title = NodeFunc.getContentDescription(root_node);//取当前页面描述，标题
            top_left_text = NodeFunc.getTopLeftText(ui_title);
            linearLayout_node = NodeFunc.getLinearLayoutNodeinfo(root_node);
            address_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_BottomMenu_Btn_ID,NodeFunc.Wx_BottomMenu_Text[1],NodeFunc.Wx_BottomMenu_Btn_Class);//“通讯录”node
            gzh_node = NodeFunc.findNodebyText_Class(linearLayout_node,"公众号","android.widget.TextView");//“公众号”node
            search_node = NodeFunc.findNodebyClass_Desc(linearLayout_node,"android.widget.TextView","搜索");
            top_left_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/h2",gzh_name,"android.widget.TextView");
            edit_search_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/hb","搜索","android.widget.EditText");
            gzh_account_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/v9",gzh_name,"android.widget.TextView");
            no_result_node  = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/va","无结果","android.widget.TextView");
            more_node  = NodeFunc.findNodebyClass_Desc(linearLayout_node,"android.widget.TextView","更多");
            switch2keyboard_node = NodeFunc.findNodebyID_Class_Desc(linearLayout_node,"com.tencent.mm:id/a6z","android.widget.ImageButton","切换到键盘");
            gzh_edittext_node = NodeFunc.findNodebyID_Class(linearLayout_node,"com.tencent.mm:id/a71","android.widget.EditText");
            msg_button_node = NodeFunc.findNodebyID_Class_Desc(linearLayout_node,"com.tencent.mm:id/a7i","android.widget.ImageView","消息");

            history_msg_node  = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"android:id/title","查看历史消息","android.widget.TextView");
            single_article_node_count = NodeFunc.findNodebyID_Class(linearLayout_node,NodeFunc.Wx_Single_Article_ID,NodeFunc.Wx_Single_Article_Class,single_article_node_list);
            mularticle_header_node_count = NodeFunc.findNodebyID_Class(linearLayout_node,NodeFunc.Wx_MulArticle_Header_ID,NodeFunc.Wx_MulArticle_Header_Class,mularticle_header_node_list);
            mularticle_other_node_count = NodeFunc.findNodebyID_Class(linearLayout_node,NodeFunc.Wx_MulArticle_Other_ID,NodeFunc.Wx_MulArticle_Other_Class,mularticle_other_node_list);
            //特定公众号图文阅读界面，“网页由 mp.weixin.qq.com 提供”参数
            AccessibilityNodeInfo TuWenReadUI_WebPage_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_TuWenReadUI_WebPage_ID,NodeFunc.Wx_TuWenReadUI_WebPage_Text,NodeFunc.Wx_TuWenReadUI_WebPage_Class);
            tuwen_count = single_article_node_count + mularticle_header_node_count + mularticle_other_node_count;
            backbtn_node = NodeFunc.findNodebyClass_Desc(linearLayout_node,NodeFunc.Wx_BackBtn_Class,NodeFunc.Wx_BackBtn__Desc);


            //endregion
            //region 验证部分，测试跟踪用
            info = "single_article_node_count=" + String.valueOf(single_article_node_count) + ",mularticle_header_node_count=" + String.valueOf(mularticle_header_node_count);
            info += ",mularticle_other_node_count=" + String.valueOf(mularticle_other_node_count);
            info += ",ui_title=" + ui_title;
            if (gzh_node != null) info += ",gzh_node=" + NodeFunc.getText(gzh_node);
            if (backbtn_node != null)
                info += ",backbtn_node=" + NodeFunc.getText(backbtn_node);
            if (address_node != null)
                info += ",address_node=" + NodeFunc.getText(address_node);
            //LogToFile.write(info);
            //endregion3222222222222222222222222222222222222222222222222222222222222222222222222222222222
            //region 逻辑执行部分
            //this.printNodeInfo(linearLayout_node);
            if(address_node != null && gzh_node == null && success_click_btn_count == 0){
                clickNode(address_node,delay_after_click);success_click_btn_count++;
                run_msg = "检测到“通讯录”node，但没有检测到“公众号”node，应该是在非“通讯录”页面，故点击“通讯录”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(address_node != null && gzh_node != null && success_click_btn_count == 0){
                clickNode(gzh_node,delay_after_click);success_click_btn_count++;
                run_msg = "检测到“通讯录”node 和 “公众号”node，应该是在“通讯录”页面，故点击“公众号”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(ui_title.equalsIgnoreCase("当前所在页面,公众号") && search_node != null && success_click_btn_count == 0){
                clickNode(search_node,delay_after_click);success_click_btn_count++;
                run_msg = "当前所在页面,公众号，点击“搜索”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(ui_title.isEmpty() && edit_search_node !=  null && success_click_btn_count == 0){
                edit_search_node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", gzh_name);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                boolean f = edit_search_node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                success_click_btn_count++;
                run_msg = "当前所在公众号“搜索”页面，在Edit输入要查找的公众号=" + gzh_name  + ",success_click_btn_count=" + String.valueOf(success_click_btn_count);
                run_msg += ",粘贴是否成功标识 f=" + String.valueOf(f);
            }
            if(ui_title.isEmpty() && gzh_account_node !=  null && success_click_btn_count == 0){
                clickNode(gzh_account_node,delay_after_click);success_click_btn_count++;
                run_msg = "当前所在公众号“搜索”页面，已查找到公众号=" + gzh_name + "并点击,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(ui_title.isEmpty() && NodeFunc.getText(edit_search_node).equalsIgnoreCase(gzh_name) && no_result_node !=  null && success_click_btn_count == 0){
                clickNode(gzh_account_node,delay_after_click);success_click_btn_count++;
                run_msg = "当前所在公众号“搜索”页面，未查找到公众号=" + gzh_name + "，转入关注流程。,success_click_btn_count=" + String.valueOf(success_click_btn_count);
                hasReadMPAticle = false;
                hasMsgSend = true;//有待发送的信息
                MsgReadySend.add(sname);//第一个参数是接受人，其余的参数是待发送的信息
                MsgReadySend.add(run_msg);
            }
            //region 当前所在页面是 MP_Account_Name 的图文信息界面，按照单图文、多图文头条、多图文其他条的顺序进行阅读
            if(ui_title.equalsIgnoreCase("当前所在页面,与" + gzh_name + "的聊天") && top_left_node !=  null && success_click_btn_count == 0){
                this.printNodeInfo(linearLayout_node);
                if(msg_button_node != null && success_click_btn_count == 0){
                    clickNode(msg_button_node,delay_after_click);success_click_btn_count++;
                    LogToFile.write("当前所在页面,与" + gzh_name + "的聊天,发现“消息”按钮，点击。success_click_btn_count=" +  String.valueOf(success_click_btn_count));
                }
                if(switch2keyboard_node != null && success_click_btn_count == 0){
                    clickNode(switch2keyboard_node,delay_after_click);success_click_btn_count++;
                    LogToFile.write("当前所在页面,与" + gzh_name + "的聊天,发现“切换到键盘”按钮，点击。success_click_btn_count=" +  String.valueOf(success_click_btn_count));
                }
                if(gzh_edittext_node != null && success_click_btn_count == 0){
                    LogToFile.write("当前所在页面,与" + gzh_name + "的聊天,发现“EditText”输入框，输入要阅读公众号参数。");
                    paste_sendMsg(gzh_edittext_node,MP_Account_Name);success_click_btn_count++;
                }
                if(single_article_node_count > 0 && success_click_btn_count == 0){//发现有单图文，开始点击阅读单图文
                    LogToFile.write("在图文界面发现单图文 single_article_node_count=" +  String.valueOf(single_article_node_count));
                    for(AccessibilityNodeInfo node : single_article_node_list){
                        if(!NodeFunc.getText(node).isEmpty() && !Readed_Text_List.contains(node)) {
                            LogToFile.write("公众号：" + gzh_name + " ,阅读单图文：" + NodeFunc.getText(node) + ",等待 " + String.valueOf(delay_after_read_click) + " 毫秒。");
                            clickNode(node,delay_after_read_click);success_click_btn_count++;Readed_Text_List.add(node);break;
                        }
                    }
                }
                if(mularticle_header_node_count > 0 && success_click_btn_count == 0){//发现有多图文，开始点击阅读多图文的头条
                    LogToFile.write("在图文界面发现多图文,多图文的头条 mularticle_header_node_count=" +  String.valueOf(mularticle_header_node_count));
                    for(AccessibilityNodeInfo node : mularticle_header_node_list){
                        if(!NodeFunc.getText(node).isEmpty() && !Readed_Text_List.contains(node)) {
                            LogToFile.write("公众号：" + gzh_name + " ,阅读多图文的头条：" + NodeFunc.getText(node) + ",等待 " + String.valueOf(delay_after_read_click) + " 毫秒。");
                            clickNode(node,delay_after_read_click);success_click_btn_count++;Readed_Text_List.add(node);break;
                        }

                    }
                }
                if(mularticle_other_node_count > 0 && success_click_btn_count == 0){//发现有多图文，开始点击阅读多图文的其他条
                    LogToFile.write("在图文界面发现多图文,多图文的其他条 mularticle_other_node_count=" +  String.valueOf(mularticle_other_node_count));
                    for(AccessibilityNodeInfo node : mularticle_other_node_list){
                        if(!NodeFunc.getText(node).isEmpty()  && !Readed_Text_List.contains(node)) {
                            LogToFile.write("公众号：" + gzh_name + " ,阅读多图文的其他条：" + NodeFunc.getText(node) + ",等待 " + String.valueOf(delay_after_read_click) + " 毫秒。");
                            clickNode(node,delay_after_read_click);success_click_btn_count++;Readed_Text_List.add(node);break;
                        }
                    }
                }
                if(tuwen_count == 0 && success_click_btn_count == 0){
                    LogToFile.write("在图文界面未发现图文信息,转入“查看历史信息” tuwen_count=" +  String.valueOf(tuwen_count));
                    clickNode(edit_search_node,delay_after_back_click);success_click_btn_count++;
                }
            }
            if(ui_title.equalsIgnoreCase("当前所在页面,与" + gzh_name + "的聊天") && more_node !=  null && history_msg_node != null &&  success_click_btn_count == 0){
                LogToFile.write("在“查看历史信息”界面，点击进入“查看历史信息”按钮， tuwen_count=" +  String.valueOf(tuwen_count));
                clickNode(history_msg_node,delay_after_back_click);success_click_btn_count++;
            }
            //endregion
            if(backbtn_node != null && TuWenReadUI_WebPage_node != null && success_click_btn_count == 0){//发现返回按钮
                LogToFile.write("在图文阅读界面点击“返回”按钮。");
                clickNode(backbtn_node,delay_after_back_click);success_click_btn_count++;
                LogToFile.write("公众号：" + this.MP_Account_Name + " ,点击“返回”,等待 " + String.valueOf(delay_after_read_click) + " 毫秒。");
            }
            if(Readed_Text_List.size() >= tuwen_count && tuwen_count > 0 && success_click_btn_count == 0){
                String msg = "阅读公众号=" + gzh_name + " 图文信息结束，共阅读 " + String.valueOf(tuwen_count) + " 条。";
                LogToFile.write(msg);
                LogToFile.toast(msg);
                hasReadMPAticle = false;
                hasMsgSend = true;//有待发送的信息
                MsgReadySend.add(sname);//第一个参数是接受人，其余的参数是待发送的信息
                MsgReadySend.add(msg);
                int i = 1;
                for(AccessibilityNodeInfo node : Readed_Text_List){
                    LogToFile.write("标题" + String.valueOf(i) + ":" + NodeFunc.getText(node));
                    MsgReadySend.add("标题" + String.valueOf(i) + ":" + NodeFunc.getText(node));
                    i++;
                }
                success_click_btn_count++;
            }
            if(backbtn_node != null && success_click_btn_count == 0){//发现返回按钮
                LogToFile.write("未执行任何命令，且当前页面有“返回”按钮，那就点击吧。");
                clickNode(backbtn_node,delay_after_back_click);success_click_btn_count++;
            }
            if(success_click_btn_count == 0){
                //如果还是没有任何命令被执行,就执行返回命令
                clickBackNode(backbtn_node,delay_after_back_click);
            }
            //endregion
        }
        catch (Exception e){
            LogToFile.write("dealReadMPArticle Fail!Error=" + e.getMessage());
            LogToFile.toast("dealReadMPArticle Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write(run_msg);
            LogToFile.write("接到阅读公众号命令，阅读公众号文章 运行结束,（重写的方法）dealReadMPArticle run over.success_click_btn_count=" + String.valueOf(success_click_btn_count) + ",ui_title="+ ui_title);
        }
    }
    // region 接到阅读公众号命令，阅读公众号文章--在历史记录
    //endregion
    public void dealReadGzhHistoryArticle(AccessibilityEvent event){
        String msg_date = MP_Article_Date;//DateUtils.getCurrentTime("yyyy年MM月dd日");
        LogToFile.write("接到阅读公众号命令，阅读公众号文章开始运行,（通过阅读历史信息）dealReadGzhHistoryArticle is running...msg_date=" + msg_date );
        //region 参数定义
        int success_click_btn_count = 0;
        int delay_after_click = 200;//click后延迟 500 毫秒
        int delay_after_back_click = 1000;//back click后延迟 1000 毫秒
        int delay_after_read_click = 6000;//阅读click后延迟 6000  毫秒
        AccessibilityNodeInfo linearLayout_node = null;
        AccessibilityNodeInfo root_node = null;
        AccessibilityNodeInfo wechat_node = null;
        AccessibilityNodeInfo search_node = null;
        AccessibilityNodeInfo edit_search_node = null;
        AccessibilityNodeInfo gzh_account_node = null;
        AccessibilityNodeInfo no_result_node = null;
        AccessibilityNodeInfo more_node = null;
        AccessibilityNodeInfo history_msg_node = null;
        AccessibilityNodeInfo chat_info__node = null;
        AccessibilityNodeInfo send_msg__node = null;
        AccessibilityNodeInfo gzh_account_view_node = null;
        List<AccessibilityNodeInfo> msg_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> single_article_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> mularticle_header_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> mularticle_other_node_list = new ArrayList<AccessibilityNodeInfo>();
        AccessibilityNodeInfo backbtn_node = null;
        AccessibilityNodeInfo address_node = null;//“通讯录”node
        AccessibilityNodeInfo gzh_node = null;//“公众号”node
        List<String> ui_Title_List = new ArrayList<>(Arrays.asList("当前所在页面,新的朋友","当前所在页面,朋友验证","当前所在页面,详细资料"));
        int tuwen_count = 0;
        int msg_count = 0;
        int single_article_node_count = 0;
        int mularticle_header_node_count = 0;
        int mularticle_other_node_count = 0;
        String ui_title = "";
        String run_msg = "";
        String info = "";
        //endregion
        try{
            //region 参数初始化
            root_node = this.getRootInActiveWindow();
            linearLayout_node = NodeFunc.getLinearLayoutNodeinfo(root_node);
            address_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_BottomMenu_Btn_ID,NodeFunc.Wx_BottomMenu_Text[1],NodeFunc.Wx_BottomMenu_Btn_Class);//“通讯录”node
            gzh_node = NodeFunc.findNodebyText_Class(linearLayout_node,"公众号","android.widget.TextView");//“公众号”node
            search_node = NodeFunc.findNodebyClass_Desc(linearLayout_node,"android.widget.TextView","搜索");
            edit_search_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/hb","搜索","android.widget.EditText");
            gzh_account_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/v9",MP_Account_Name,"android.widget.TextView");
            no_result_node  = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/va","无结果","android.widget.TextView");
            more_node  = NodeFunc.findNodebyClass_Desc(linearLayout_node,"android.widget.TextView","更多");
            chat_info__node = NodeFunc.findNodebyClass_Desc(linearLayout_node,"android.widget.TextView","聊天信息");
            history_msg_node  = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"android:id/title","查看历史消息","android.widget.TextView");
            gzh_account_view_node = NodeFunc.findNodebyClass_Desc(linearLayout_node,"android.view.View",MP_Account_Name);
            send_msg__node = NodeFunc.findNodebyClass_Desc(linearLayout_node,"android.view.View","发消息");
            single_article_node_count = NodeFunc.findNodebyID_Class(linearLayout_node,NodeFunc.Wx_Single_Article_ID,NodeFunc.Wx_Single_Article_Class,single_article_node_list);
            mularticle_header_node_count = NodeFunc.findNodebyID_Class(linearLayout_node,NodeFunc.Wx_MulArticle_Header_ID,NodeFunc.Wx_MulArticle_Header_Class,mularticle_header_node_list);
            mularticle_other_node_count = NodeFunc.findNodebyID_Class(linearLayout_node,NodeFunc.Wx_MulArticle_Other_ID,NodeFunc.Wx_MulArticle_Other_Class,mularticle_other_node_list);
            //特定公众号图文阅读界面，“网页由 mp.weixin.qq.com 提供”参数
            AccessibilityNodeInfo TuWenReadUI_WebPage_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_TuWenReadUI_WebPage_ID,NodeFunc.Wx_TuWenReadUI_WebPage_Text,NodeFunc.Wx_TuWenReadUI_WebPage_Class);
            tuwen_count = single_article_node_count + mularticle_header_node_count + mularticle_other_node_count;
            backbtn_node = NodeFunc.findNodebyClass_Desc(linearLayout_node,NodeFunc.Wx_BackBtn_Class,NodeFunc.Wx_BackBtn__Desc);

            ui_title = NodeFunc.getContentDescription(root_node);//取当前页面描述，标题
            //endregion
            //region 验证部分，测试跟踪用
            info = "single_article_node_count=" + String.valueOf(single_article_node_count) + ",mularticle_header_node_count=" + String.valueOf(mularticle_header_node_count);
            info += ",mularticle_other_node_count=" + String.valueOf(mularticle_other_node_count);
            info += ",ui_title=" + ui_title;
            if (gzh_node != null) info += ",gzh_node=" + NodeFunc.getText(gzh_node);
            if (backbtn_node != null)
                info += ",backbtn_node=" + NodeFunc.getText(backbtn_node);
            if (address_node != null)
                info += ",address_node=" + NodeFunc.getText(address_node);
            //LogToFile.write(info);
            //endregion3222222222222222222222222222222222222222222222222222222222222222222222222222222222
            //region 逻辑执行部分
            //this.printNodeInfo(linearLayout_node);
            if(address_node != null && gzh_node == null && success_click_btn_count == 0){
                clickNode(address_node,delay_after_click);success_click_btn_count++;
                run_msg = "检测到“通讯录”node，但没有检测到“公众号”node，应该是在非“通讯录”页面，故点击“通讯录”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(address_node != null && gzh_node != null && success_click_btn_count == 0){
                clickNode(gzh_node,delay_after_click);success_click_btn_count++;
                run_msg = "检测到“通讯录”node 和 “公众号”node，应该是在“通讯录”页面，故点击“公众号”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(ui_title.equalsIgnoreCase("当前所在页面,公众号") && search_node != null && success_click_btn_count == 0){
                clickNode(search_node,delay_after_click);success_click_btn_count++;
                run_msg = "当前所在页面,公众号，点击“搜索”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(ui_title.isEmpty() && edit_search_node !=  null && success_click_btn_count == 0){
                edit_search_node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", this.MP_Account_Name);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                boolean f = edit_search_node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                success_click_btn_count++;
                run_msg = "当前所在公众号“搜索”页面，在Edit输入要查找的公众号=" + this.MP_Account_Name  + ",success_click_btn_count=" + String.valueOf(success_click_btn_count);
                run_msg += ",粘贴是否成功标识 f=" + String.valueOf(f);
            }
            if(ui_title.isEmpty() && gzh_account_node !=  null && success_click_btn_count == 0){
                clickNode(gzh_account_node,delay_after_click);success_click_btn_count++;
                run_msg = "当前所在公众号“搜索”页面，已查找到公众号=" + MP_Account_Name + "并点击,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(ui_title.isEmpty() && NodeFunc.getText(edit_search_node).equalsIgnoreCase(MP_Account_Name) && no_result_node !=  null && success_click_btn_count == 0){
                clickNode(gzh_account_node,delay_after_click);success_click_btn_count++;
                run_msg = "当前所在公众号“搜索”页面，未查找到公众号=" + MP_Account_Name + "，转入关注流程。,success_click_btn_count=" + String.valueOf(success_click_btn_count);
                hasReadMPAticle = false;
                hasMsgSend = true;//有待发送的信息
                MsgReadySend.add(sname);//第一个参数是接受人，其余的参数是待发送的信息
                MsgReadySend.add(run_msg);
            }
            if(ui_title.equalsIgnoreCase("当前所在页面,与" + MP_Account_Name + "的聊天") && chat_info__node != null && success_click_btn_count == 0){
                clickNode(chat_info__node,delay_after_click);success_click_btn_count++;
                run_msg = "当前在公众号图文信息界面，点击进入公众号详细资料界面，success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(ui_title.equalsIgnoreCase("当前所在页面," + MP_Account_Name) && history_msg_node != null && success_click_btn_count == 0){
                clickNode(history_msg_node,delay_after_read_click);success_click_btn_count++;runcontent = "当前在公众号详细资料界面,点击“查看历史信息”node";runcount = 15;
                run_msg = "当前在公众号详细资料界面,点击“查看历史信息”node，success_click_btn_count=" + String.valueOf(success_click_btn_count + ",delay_after_read_click=" + String.valueOf(delay_after_read_click));
            }
            if(runcontent.equalsIgnoreCase("当前在公众号详细资料界面,点击“查看历史信息”node") && success_click_btn_count == 0 && runcount > 0){
                if(gzh_account_view_node != null && send_msg__node != null){
                    runcount = 0;success_click_btn_count = 1;
                }
                else{
                    runcount--;success_click_btn_count++;
                    FuncTools.delay(delay_after_back_click);
                }
                run_msg = "当前在公众号历史信息界面，等待加载完成，success_click_btn_count=" + String.valueOf(success_click_btn_count) + ",runcount=" + String.valueOf(runcount);
            }
            if(ui_title.isEmpty() && gzh_account_view_node != null && send_msg__node != null && success_click_btn_count == 0){
                NodeFunc.getAllNodeInfo(linearLayout_node,msg_node_list);
                int msg_date_count = 0;
                boolean f = false;
                for(AccessibilityNodeInfo sub_node : msg_node_list){
                    if(NodeFunc.getContentDescription(sub_node).equalsIgnoreCase(msg_date)) msg_date_count++;
                }
                if(Readed_Text_List.size() >= msg_date_count ){//指定日期的公众号图文信息已经阅读完毕
                    String msg = "阅读公众号=" + this.MP_Account_Name + " 图文信息结束，发现 " +  String.valueOf(msg_date_count) + " 条,阅读 " + String.valueOf(Readed_Text_List.size()) + " 条。";
                    hasReadMPAticle = false;
                    hasMsgSend = true;//有待发送的信息
                    MsgReadySend.add(sname);//第一个参数是接受人，其余的参数是待发送的信息
                    MsgReadySend.add(msg);
                    int i = 1;
                    for(AccessibilityNodeInfo node : Readed_Text_List){
                        LogToFile.write("标题" + String.valueOf(i) + ":" + NodeFunc.getContentDescription(node));
                        MsgReadySend.add("标题" + String.valueOf(i) + ":" + NodeFunc.getContentDescription(node));
                        i++;
                    }
                    success_click_btn_count++;
                }
                else {
                    for (int i = 0; i < msg_node_list.size(); i++) {
                        //LogToFile.write("msg_node_list.get(" + String.valueOf(i) + ")=" + NodeFunc.getContentDescription(msg_node_list.get(i)));
                        if (NodeFunc.getContentDescription(msg_node_list.get(i)).equalsIgnoreCase(msg_date)) {
                            AccessibilityNodeInfo node = msg_node_list.get(i - 1);
                            //LogToFile.write("node=" + NodeFunc.getContentDescription(node) + ",Readed_Text_List.size()=" + String.valueOf(Readed_Text_List.size()));
                            if (!Readed_Text_List.contains(node)) {
                                f = clickNode(node, delay_after_read_click);success_click_btn_count++;
                                Readed_Text_List.add(node);break;
                            }
                        }
                    }
                }
                run_msg = "当前在公众号“历史信息”界面，success_click_btn_count=" + String.valueOf(success_click_btn_count) + ",f=" + String.valueOf(f);
                run_msg += ",msg_date_count=" + String.valueOf(msg_date_count) + ",Readed_Text_List.size()=" + String.valueOf(Readed_Text_List.size());
            }
            if(backbtn_node != null && TuWenReadUI_WebPage_node != null && success_click_btn_count == 0){//发现返回按钮
                LogToFile.write("在图文阅读界面点击“返回”按钮。");
                AccessibilityNodeInfo webview = null;
                List<AccessibilityNodeInfo> node_list = new ArrayList<AccessibilityNodeInfo>();
                NodeFunc.getAllNodeInfo(linearLayout_node,node_list);
                for(AccessibilityNodeInfo node : node_list){
                    if(NodeFunc.getClassName(node).equalsIgnoreCase("android.webkit.WebView")){
                        if(node.isScrollable()){ webview = node;LogToFile.write("webview=" + webview.toString());break;}
                    }
                }
                if(clickNode(webview,AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,0)){
                    FuncTools.delay(delay_after_back_click);
                }
                else {
                    clickNode(backbtn_node, delay_after_back_click);
                }
                success_click_btn_count++;
                LogToFile.write("公众号：" + this.MP_Account_Name + " ,点击“返回”,等待 " + String.valueOf(delay_after_read_click) + " 毫秒。");
            }
            if(backbtn_node != null && success_click_btn_count == 0 ){//发现返回按钮
                LogToFile.write("未执行任何命令，且当前页面有“返回”按钮，那就点击吧。");
                runcontent = "未执行任何命令，且当前页面有“返回”按钮，那就点击吧。";
                clickNode(backbtn_node,delay_after_back_click);success_click_btn_count++;
            }
            if(success_click_btn_count == 0){
                //如果还是没有任何命令被执行,就执行返回命令
                //clickBackNode(backbtn_node,delay_after_back_click);
            }
            //endregion
        }
        catch (Exception e){
            LogToFile.write("dealReadGzhHistoryArticle Fail!Error=" + e.getMessage());
            LogToFile.toast("dealReadGzhHistoryArticle Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write(run_msg);
            LogToFile.write("接到阅读公众号命令，阅读公众号文章 运行结束,（通过阅读历史信息）dealReadGzhHistoryArticle run over.success_click_btn_count=" + String.valueOf(success_click_btn_count) + ",ui_title="+ ui_title);
        }
    }
    // region 处理待发送信息
    //endregion
    public void dealMsgReadySend(AccessibilityEvent event){
        LogToFile.write("处理待发送信息 开始运行,dealMsgReadySend is running...");
        //region 参数定义
        int success_click_btn_count = 0;
        int delay_after_click = 200;//click后延迟 500 毫秒
        int delay_after_back_click = 1000;//back click后延迟 1000 毫秒
        int delay_after_read_click = 6000;//阅读click后延迟 6000  毫秒
        AccessibilityNodeInfo linearLayout_node = null;
        AccessibilityNodeInfo root_node = null;
        AccessibilityNodeInfo search_node = null;
        AccessibilityNodeInfo edit_search_node = null;
        AccessibilityNodeInfo friend_node = null;
        AccessibilityNodeInfo backbtn_node = null;
        AccessibilityNodeInfo address_node = null;//“通讯录”node
        AccessibilityNodeInfo gzh_node = null;//“公众号”node
        List<String> ui_Title_List = new ArrayList<>(Arrays.asList("当前所在页面,新的朋友","当前所在页面,朋友验证","当前所在页面,详细资料"));
        String ui_title = "";
        String run_msg = "";
        String info = "";
        //endregion
        try{
            //region 参数初始化
            root_node = this.getRootInActiveWindow();
            linearLayout_node = NodeFunc.getLinearLayoutNodeinfo(root_node);
            address_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,NodeFunc.Wx_BottomMenu_Btn_ID,NodeFunc.Wx_BottomMenu_Text[1],NodeFunc.Wx_BottomMenu_Btn_Class);//“通讯录”node
            gzh_node = NodeFunc.findNodebyText_Class(linearLayout_node,"公众号","android.widget.TextView");//“公众号”node
            search_node = NodeFunc.findNodebyClass_Desc(linearLayout_node,"android.widget.TextView","搜索");
            edit_search_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/hb","搜索","android.widget.EditText");
            friend_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/jy",MsgReadySend.get(0),"android.widget.TextView");
            ui_title = NodeFunc.getContentDescription(root_node);//取当前页面描述，标题
            //endregion
            //region 逻辑执行部分
            if(address_node != null && gzh_node == null && success_click_btn_count == 0){
                clickNode(address_node,delay_after_click);success_click_btn_count++;
                run_msg = "检测到“通讯录”node，但没有检测到“公众号”node，应该是在非“通讯录”页面，故点击“通讯录”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(address_node != null && gzh_node != null && search_node != null && success_click_btn_count == 0){
                clickNode(search_node,delay_after_click);success_click_btn_count++;
                run_msg = "检测到“通讯录”node 和 “公众号”node，点击“搜索”node,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(ui_title.isEmpty() && edit_search_node !=  null && success_click_btn_count == 0){
                edit_search_node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", MsgReadySend.get(0));
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                boolean f = edit_search_node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                success_click_btn_count++;
                run_msg = "当前所在联系人“搜索”页面，在Edit输入要查找的联系人=" + MsgReadySend.get(0)  + ",success_click_btn_count=" + String.valueOf(success_click_btn_count);
                run_msg += ",粘贴是否成功标识 f=" + String.valueOf(f);
            }
            if(ui_title.isEmpty() && friend_node !=  null && success_click_btn_count == 0){
                clickNode(friend_node,delay_after_click);success_click_btn_count++;
                run_msg = "当前所在联系人“搜索”页面，已查找到联系人=" + MsgReadySend.get(0) + "并点击,success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(ui_title.equalsIgnoreCase("当前所在页面,与" + MsgReadySend.get(0) + "的聊天") && success_click_btn_count == 0){
                for(String msg : MsgReadySend){//开始发送信息
                    if(sendMsg(linearLayout_node,msg)) success_click_btn_count++;
                }
                if(success_click_btn_count == 0) success_click_btn_count = 1;
                run_msg = "开始发送信息到联系人=" + MsgReadySend.get(0) + ",发送数量 success_click_btn_count=" + String.valueOf(success_click_btn_count);
            }
            if(ui_title.equalsIgnoreCase("当前所在页面,与" + MsgReadySend.get(0) + "的聊天") && success_click_btn_count >= 2){
                hasMsgSend = false;MsgReadySend.clear();
                AccessibilityNodeInfo chat_node = NodeFunc.findNodebyClass_Desc(NodeFunc.getLinearLayoutNodeinfo(this.getRootInActiveWindow()),"android.widget.TextView","聊天信息");
                clickNode(chat_node,delay_after_click);
                run_msg = "成功发送信息到联系人=" + MsgReadySend.get(0) + ",成功发送数量>=2, success_click_btn_count=" + String.valueOf(success_click_btn_count);
                LogToFile.toast(run_msg);
            }
            if(backbtn_node != null && success_click_btn_count == 0){//发现返回按钮
                LogToFile.write("未执行任何命令，且当前页面有“返回”按钮，那就点击吧。");
                clickNode(backbtn_node,delay_after_back_click);success_click_btn_count++;
            }
            if(success_click_btn_count == 0){
                //如果还是没有任何命令被执行,就执行返回命令
                clickBackNode(backbtn_node,delay_after_back_click);
            }
            //endregion
        }
        catch (Exception e){
            LogToFile.write("dealMsgReadySend Fail!Error=" + e.getMessage());
            LogToFile.toast("dealMsgReadySend Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write(run_msg);
            LogToFile.write("处理待发送信息 运行结束,dealMsgReadySend run over.success_click_btn_count=" + String.valueOf(success_click_btn_count) + ",ui_title="+ ui_title);
        }
    }
    //region 发送信息
    //endregion
    public boolean sendMsg(AccessibilityNodeInfo node,String msg){
        boolean f = false;
        List<AccessibilityNodeInfo> all_node_list = new ArrayList<AccessibilityNodeInfo>();
        try{
            if(node != null){
                this.printNodeInfo(node,all_node_list,false);
                for(AccessibilityNodeInfo nodeInfo : all_node_list){
                    if ("android.widget.EditText".equalsIgnoreCase(NodeFunc.getClassName(nodeInfo))) {
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                        ClipData clip = ClipData.newPlainText("label", msg);
                        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboardManager.setPrimaryClip(clip);
                        if(nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE)){
                            send();
                            FuncTools.delay(100);
                            f = true;
                        }
                    }
                }
            }
        }
        catch (Exception e){
            LogToFile.write("sendMsg Fail!Error=" + e.getMessage());
            LogToFile.toast("sendMsg Fail!Error=" + e.getMessage());
        }finally {
            return f;
        }
    }
    //region 粘贴并发送信息
    //endregion
    public boolean paste_sendMsg(AccessibilityNodeInfo node,String msg){
        boolean f = false;
        try{
            if(node != null){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Bundle args = new Bundle();
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            msg);
                    f = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                } else {
                    ClipData data = ClipData.newPlainText("reply", msg);
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(data);
                    node.performAction(AccessibilityNodeInfo.ACTION_FOCUS); // 获取焦点
                    f = node.performAction(AccessibilityNodeInfo.ACTION_PASTE); // 执行粘贴
                }
                if(f){send();FuncTools.delay(100);f = true;}
            }
        }
        catch (Exception e){
            LogToFile.write("sendMsg Fail!Error=" + e.getMessage());
            LogToFile.toast("sendMsg Fail!Error=" + e.getMessage());
        }finally {
            return f;
        }
    }
    //region 点击“返回”按钮
    //endregion
    public int clickBackNode(AccessibilityNodeInfo backbtn_node,int delay_after_click){
        int flags = 0;
        try{
            if(backbtn_node != null){
                if(clickNode(backbtn_node,delay_after_click)){
                    FuncTools.delay(delay_after_click);
                    flags = 1;
                }
                else{
                    this.performBackClick(delay_after_click);
                    flags = 2;
                }
            }
            else{
                this.performBackClick(delay_after_click);
                flags = 3;
            }
        }
        catch (Exception e){
            LogToFile.write("clickBackNode Fail!Error=" + e.getMessage());
            LogToFile.toast("clickBackNode Fail!Error=" + e.getMessage());
        }finally {
            return flags;
        }
    }
    //region 点击按钮
    //endregion
    public boolean clickNode(AccessibilityNodeInfo node,int delay_after_click){
        boolean f = false;
        try{
            if(node != null){
                AccessibilityNodeInfo click_node = NodeFunc.getClickableParentNode(node);
                if(click_node != null){
                    if(NodeFunc.getClassName(click_node).equalsIgnoreCase("android.view.View") && RootUtil.is_root()){
                        Rect rect = new Rect(0,0,1,1);
                        click_node.getBoundsInScreen(rect);
                        RootShellCmd.execShellCmd("input tap " + String.valueOf(rect.centerX()) + " " + String.valueOf(rect.centerY()) + " ");
                    }
                    else {
                        f = click_node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    FuncTools.delay(delay_after_click);
                }
            }
        }
        catch (Exception e){
            LogToFile.write("clickNode Fail!Error=" + e.getMessage());
            LogToFile.toast("clickNode Fail!Error=" + e.getMessage());
        }finally {
            return f;
        }
    }
    //region 点击按钮
    //endregion
    public boolean clickNode(AccessibilityNodeInfo node, int action, int delay_after_click){
        boolean f = false;
        try{
            if(node != null){
                AccessibilityNodeInfo click_node = NodeFunc.getClickableParentNode(node);
                if(click_node != null){
                    f = click_node.performAction(action);
                    FuncTools.delay(delay_after_click);
                }
            }
        }
        catch (Exception e){
            LogToFile.write("clickNode Fail!Error=" + e.getMessage());
            LogToFile.toast("clickNode Fail!Error=" + e.getMessage());
        }finally {
            return f;
        }
    }
    //region 获取底栏按钮 node NodeFunc.Wx_BottomMenu_Text[1]
    //endregion
    public AccessibilityNodeInfo getBottomMenuBtn(String BottomMenu_Text){
        LogToFile.write("获取底栏按钮开始运行,getBottomMenuBtn is running...");
        int success_click_btn_count = 0;
        int delay_after_click = 200;//click后延迟200毫秒
        int max_run_count = 10;
        int run_count = 0;
        List<AccessibilityNodeInfo> all_node_list = new ArrayList<AccessibilityNodeInfo>();
        AccessibilityNodeInfo linearLayout_node = null;
        AccessibilityNodeInfo backbtn_node = null;
        AccessibilityNodeInfo address_node = null;
        String ui_title = "";

        try{
            AccessibilityNodeInfo old_node = this.getRootInActiveWindow();
            linearLayout_node = NodeFunc.getLinearLayoutNodeinfo(this.getRootInActiveWindow());
            backbtn_node = NodeFunc.getBackBtnNodeinLinearLayout(linearLayout_node);
            address_node = NodeFunc.getBottomBtnNodeinLinearLayout(linearLayout_node,BottomMenu_Text);
            while(address_node == null && run_count<max_run_count){
                if(old_node.equals(this.getRootInActiveWindow())){
                    old_node = this.getRootInActiveWindow();
                    if(backbtn_node != null){
                        AccessibilityNodeInfo click_node = NodeFunc.getClickableParentNode(backbtn_node);
                        if(click_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
                            FuncTools.delay(delay_after_click);
                        }
                        else this.performBackClick(delay_after_click);
                    }
                    else{
                        this.performBackClick(delay_after_click);
                    }
                }
                else FuncTools.delay(delay_after_click);

                linearLayout_node = NodeFunc.getLinearLayoutNodeinfo(this.getRootInActiveWindow());
                backbtn_node = NodeFunc.getBackBtnNodeinLinearLayout(linearLayout_node);
                address_node = NodeFunc.getBottomBtnNodeinLinearLayout(linearLayout_node,NodeFunc.Wx_BottomMenu_Text[1]);
                run_count++;
            }

        }
        catch (Exception e){
            LogToFile.write("getBottomMenuBtn Fail!Error=" + e.getMessage());
            LogToFile.toast("getBottomMenuBtn Fail!Error=" + e.getMessage());
        }finally {
            if(address_node != null) ui_title = NodeFunc.getText(address_node);
            LogToFile.write("获取底栏按钮运行结束,getBottomMenuBtn run over.run_count=" + String.valueOf(run_count) + ",BottomMenu_Text="+ ui_title);
            return address_node;
        }
    }
    //region 获取当前页面非空标题
    //endregion
    public String  getNotEmptyUITitle(){
        //LogToFile.write("获取当前页面非空标题开始运行,getUITitle is running...");
        int delay_after_click = 200;//click后延迟200毫秒
        int max_run_count = 10;
        int run_count = 0;
        String ui_title = "";

        try{
            AccessibilityNodeInfo old_node = this.getRootInActiveWindow();
            ui_title = NodeFunc.getContentDescription(old_node);
            run_count = 0;
            //LogToFile.write("run_count=" + String.valueOf(run_count) + ",ui_title="+ ui_title);
            while(ui_title.isEmpty() && run_count < max_run_count){
                if(old_node.equals(this.getRootInActiveWindow())){
                    old_node = this.getRootInActiveWindow();
                    this.performBackClick(delay_after_click);
                }
                else{
                    FuncTools.delay(delay_after_click);
                }
                ui_title = NodeFunc.getContentDescription(this.getRootInActiveWindow());
                run_count++;
            }
        }
        catch (Exception e){
            LogToFile.write("getNotEmptyUITitle Fail!Error=" + e.getMessage());
            LogToFile.toast("getNotEmptyUITitle Fail!Error=" + e.getMessage());
        }finally {
            //LogToFile.write("获取当前页面非空标题运行结束,getNotEmptyUITitle run over.run_count=" + String.valueOf(run_count) + ",ui_title="+ ui_title);
            return ui_title;
        }
    }
    //region 判断是否有“切换到键盘”按钮，如有切换到输入界面
    //endregion
    public void switch2Keyboard(AccessibilityEvent event){
        LogToFile.write("判断是否有“切换到键盘”按钮，如有切换到输入界面 开始运行,switch2Keyboard is running...");
        int delay_after_click = 200;//click后延迟200毫秒
        AccessibilityNodeInfo linearLayout_node = null;
        AccessibilityNodeInfo root_node = null;
        AccessibilityNodeInfo top_left_node = null;
        AccessibilityNodeInfo chat_node = null;
        AccessibilityNodeInfo switch2keyboard_node = null;
        AccessibilityNodeInfo switch2talk_node = null;
        String ui_title = "";
        String top_left_text = "";
        int success_click_btn_count = 0;
        try {
            itemNodeinfo = null;
            root_node = this.getRootInActiveWindow();
            ui_title = NodeFunc.getContentDescription(root_node);//取当前页面描述，标题
            top_left_text = NodeFunc.getTopLeftText(ui_title);
            linearLayout_node = NodeFunc.getLinearLayoutNodeinfo(root_node);
            top_left_node = NodeFunc.findNodebyID_Class_Text(linearLayout_node,"com.tencent.mm:id/h2",top_left_text,"android.widget.TextView");
            chat_node = NodeFunc.findNodebyClass_Desc(NodeFunc.getLinearLayoutNodeinfo(this.getRootInActiveWindow()),"android.widget.TextView","聊天信息");
            switch2keyboard_node = NodeFunc.findNodebyID_Class_Desc(linearLayout_node,"com.tencent.mm:id/a6z","android.widget.ImageButton","切换到键盘");
            switch2talk_node = NodeFunc.findNodebyID_Class_Desc(linearLayout_node,"com.tencent.mm:id/a6z","android.widget.ImageButton","切换到按住说话");
            if(switch2keyboard_node != null && success_click_btn_count == 0){
                LogToFile.write("检测到“切换到键盘”按钮，点击。");
                clickNode(switch2keyboard_node,delay_after_click);success_click_btn_count++;
            }
            if (!top_left_text.isEmpty() && top_left_node != null && chat_node != null && switch2talk_node != null && success_click_btn_count == 0 ) {
                //clickNode(chat_node,delay_after_click);success_click_btn_count++;
            }
            if(success_click_btn_count == 0){
                LogToFile.write("微信窗口尚未激活，等待 " + String.valueOf(delay_after_click)+ " 毫秒。ui_title=" + ui_title);
                FuncTools.delay(delay_after_click);
            }
        }
        catch (Exception e){
            LogToFile.write("switch2Keyboard Fail!Error=" + e.getMessage());
            LogToFile.toast("switch2Keyboard Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write("判断是否有“切换到键盘”按钮，如有切换到输入界面 运行结束,switch2Keyboard run over.");
        }
    }
    //region 跳转到聊天界面发送信息
    //endregion
    public void ChatWith(String Name,String msg){
        LogToFile.write("跳转到 聊天 " + Name + " 页面开始运行,ChatWith is running...");
        int success_click_btn_count = 0;
        int delay_after_click = 1000;//click后延迟 1000 毫秒
        int find_text_count = 0;
        List<AccessibilityNodeInfo> chat_record_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> all_node_list = new ArrayList<AccessibilityNodeInfo>();

        try{
            //this.printNodeInfo(chat_record_node_list);
            int node_count = NodeFunc.findNodebyID_Class(this.getRootInActiveWindow(),NodeFunc.Wx_ChatRecord_Node_ID,NodeFunc.Wx_ChatRecord_Node_Class,chat_record_node_list);

            if(node_count > 0){
                for(AccessibilityNodeInfo node : chat_record_node_list){
                    if(node.getText() != null) {
                        if(Name.equalsIgnoreCase(NodeFunc.getText(node))){
                            find_text_count++;
                            AccessibilityNodeInfo click_find_node = NodeFunc.getClickableParentNode(node);
                            if (click_find_node != null) {
                                if (click_find_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    LogToFile.write("进入“" + Name + "” 聊天页面成功。");
                                    FuncTools.delay(delay_after_click);
                                    success_click_btn_count++;
                                    this.printNodeInfo(this.getRootInActiveWindow(),all_node_list,false);
                                } else {
                                    LogToFile.write("进入“" + Name + "”聊天页面失败。");
                                }
                            }
                        }
                    }
                }
                if(success_click_btn_count == 1 && all_node_list.size() > 0){
                    for(AccessibilityNodeInfo node : all_node_list){
                        if(NodeFunc.Wx_Switch2KeyboardBtn_Desc.trim().equalsIgnoreCase(NodeFunc.getContentDescription(node)) && node.isClickable()) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            LogToFile.write("发现“切换到键盘”按钮。先切换到输入模式。nodeinfo=" + NodeFunc.getContentDescription(node));
                            FuncTools.delay(delay_after_click);
                            this.printNodeInfo(this.getRootInActiveWindow(),all_node_list,false);
                            break;
                        }
                    }
                    for(AccessibilityNodeInfo nodeInfo : all_node_list){
                        if ("android.widget.EditText".equalsIgnoreCase(NodeFunc.getClassName(nodeInfo))) {
                            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                            ClipData clip = ClipData.newPlainText("label", msg);
                            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboardManager.setPrimaryClip(clip);
                            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                            success_click_btn_count++;
                        }
                    }
                }
                if(success_click_btn_count == 2 && all_node_list.size() > 0){
                    send();
                }
            }

        }catch (Exception e){
            LogToFile.write("ChatWith Fail!Error=" + e.getMessage());
            LogToFile.toast("ChatWith Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write("跳转到 聊天 " + Name + " 页面 运行结束,ChatWith run over.success_click_btn_count=" + String.valueOf(success_click_btn_count));
        }
    }
    //region 返回到微信的主界面
    //endregion
    public boolean back2WeChatMain(AccessibilityNodeInfo node){
        boolean f = false;
        int count = 0;
        if(node != null){

            AccessibilityNodeInfo wx_node = NodeFunc.findNodebyID_Text(node,NodeFunc.Wx_BottomMenuSourceID,NodeFunc.Wx_BottomMenuName[0]);
            AccessibilityNodeInfo wx_main_dyh_node = NodeFunc.findNodebyID_Text(node,NodeFunc.Wx_MP_DYH_ID,NodeFunc.Wx_MP_DYH_Name);
            while(count < 10) {
                if(wx_node == null) {
                    LogToFile.write("不在微信4个主界面内，执行返回点击。count=" + String.valueOf(count));
                    this.performBackClick(100);
                    FuncTools.delay(NodeFunc.delay_after_click);
                }
                else {
                    if(wx_main_dyh_node == null){
                        LogToFile.write("在微信4个主界面内，执行点击返回主界面。count=" + String.valueOf(count));
                        wx_node = NodeFunc.clickParentNode(node,NodeFunc.Wx_BottomMenuSourceID,NodeFunc.Wx_BottomMenuName[0],true);
                    }
                    else break;
                }
                wx_node = NodeFunc.findNodebyID_Text(node,NodeFunc.Wx_BottomMenuSourceID,NodeFunc.Wx_BottomMenuName[0]);
                wx_main_dyh_node = NodeFunc.findNodebyID_Text(node,NodeFunc.Wx_MP_DYH_ID,NodeFunc.Wx_MP_DYH_Name);
                count ++;
            }
        }
        return f;
    }
    //region  * 遍历所有控件，找到头像Imagview，里面有对联系人的描述
    //endregion
    private void GetChatName(AccessibilityNodeInfo node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo node1 = node.getChild(i);
            if ("android.widget.ImageView".equals(node1.getClassName()) && node1.isClickable()) {
                //获取聊天对象,这里两个if是为了确定找到的这个ImageView是头像的
                if (!TextUtils.isEmpty(node1.getContentDescription())) {
                    ChatName = NodeFunc.getContentDescription(node1);
                    if (ChatName.contains("头像")) {
                        ChatName = ChatName.replace("头像", "");
                    }
                }

            }
            GetChatName(node1);
        }
    }
    //region    * 遍历所有控件:这里分四种情况
    /*
     * 文字聊天: 一个TextView，并且他的父布局是android.widget.RelativeLayout
     * 语音的秒数: 一个TextView，并且他的父布局是android.widget.RelativeLayout，但是他的格式是0"的格式，所以可以通过这个来区分
     * 图片:一个ImageView,并且他的父布局是android.widget.FrameLayout,描述中包含“图片”字样（发过去的图片），发回来的图片现在还无法监听
     * 表情:也是一个ImageView,并且他的父布局是android.widget.LinearLayout
     * 小视频的秒数:一个TextView，并且他的父布局是android.widget.FrameLayout，但是他的格式是00:00"的格式，所以可以通过这个来区分
     *
     * @param node
     */
    //endregion
    public void GetChatRecord(AccessibilityNodeInfo node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo nodeChild = node.getChild(i);

            //聊天内容是:文字聊天(包含语音秒数)
            if ("android.widget.TextView".equals(nodeChild.getClassName()) && "android.widget.RelativeLayout".equals(nodeChild.getParent().getClassName().toString())) {
                if (!TextUtils.isEmpty(nodeChild.getText())) {
                    String RecordText = nodeChild.getText().toString();
                    //这里加个if是为了防止多次触发TYPE_VIEW_SCROLLED而打印重复的信息
                    if (!RecordText.equals(ChatRecord)) {
                        ChatRecord = RecordText;
                        //判断是语音秒数还是正常的文字聊天,语音的话秒数格式为5"
                        if (ChatRecord.contains("\"")) {
                            LogToFile.write(ChatName + "发了一条" + ChatRecord + "的语音");
                            LogToFile.toast(ChatName + "发了一条" + ChatRecord + "的语音");
                        } else {
                            //这里在加多一层过滤条件，确保得到的是聊天信息，因为有可能是其他TextView的干扰，例如名片等
                            if (nodeChild.isLongClickable()) {
                                LogToFile.write(ChatName + "：" + ChatRecord);
                                LogToFile.toast(ChatName + "：" + ChatRecord);
                            }

                        }
                        return;
                    }
                }
            }

            //聊天内容是:表情
            if ("android.widget.ImageView".equals(nodeChild.getClassName()) && "android.widget.LinearLayout".equals(nodeChild.getParent().getClassName().toString())) {
                Toast.makeText(this, ChatName+"发的是表情", Toast.LENGTH_SHORT).show();
                LogToFile.write(ChatName + "：" + "发的是表情");
                LogToFile.toast(ChatName + "：" + "发的是表情");

                return;
            }

            //聊天内容是:图片
            if ("android.widget.ImageView".equals(nodeChild.getClassName())) {
                //安装软件的这一方发的图片（另一方发的暂时没实现）
                if("android.widget.FrameLayout".equals(nodeChild.getParent().getClassName().toString())){
                    if(!TextUtils.isEmpty(nodeChild.getContentDescription())){
                        if(NodeFunc.getContentDescription(nodeChild).contains("图片")){
                            LogToFile.write(ChatName + "：" + "发的是表情");
                            LogToFile.toast(ChatName + "：" + "发的是图片");
                        }
                    }
                }
            }

            //聊天内容是:小视频秒数,格式为00：00
            if ("android.widget.TextView".equals(nodeChild.getClassName()) && "android.widget.FrameLayout".equals(nodeChild.getParent().getClassName().toString())) {
                if (!TextUtils.isEmpty(nodeChild.getText())) {
                    String second = nodeChild.getText().toString().replace(":", "");
                    //正则表达式，确定是不是纯数字,并且做重复判断
                    if (second.matches("[0-9]+") && !second.equals(VideoSecond)) {
                        VideoSecond = second;
                        LogToFile.write(ChatName + "：" + "发了一段" + nodeChild.getText().toString() + "的小视频");
                        LogToFile.toast(ChatName + "：" + "发了一段" + nodeChild.getText().toString() + "的小视频");

                    }
                }

            }

            GetChatRecord(nodeChild);
        }
    }
    public static AutomationService getService() {
        return mService;
    }

    //region  寻找窗体中的“发送”按钮，并且点击。
    /**
     * 寻找窗体中的“发送”按钮，并且点击。
     */

    @SuppressLint("NewApi")
    //endregion
    private void send() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByText("发送");
            if (list != null && list.size() > 0) {
                for (AccessibilityNodeInfo n : list) {
                    if (n.getClassName().equals("android.widget.Button") && n.isEnabled()) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }

            } else {
                List<AccessibilityNodeInfo> liste = nodeInfo
                        .findAccessibilityNodeInfosByText("Send");
                if (liste != null && liste.size() > 0) {
                    for (AccessibilityNodeInfo n : liste) {
                        if (n.getClassName().equals("android.widget.Button") && n.isEnabled()) {
                            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
            }
            //this.performBackClick(500);
        }
    }
    //region 模拟back按键
    /**
     * 模拟back按键
     */
    //endregion
    private void pressBackButton() {
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("input keyevent " + KeyEvent.KEYCODE_BACK);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //region 拉起微信界面
    /**
     * 拉起微信界面
     *
     * @param event event
     */
    //endregion
    private void notifyWechat(AccessibilityEvent event) {
        String content = "";
        try {
            if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                Notification notification = (Notification) event.getParcelableData();
                if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && event.getPackageName().equals(NodeFunc.Wx_PackageName)) {
                    //if (notification.priority == Notification.PRIORITY_HIGH || notification.priority == Notification.PRIORITY_DEFAULT)//文字信息 PRIORITY_HIGH = 1;PRIORITY_DEFAULT = 0;//加好友信息 PRIORITY_MAX = 2
                    notification_text = notification.tickerText.toString();
                    hasNotification = true;
                }
                PendingIntent pendingIntent = notification.contentIntent;
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e){
            LogToFile.write("notifyWechat Fail!Error=" + e.getMessage() + ",content=" + content);
            LogToFile.toast("notifyWechat Fail!Error=" + e.getMessage() + ",content=" + content);
        }
    }
    //region 自动回复信息
    @SuppressLint("NewApi")
    //endregion
    private boolean auto_replay_msg(String msg) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {

            return findEditText(rootNode, msg);
        }
        return false;
    }

    //region 查找类“android.widget.EditText”，并将content的信息复制到Text中
    //endregion
    private boolean findEditText(AccessibilityNodeInfo rootNode, String content) {
        int count = rootNode.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);

            if (nodeInfo == null) {
                continue;
            }
            //LogToFile.write("nodeinfo:" + nodeInfo.toString());
            if (nodeInfo.getContentDescription() != null) {
                int nindex = NodeFunc.getContentDescription(nodeInfo).indexOf(sname);
                int cindex = NodeFunc.getContentDescription(nodeInfo).indexOf(scontent);
                if (nindex != -1) {
                    itemNodeinfo = nodeInfo;
                    //LogToFile.write("nodeinfo:" + nodeInfo.toString());
                }
            }
            if ("android.widget.EditText".equals(nodeInfo.getClassName())) {
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, true);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", content);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                return true;
            }
            if (findEditText(nodeInfo, content)) {
                return true;
            }
        }
        return false;
    }
}
