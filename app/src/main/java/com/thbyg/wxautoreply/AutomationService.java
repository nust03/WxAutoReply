package com.thbyg.wxautoreply;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
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
    private String  stay_input_time = "";//在输入界面停留的循环次数
    String runcontent = "";//上下文
    private String sname;
    private String scontent;
    AccessibilityNodeInfo itemNodeinfo;
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
        LogToFile.write(String.format("=====处理 event=%s 开始...", AccessibilityEvent.eventTypeToString(eventType)) + ",UI_Title=" + NodeFunc.getContentDescription(this.getRootInActiveWindow()));
        if(locked == true) {
            LogToFile.write("重复进入！");
        }
        else locked = true;
        int default_run_count =  0;
        try {
            switch (eventType) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                    //region 处理 TYPE_NOTIFICATION_STATE_CHANGED 事件
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
                case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                    AccessibilityNodeInfo node= this.getRootInActiveWindow();
                    if(node != null) {
                    }
                    //break;
                default:
                    if(hasAction && default_run_count == 0){
                        dealAutoReplay(event);//处理自动回复消息
                        default_run_count++;
                    }
                    if(hasFriendRequest  && default_run_count == 0){
                        dealNewfriendRequest(event);//处理好友请求
                        default_run_count++;
                    }
                    if(hasReadMPAticle && default_run_count == 0){//处理阅读公众号文章
                        dealReadMPArticle(event);
                        default_run_count++;
                    }
                    if(hasAction == false && hasFriendRequest == false && hasReadMPAticle == false  && default_run_count == 0){
                        Bottom_Menu_Name = "";
                        switch2Keyboard(event);//判断是否有“切换到键盘”按钮，如有切换到输入界面
                        {//无任务，设置朋友圈
                            /*
                            String current_time = DateUtils.getCurrentTime("yyyy-MM-dd HH:mm:ss");
                            if(stay_input_time.isEmpty()) stay_input_time = DateUtils.getCurrentTime("yyyy-MM-dd HH:mm:ss");
                            int second = DateUtils.compareTime(stay_input_time,current_time,3);
                            if(second >= 10) goMoments(event);//设置到“朋友圈”页面
                            LogToFile.write("停留在输入界面时长，stay_input_time=" + stay_input_time + ",current_time=" + current_time + ",时长=" + String.valueOf(second));
                            */
                            //NodeFunc.getLinearLayoutNodeinfo(this.getRootInActiveWindow());
                        }
                    }
                    else{
                        stay_input_time = DateUtils.getCurrentTime("yyyy-MM-dd HH:mm:ss");
                        //LogToFile.write("初始化时间stay_input_time=" + stay_input_time);

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
        LogToFile.write(String.format("=====处理 event=%s 结束！default_run_count=%d", AccessibilityEvent.eventTypeToString(eventType),default_run_count) + ",UI_Title=" + NodeFunc.getContentDescription(this.getRootInActiveWindow()));
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
    //region 处理自动回复消息
    //endregion
    public void dealAutoReplay(AccessibilityEvent event){
        LogToFile.write("处理自动回复消息开始运行,dealAutoReplay is running...");
        try {
            if (hasAction) {//有新消息进来，自动随机回复，仅回复群“内部管理群”的新信息
                itemNodeinfo = null;
                String className = event.getClassName() == null ? "" : event.getClassName().toString();
                String contentDesc = event.getContentDescription() == null ? "" : event.getContentDescription().toString();

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
                    hasAction = false;
                }
                else{
                    LogToFile.write("微信窗口尚未激活，等待500毫秒。className=" + className);
                    FuncTools.delay(100);
                }
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

                this.printNodeInfo(linearLayout_node,all_node_list,false);
                for(AccessibilityNodeInfo nodeInfo : all_node_list){
                    if ("android.widget.EditText".equalsIgnoreCase(NodeFunc.getClassName(nodeInfo))) {
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                        ClipData clip = ClipData.newPlainText("label", autoplay_msg[FuncTools.getRandom(autoplay_msg.length)] + "，" + NodeFunc.getTopLeftText(NodeFunc.getContentDescription(root_node)));
                        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboardManager.setPrimaryClip(clip);
                        if(nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE)){
                            send();
                        }
                    }
                }

                if(backbtn_node != null){
                    if(clickNode(backbtn_node,delay_after_click)){
                        FuncTools.delay(delay_after_click);
                    }
                    else{
                        this.performBackClick(delay_after_click);
                    }
                }
                else{
                    this.performBackClick(delay_after_click);
                }
                run_msg = "未执行到任何符合条件的判断，查找“返回”node并点击或者直接执行全局“返回”." ;
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
    //region 点击按钮
    //endregion
    public boolean clickNode(AccessibilityNodeInfo node,int delay_after_click){
        boolean f = false;
        try{
            if(node != null){
                AccessibilityNodeInfo click_node = NodeFunc.getClickableParentNode(node);
                if(click_node != null){
                    f = click_node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    FuncTools.delay(delay_after_click);
                }
            }
        }
        catch (Exception e){
            LogToFile.write("dealNewfriendRequest Fail!Error=" + e.getMessage());
            LogToFile.toast("dealNewfriendRequest Fail!Error=" + e.getMessage());
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
                
                DYHUI_back_node = NodeFunc.findNodebyID_Class_Desc(this.getRootInActiveWindow(),NodeFunc.Wx_DYHUI_back_ID,NodeFunc.Wx_DYHUI_back_Class,NodeFunc.Wx_DYHUI_back_Desc);
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
                    content = notification.tickerText.toString();
                    if (content.contains("请求添加你为")) {//加好友信息 PRIORITY_MAX = 2
                        String[] cc = content.split("请求添加你为");
                        sname = cc[0].trim();
                        scontent = cc[1].trim();
                        hasFriendRequest = true;
                    }
                    else
                    {
                        String[] cc = content.split("\\:");
                        if(cc.length >= 2) {
                            sname = cc[0].trim();
                            scontent = cc[1].trim();
                        }
                        else{
                            scontent = content;
                        }
                        hasAction = true;
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
