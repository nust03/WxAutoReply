package com.thbyg.wxautoreply;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class AutomationService extends BaseAccessibilityService {
    //region 全局参数定义
    private final static String MM_PNAME = "com.tencent.mm";
    private String pre_notification_time = null;
    private boolean hasAction = false;//自动回复信息
    private boolean hasFriendRequest = false;//接受好友请求
    private int hasFriendRequestCount = 0;
    private boolean hasReadMPAticle = false;//阅读公众号文章标示
    private String MP_Account_Name = "";//拟阅读公众号账号
    List<AccessibilityNodeInfo> Readed_Text_List = new ArrayList<AccessibilityNodeInfo>();//已读公众号文章Text
    private String Bottom_Menu_Name = "";//底栏菜单按钮所在页面
    private boolean locked = false;
    private boolean background = false;
    private String sname;
    private String scontent;
    AccessibilityNodeInfo itemNodeinfo;
    private KeyguardManager.KeyguardLock kl;
    private Handler handler = new Handler();
    private static AutomationService mService = null;
    AccessibilityNodeInfo focused_node = null;
    List<AccessibilityNodeInfo> nodelist = new ArrayList<AccessibilityNodeInfo>();
    private static String[] autoplay_msg = new String[]{"[微笑][微笑][微笑]", "[玫瑰][玫瑰][玫瑰][玫瑰][玫瑰][玫瑰][玫瑰][玫瑰][玫瑰]", "[强][强][强]", "[拥抱][拥抱]", "[握手][握手]", "[拳头][拳头]", "[OK]", "OK", "ok", "好的", "NB", "好！"};
    private String ChatName = "";
    private String ChatRecord = "";
    private String VideoSecond = "";
    //endregion
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
        serviceInfo.notificationTimeout = 1000;
        setServiceInfo(serviceInfo);
        LogToFile.write("AutomationService is onServiceConnected.");
        LogToFile.toast("AutomationService is onServiceConnected.");
    }
    //region * 处理服务事件函数
    @Override
    //endregion
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        int eventType = event.getEventType();
        LogToFile.write(String.format("=====处理 event=%s 开始...", AccessibilityEvent.eventTypeToString(eventType)));
        if(locked == true) {
            LogToFile.write("重复进入！");
        }
        else locked = true;
        int default_run_count =  0;
        try {
            switch (eventType) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
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
                default:
                    if(hasAction && default_run_count == 0){
                        dealAutoReplay(event);//处理自动回复消息
                        default_run_count++;
                    }
                    if(hasFriendRequest && hasFriendRequestCount > 0 && default_run_count == 0){
                        dealNewfriendRequest(event);//处理好友请求
                        default_run_count++;
                    }
                    if(hasReadMPAticle && default_run_count == 0){
                        dealReadMPArticle(event);
                        default_run_count++;
                    }
                    if(hasAction == false && hasFriendRequest == false && hasReadMPAticle == false  && default_run_count == 0){
                        Bottom_Menu_Name = "";
                        switch2Keyboard(event);//判断是否有“切换到键盘”按钮，如有切换到输入界面
                        goBottomFindMenu(event);//设置到“发现”页面
                        //default_run_count++;
                    }
            }
        }
        catch (Exception e){
            LogToFile.write("onAccessibilityEvent Fail!Error=" + e.getMessage());
            LogToFile.toast("onAccessibilityEvent Fail!Error=" + e.getMessage());
        }
        locked = false;
        LogToFile.write("locked:" + String.valueOf(locked) + ",hasAction=" + String.valueOf(hasAction)  + ",hasReadMPAticle=" + String.valueOf(hasReadMPAticle)
                + ",hasFriendRequest=" + String.valueOf(hasFriendRequest) + ",hasFriendRequestCount=" + String.valueOf(hasFriendRequestCount));
        LogToFile.write(String.format("=====处理 event=%s 结束！default_run_count=%d", AccessibilityEvent.eventTypeToString(eventType),default_run_count));
    }
    //region 设置到“发现”页面
    //endregion
    public void goBottomFindMenu(AccessibilityEvent event){
        LogToFile.write("设置到“发现”页面开始运行,goBottomFindMenu is running...");
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
                        if (NodeFunc.Wx_FindUI_Node_Text.contains(NodeFunc.getText(node))) {
                            find_text_count++;
                        }
                        if("朋友圈".equalsIgnoreCase(NodeFunc.getText(node))){
                            AccessibilityNodeInfo click_find_node = NodeFunc.getClickableParentNode(node);
                            if (click_find_node != null) {
                                if (click_find_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    LogToFile.write("点击“朋友圈”按钮成功。");
                                    Bottom_Menu_Name = "朋友圈";
                                    FuncTools.delay(delay_after_click);
                                } else {
                                    LogToFile.write("点击“朋友圈”按钮失败。");
                                }
                            }
                        }
                    }
                }
            }
            LogToFile.write(String.valueOf(line++));
            if(find_text_count >=4 ){
                LogToFile.write("当前页面已设置在“发现”页面。find_text_count=" + String.valueOf(find_text_count));
            }
            else{
                //“发现”菜单
                AccessibilityNodeInfo find_node = NodeFunc.findNodebyID_Class_Text(this.getRootInActiveWindow(),NodeFunc.Wx_BottomMenu_Btn_ID,NodeFunc.Wx_BottomMenu_Text[2],NodeFunc.Wx_BottomMenu_Btn_Class);
                AccessibilityNodeInfo click_find_node = null;
                if(find_node != null) {
                    LogToFile.write(find_node.toString());
                    click_find_node = NodeFunc.getClickableParentNode(find_node);
                    if (click_find_node != null) {
                        if (click_find_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            LogToFile.write("点击“发现”按钮成功。");
                            FuncTools.delay(delay_after_click);
                        } else {
                            LogToFile.write("点击“发现”按钮失败。");
                        }
                    } else {
                        this.performBackClick(200);
                    }
                }
            }
            LogToFile.write(String.valueOf(line++));
        }catch (Exception e){
            LogToFile.write("goBottomFindMenu Fail!Error=" + e.getMessage());
            LogToFile.toast("goBottomFindMenu Fail!Error=" + e.getMessage());
        }finally {
            LogToFile.write("设置到“发现”页面 运行结束,goBottomFindMenu run over.success_click_btn_count=" + String.valueOf(success_click_btn_count));
        }
    }
    //region 处理自动回复消息
    //endregion
    public void dealAutoReplay(AccessibilityEvent event){
        LogToFile.write("处理自动回复消息开始运行,dealAutoReplay is running...");
        try {
            if (hasAction) {//有新消息进来，自动随机回复，仅回复群“内部管理群”的新信息
                itemNodeinfo = null;
                String className = event.getClassName().toString();
                String contentDesc = event.getContentDescription().toString();

                //LogToFile.write("className:" + className + ",ContentDescription:" + contentDesc);

                if (className.equalsIgnoreCase("com.tencent.mm.ui.LauncherUI") ) {
                    String msg = autoplay_msg[FuncTools.getRandom(autoplay_msg.length)] + ",@" + sname;
                    if (sname.equalsIgnoreCase("史言兵") || sname.equalsIgnoreCase("优惠券搬运工")) {
                        if (scontent.indexOf("command") == 0) {
                            String[] command = scontent.trim().split("-");
                            if(command.length >= 2) {
                                if(command[1].trim().equalsIgnoreCase("阅读公众号文章")){
                                    hasReadMPAticle = true;
                                    MP_Account_Name = command[2];
                                    Readed_Text_List.clear();
                                }
                                LogToFile.write("接到 " + sname + " 返回微信主界面的命令,command=" + command[1] + ",MP_Account_Name:" + MP_Account_Name + ",hasReadMPAticle=" + String.valueOf(hasReadMPAticle));
                                msg= sname + ",接到您 " + command[1] + " 命令，即可执行!";
                            }
                        }
                        if (auto_replay_msg(msg)) {
                            send();
                            LogToFile.write("自动回复信息:" + msg);
                        }
                        //if(!hasReadMPAticle) this.performHomeClick(200);
                    }
                    else{
                        int replay_percent = FuncTools.getRandom(100);
                        if(replay_percent > 90) {//10%的概率回复其他人的信息
                            msg = autoplay_msg[FuncTools.getRandom(autoplay_msg.length)] + "," + sname + ", " + String.valueOf(replay_percent) + "%";
                            if (auto_replay_msg(msg)) {
                                send();
                                LogToFile.write("自动回复信息:" + msg);
                            }
                        }
                        //this.performHomeClick(200);
                    }
                }
                hasAction = false;
            }
        }
        catch (Exception e){
            LogToFile.write("dealAutoReplay Fail!Error=" + e.getMessage());
            LogToFile.toast("dealAutoReplay Fail!Error=" + e.getMessage());
        }
        LogToFile.write("处理自动回复消息运行结束,dealAutoReplay run over.");
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
                    LogToFile.write("TYPE_NOTIFICATION_STATE_CHANGED:content:" + content);
                    if (!TextUtils.isEmpty(content) ) {//&& (content.indexOf("史言兵") == 0 || content.indexOf("优惠券搬运工") == 0)
                        background = true;
                        notifyWechat(event);
                        if (pri == Notification.PRIORITY_HIGH || pri == Notification.PRIORITY_DEFAULT)
                            hasAction = true;//文字信息 PRIORITY_HIGH = 1;PRIORITY_DEFAULT = 0;
                        else if (pri == Notification.PRIORITY_MAX) {
                            hasFriendRequest = true;
                        }//加好友信息 PRIORITY_MAX = 2
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
    //region 处理好友请求
    //endregion
    public void dealNewfriendRequest(AccessibilityEvent event){
        LogToFile.write("处理好友请求开始运行,dealNewfriendRequest is running...");
        int success_click_btn_count = 0;
        try{
            List<AccessibilityNodeInfo> tmp_node_list = new ArrayList<AccessibilityNodeInfo>();
            AccessibilityNodeInfo address_list_node = NodeFunc.findNodebyID_Class_Text(this.getRootInActiveWindow(),NodeFunc.Wx_BottomMenu_Btn_ID,NodeFunc.Wx_BottomMenu_Text[1],NodeFunc.Wx_BottomMenu_Btn_Class);
            int new_friend_node_count = NodeFunc.findNodebyText_Class(this.getRootInActiveWindow(),NodeFunc.Wx_NewFriend_Text,NodeFunc.Wx_NewFriend_Class,tmp_node_list);
            AccessibilityNodeInfo recv_node = NodeFunc.findNodebyID_Class_Text(this.getRootInActiveWindow(),NodeFunc.Wx_NewFriend_RecvBtn_ID,NodeFunc.Wx_NewFriend_RecvBtn_Text,NodeFunc.Wx_NewFriend_RecvBtn_Class);
            AccessibilityNodeInfo finish_node = NodeFunc.findNodebyID_Class_Text(this.getRootInActiveWindow(),NodeFunc.Wx_NewFriend_FinishBtn_ID,NodeFunc.Wx_NewFriend_FinishBtn_Text,NodeFunc.Wx_NewFriend_FinishBtn_Class);
            AccessibilityNodeInfo send_msg_node = NodeFunc.findNodebyID_Class_Text(this.getRootInActiveWindow(),NodeFunc.Wx_NewFriend_SendMsgBtn_ID,NodeFunc.Wx_NewFriend_SendMsgBtn_Text,NodeFunc.Wx_NewFriend_SendMsgBtn_Class);
            int delay_after_click = 200;//click后延迟200毫秒

            if (hasFriendRequest && hasFriendRequestCount == 0 && success_click_btn_count == 0) {
                //printNodeInfo();
                tmp_node_list.clear();
                hasFriendRequestCount = NodeFunc.findNodebyID_Class_Text(this.getRootInActiveWindow(),NodeFunc.Wx_NewFriend_RecvBtn_ID,NodeFunc.Wx_NewFriend_RecvBtn_Text,NodeFunc.Wx_NewFriend_RecvBtn_Class,tmp_node_list);
                success_click_btn_count++;
            }
            if(address_list_node != null && success_click_btn_count == 0){//微信底部导航栏“通讯录”的node
                AccessibilityNodeInfo click_node = NodeFunc.getClickableParentNode(address_list_node);
                if(click_node != null){
                    if(click_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
                        LogToFile.write("点击底部导航栏“通讯录”按钮成功。等待 " + String.valueOf(delay_after_click) + " 毫秒。");
                        FuncTools.delay(delay_after_click);
                        success_click_btn_count++;
                    }
                    else LogToFile.write("点击底部导航栏“通讯录”按钮失败。点击的父节点click_node=" + click_node.toString());
                }
                else{
                    LogToFile.write("点击底部导航栏“通讯录”按钮。可点击的父节点click_node=null");
                }
            }
            if(new_friend_node_count > 0 && success_click_btn_count == 0){//微信底部导航栏“通讯录”的node,获取“新的朋友”node，然后点击
                LogToFile.write("new_friend_node_count=" + String.valueOf(new_friend_node_count) + ",tmp_node_list.size=" + String.valueOf(tmp_node_list.size()));
                for(AccessibilityNodeInfo new_friend_node : tmp_node_list){
                    AccessibilityNodeInfo click_node = NodeFunc.getClickableParentNode(new_friend_node);
                    if(click_node != null){
                        if(click_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
                            LogToFile.write("点击“通讯录”-“新的朋友”成功。等待 " + String.valueOf(delay_after_click) + " 毫秒。");
                            FuncTools.delay(delay_after_click);
                            success_click_btn_count++;
                        }
                        else LogToFile.write("点击“通讯录”-“新的朋友”失败。点击的父节点click_node=" + click_node.toString());
                    }
                    else{
                        LogToFile.write("点击“通讯录”-“新的朋友”。可点击的父节点click_node=null");
                    }
                }
            }
            if(recv_node != null  && success_click_btn_count == 0){
                if(recv_node.isClickable()){
                    if(recv_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
                        LogToFile.write("接受加好友请求，在“新的朋友”页面，点击“接受”按钮成功。等待 " + String.valueOf(delay_after_click) + " 毫秒。");
                        FuncTools.delay(delay_after_click);
                        success_click_btn_count++;
                    }
                    else LogToFile.write("接受加好友请求，在“新的朋友”页面，点击“接受”按钮失败。点击的父节点click_node=" + recv_node.toString());
                }
                else
                    LogToFile.write("接受加好友请求，在“新的朋友”页面，检测到“接受”按钮，但不可点击。nodeinfo=" + recv_node.toString());
            }
            if(finish_node != null && success_click_btn_count == 0){
                if(finish_node.isClickable()){
                    if(finish_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
                        LogToFile.write("接受加好友请求，在“朋友验证”页面 ，点击“完成”成功。等待 " + String.valueOf(delay_after_click) + " 毫秒。");
                        FuncTools.delay(delay_after_click);
                        success_click_btn_count++;
                    }
                    else LogToFile.write("接受加好友请求，在“朋友验证”页面 ，点击“完成”按钮失败。点击的父节点click_node=" + finish_node.toString());
                }
                else
                    LogToFile.write("接受加好友请求，在“朋友验证”页面 ，点击“完成”按钮，但不可点击。nodeinfo=" + finish_node.toString());
            }
            if(send_msg_node != null && success_click_btn_count == 0){
                if(send_msg_node.isClickable()){
                    if(send_msg_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
                        LogToFile.write("接受加好友请求，“验证”完成后“发消息”，点击“发消息”成功。等待 " + String.valueOf(delay_after_click) + " 毫秒。");
                        FuncTools.delay(delay_after_click);
                        success_click_btn_count++;
                    }
                    else LogToFile.write("接受加好友请求，“验证”完成后“发消息”，点击“发消息”失败。点击的父节点click_node=" + send_msg_node.toString());

                    hasFriendRequestCount--;
                    if(hasFriendRequestCount == 0) hasFriendRequest = false;
                }
                else
                    LogToFile.write("接受加好友请求，“验证”完成后“发消息”，点击“发消息”按钮，但不可点击。nodeinfo=" + send_msg_node.toString());
            }
            if(success_click_btn_count == 0) this.performBackClick(delay_after_click);
        }
        catch (Exception e){
            LogToFile.write("dealNewfriendRequest Fail!Error=" + e.getMessage());
            LogToFile.toast("dealNewfriendRequest Fail!Error=" + e.getMessage());
        }
        LogToFile.write("处理好友请求运行结束,dealNewfriendRequest run over.success_click_btn_count=" + String.valueOf(success_click_btn_count));
    }
    //region 判断是否有“切换到键盘”按钮，如有切换到输入界面
    //endregion
    public void switch2Keyboard(AccessibilityEvent event){
        //LogToFile.write("判断是否有“切换到键盘”按钮，如有切换到输入界面开始运行,switch2Keyboard is running...");
        int success_click_btn_count = 0;
        int delay_after_click = 200;//click后延迟200毫秒
        try{
            List<AccessibilityNodeInfo> tmp_node_list = new ArrayList<AccessibilityNodeInfo>();
            //输入界面，检测“切换到按住说话”按钮或者“切换到键盘”按钮
            int node_count = NodeFunc.findNodebyID_Class(this.getRootInActiveWindow(),NodeFunc.Wx_Switch2TalkBtn_ID,NodeFunc.Wx_Switch2TalkBtn_Class,tmp_node_list);
            AccessibilityNodeInfo switch2Talk_node = null;
            AccessibilityNodeInfo switch2Keyboard_node = null;
            AccessibilityNodeInfo bottom_wechat_node = null;//底栏“微信”node
            AccessibilityNodeInfo bottom_address_node = null;//底栏“通讯录”node
            AccessibilityNodeInfo bottom_find_node = null;//底栏“发现”node
            AccessibilityNodeInfo bottom_wo_node = null;//底栏“我”node

            if(node_count > 0){
                for(AccessibilityNodeInfo node : tmp_node_list){
                    if(NodeFunc.Wx_Switch2KeyboardBtn_Desc.trim().equalsIgnoreCase(NodeFunc.getContentDescription(node)) && node.isClickable()) {
                        switch2Keyboard_node = node;
                        //LogToFile.write("发现“切换到键盘”按钮。nodeinfo=" + node.getContentDescription().toString());
                    }
                    else if(NodeFunc.Wx_Switch2TalkBtn_Desc.trim().equalsIgnoreCase(NodeFunc.getContentDescription(node)) && node.isClickable()) {
                        switch2Talk_node = node;
                        //LogToFile.write("发现“切换到按住说话”按钮。nodeinfo=" + node.getContentDescription().toString());
                    }
                }
            }
            if(switch2Keyboard_node != null && success_click_btn_count == 0){//检测到“切换到键盘”按钮即开始点击，切换到输入界面
                if(switch2Keyboard_node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    LogToFile.write("发现“切换到键盘”按钮。切换到“切换到按住说话”成功" );
                    FuncTools.delay(delay_after_click);
                }
                else LogToFile.write("发现“切换到键盘”按钮。切换到“切换到按住说话”失败" );
                success_click_btn_count++;
            }
            if(success_click_btn_count == 0){
                //this.performHomeClick(delay_after_click);
            }
        }
        catch (Exception e){
            LogToFile.write("switch2Keyboard Fail!Error=" + e.getMessage());
            LogToFile.toast("switch2Keyboard Fail!Error=" + e.getMessage());
        }
        //LogToFile.write("判断是否有“切换到键盘”按钮，如有切换到输入界面 运行结束,switch2Keyboard run over.success_click_btn_count=" + String.valueOf(success_click_btn_count));
    }
    // region 接到阅读公众号命令，阅读公众号文章
    //endregion
    public void dealReadMPArticle(AccessibilityEvent event){
        LogToFile.write("接到阅读公众号命令，阅读公众号文章开始运行,dealReadMPArticle is running...");
        int success_click_btn_count = 0;
        int delay_after_click = 200;//click后延迟 500 毫秒
        int delay_after_back_click = 1000;//back click后延迟 1000 毫秒
        int delay_after_read_click = 6000;//阅读click后延迟 6000  毫秒
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

        try{
            try {
                mp_node = NodeFunc.findNodebyID_Class_Text(this.getRootInActiveWindow(),NodeFunc.Wx_MPDYH_ID,NodeFunc.Wx_MPDYH_Text,NodeFunc.Wx_MPDYH_Class);
                //"订阅号"三个字的参数
                
                DYHUI_dyhWord_node = NodeFunc.findNodebyID_Class_Text(this.getRootInActiveWindow(),NodeFunc.Wx_DYHUI_dyhWord_ID,NodeFunc.Wx_DYHUI_dyhWord_Text,NodeFunc.Wx_DYHUI_dyhWord_Class);
                //"订阅号界面  "返回"的参数
                
                DYHUI_back_node = NodeFunc.findNodebyID_Class_Desc(this.getRootInActiveWindow(),NodeFunc.Wx_DYHUI_back_ID,NodeFunc.Wx_DYHUI_back_Desc,NodeFunc.Wx_DYHUI_back_Class);
                // 特定公众号图文界面，特定“公众号”参数
                
                TuWenUI_GZH_node = NodeFunc.findNodebyID_Class_Text(this.getRootInActiveWindow(),NodeFunc.Wx_TuWenUI_GZH_ID,this.MP_Account_Name,NodeFunc.Wx_TuWenUI_GZH_Class);
                
                account_node_count = NodeFunc.findNodebyID_Class(this.getRootInActiveWindow(),NodeFunc.Wx_MP_Account_ID,NodeFunc.Wx_MP_Account_Class,account_node_node_list);
                
                single_article_node_count = NodeFunc.findNodebyID_Class(this.getRootInActiveWindow(),NodeFunc.Wx_Single_Article_ID,NodeFunc.Wx_Single_Article_Class,single_article_node_list);
                
                mularticle_header_node_count = NodeFunc.findNodebyID_Class(this.getRootInActiveWindow(),NodeFunc.Wx_MulArticle_Header_ID,NodeFunc.Wx_MulArticle_Header_Class,mularticle_header_node_list);
                
                mularticle_other_node_count = NodeFunc.findNodebyID_Class(this.getRootInActiveWindow(),NodeFunc.Wx_MulArticle_Other_ID,NodeFunc.Wx_MulArticle_Other_Class,mularticle_other_node_list);

                //公众号文章阅读界面，“返回”按钮的参数
                
                back_node_count = NodeFunc.findNodebyID_Class(this.getRootInActiveWindow(),NodeFunc.Wx_ReadArticle_Back_ID,NodeFunc.Wx_ReadArticle_Back_Class,back_node_list);
                //特定公众号图文阅读界面，“网页由 mp.weixin.qq.com 提供”参数
                
                TuWenReadUI_WebPage_node = NodeFunc.findNodebyID_Class_Text(this.getRootInActiveWindow(),NodeFunc.Wx_TuWenReadUI_WebPage_ID,NodeFunc.Wx_TuWenReadUI_WebPage_Text,NodeFunc.Wx_TuWenReadUI_WebPage_Class);
                
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

            if(mp_node != null && success_click_btn_count == 0){//进入“订阅号”界面
                LogToFile.write("准备进入“订阅号”界面");
                AccessibilityNodeInfo node = NodeFunc.getClickableParentNode(mp_node);
                if(node != null) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    FuncTools.delay(delay_after_click);
                    success_click_btn_count++;
                    LogToFile.write("进入“订阅号”界面成功");
                }
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
            if(Readed_Text_List.size() == tuwen_count && tuwen_count > 0 && success_click_btn_count == 0){
                LogToFile.write("阅读公众号=" + this.MP_Account_Name + " 图文信息结束，共阅读 " + String.valueOf(tuwen_count) + " 条。");
                LogToFile.toast("阅读公众号=" + this.MP_Account_Name + " 图文信息结束，共阅读 " + String.valueOf(tuwen_count) + " 条。");
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
                    ChatName = NodeFunc.getContentDescription(node1);-
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
    //endregion
    @SuppressLint("NewApi")
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
        try {
            if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                Notification notification = (Notification) event.getParcelableData();
                if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && event.getPackageName().equals(NodeFunc.Wx_PackageName)) {
                    if (notification.priority == Notification.PRIORITY_HIGH || notification.priority == Notification.PRIORITY_DEFAULT)//文字信息 PRIORITY_HIGH = 1;PRIORITY_DEFAULT = 0;
                    {
                        String content = notification.tickerText.toString();
                        //LogToFile.write("content:" + content);
                        String[] cc = content.split(":");
                        sname = cc[0].trim();
                        scontent = cc[1].trim();
                    } else if (notification.priority == Notification.PRIORITY_MAX) {//加好友信息 PRIORITY_MAX = 2
                        String content = notification.tickerText.toString();
                        //LogToFile.write("content:" + content);
                        String[] cc = content.split("请求添加你为");
                        sname = cc[0].trim();
                        scontent = cc[1].trim();
                    }

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
            LogToFile.write("notifyWechat Fail!Error=" + e.getMessage());
            LogToFile.toast("notifyWechat Fail!Error=" + e.getMessage());
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
