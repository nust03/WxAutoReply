package com.thbyg.wxautoreply;

import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by myxiang on 2017/10/14.
 */

public class NodeFunc {
    public static String Wx_PackageName = "com.tencent.mm";
    public static String Wx_BottomMenuSourceID = "com.tencent.mm:id/brg";//微信底部的菜单栏的类名 resource-id
    public static String[] Wx_BottomMenuName = new String[]{"微信","通讯录","发现","我"};//微信底部的菜单栏的 TEXT
    public static  int delay_after_click = 800;
    //com.tencent.mm:id/afv
    public static String Wx_MP_TEXT_ID = "com.tencent.mm:id/fa";//微信公众号第二条一下的消息 resource-id

    public static String Wx_MP_DYH_ID = "com.tencent.mm:id/afv";//微信界面的订阅号 resource-id
    public static String Wx_MP_DYH_Name = "订阅号";//微信界面的订阅号 resource-id
    //
    public static String Wx_MP_DYHRead_ID = "com.tencent.mm:id/gh";//微信订阅号阅读界面 resource-id
    public static String Wx_MP_DYHRead_Name = "订阅号";//微信 订阅号阅读界面 resource-id
    //com.tencent.mm:id/afv
    public static String Wx_MP_GZH_ID = "com.tencent.mm:id/afv";//微信公众号 resource-id
    //输入框 com.tencent.mm:id/a3b
    public static String Wx_MP_EditText_ID = "com.tencent.mm:id/a3b";//微信输入框 resource-id
    public static String Wx_MP_EditText_Class = "android.widget.EditText";//微信输入框class resource-id
    //有新好友请求，“接受”页面的接受按钮的参数
    public static String Wx_NewFriend_RecvBtn_ID = "com.tencent.mm:id/b1n";
    public static String Wx_NewFriend_RecvBtn_Class = "android.widget.Button";
    public static String Wx_NewFriend_RecvBtn_Text = "接受";
    //有新好友请求，“验证”页面的完成按钮的参数
    public static String Wx_NewFriend_FinishBtn_ID = "com.tencent.mm:id/gy";
    public static String Wx_NewFriend_FinishBtn_Class = "android.widget.TextView";
    public static String Wx_NewFriend_FinishBtn_Text = "完成";
    //有新好友请求，“验证”完成后“发消息”按钮的参数
    public static String Wx_NewFriend_SendMsgBtn_ID = "com.tencent.mm:id/ahq";
    public static String Wx_NewFriend_SendMsgBtn_Class = "android.widget.Button";
    public static String Wx_NewFriend_SendMsgBtn_Text = "发消息";
    // 主界面 最下面导航栏 “微信”、“通讯录”、“发现”、“我”的参数
    public static String Wx_BottomMenu_Btn_ID = "com.tencent.mm:id/by5";
    public static String Wx_BottomMenu_Btn_Class = "android.widget.TextView";
    public static String[] Wx_BottomMenu_Text = new String[]{"微信","通讯录","发现","我"};
    // “通讯录”界面 “新的朋友”的参数
    public static String Wx_NewFriend_Text = "新的朋友";
    public static String Wx_NewFriend_Class = "android.widget.TextView";
    // 输入界面，“切换到按住说话”按钮的参数
    public static String Wx_Switch2TalkBtn_ID = "com.tencent.mm:id/a6z";
    public static String Wx_Switch2TalkBtn_Class = "android.widget.ImageButton";
    public static String Wx_Switch2TalkBtn_Desc = "切换到按住说话";
    // 输入界面，“切换到键盘”按钮的参数
    public static String Wx_Switch2KeyboardBtn_ID = "com.tencent.mm:id/a6z";
    public static String Wx_Switch2KeyboardBtn_Class = "android.widget.ImageButton";
    public static String Wx_Switch2KeyboardBtn_Desc = "切换到键盘";
    // 微信界面，“订阅号”参数
    public static String Wx_MPDYH_ID = "com.tencent.mm:id/ak1";
    public static String Wx_MPDYH_Class = "android.view.View";
    public static String Wx_MPDYH_Text = "订阅号";
    //-----------------------订阅号界面开始--------------------------------------
    // 订阅号界面，“公众号”参数
    public static String Wx_MP_Account_ID = "com.tencent.mm:id/ak1";
    public static String Wx_MP_Account_Class = "android.view.View";
    //"订阅号"三个字的参数
    public static String Wx_DYHUI_dyhWord_ID = "com.tencent.mm:id/h2";
    public static String Wx_DYHUI_dyhWord_Class = "android.widget.TextView";
    public static String Wx_DYHUI_dyhWord_Text = "订阅号";
    //"订阅号界面  "返回"的参数
    public static String Wx_DYHUI_back_ID = "com.tencent.mm:id/h1";
    public static String Wx_DYHUI_back_Class = "android.widget.ImageView";
    public static String Wx_DYHUI_back_Desc = "返回";
    //-----------------------订阅号界面结束--------------------------------------

    //-----------------------特定公众号图文界面开始--------------------------------------
    // 订阅号阅读界面，单条图文的“公众号文章”参数
    public static String Wx_Single_Article_ID = "com.tencent.mm:id/a82";
    public static String Wx_Single_Article_Class = "android.widget.TextView";
    public static String Wx_Single_Article_Text = "查看全文";
    // 订阅号阅读界面，多条图文的头条“公众号文章”参数
    public static String Wx_MulArticle_Header_ID = "com.tencent.mm:id/a7y";
    public static String Wx_MulArticle_Header_Class = "android.widget.TextView";
    // 订阅号阅读界面，多条图文的其他条“公众号文章”参数
    public static String Wx_MulArticle_Other_ID = "com.tencent.mm:id/fv";
    public static String Wx_MulArticle_Other_Class = "android.widget.TextView";
    // 特定公众号图文界面，特定“公众号”参数
    public static String Wx_TuWenUI_GZH_ID = "com.tencent.mm:id/h2";
    public static String Wx_TuWenUI_GZH_Class = "android.widget.TextView";

    //-----------------------特定公众号图文界面结束--------------------------------------

    //-----------------------特定公众号图文阅读界面开始--------------------------------------
    //公众号文章阅读界面，“返回”按钮的参数
    public static String Wx_ReadArticle_Back_ID = "com.tencent.mm:id/hg";
    public static String Wx_ReadArticle_Back_Class = "android.widget.ImageView";
    public static String Wx_ReadArticle_Back_Desc = "返回";
    // 特定公众号图文阅读界面，特定“公众号”参数
    public static String Wx_TuWenReadUI_GZH_ID = "android:id/text1";
    public static String Wx_TuWenReadUI_GZH_Class = "android.widget.TextView";
    //特定公众号图文阅读界面，“网页由 mp.weixin.qq.com 提供”参数
    public static String Wx_TuWenReadUI_WebPage_ID = "com.tencent.mm:id/djo";
    public static String Wx_TuWenReadUI_WebPage_Class = "android.widget.TextView";
    public static String Wx_TuWenReadUI_WebPage_Text = "网页由 mp.weixin.qq.com 提供";
    //-----------------------特定公众号图文阅读界面结束--------------------------------------

    //-----------------------“发现”页面标识开始--------------------------------------
    //“发现”页面，标识参数
    public static String Wx_FindUI_Node_ID = "android:id/title";
    public static String Wx_FindUI_Node_Class = "android.widget.TextView";
    public static List<String> Wx_FindUI_Node_Text = new ArrayList<>(Arrays.asList("朋友圈","扫一扫","购物","游戏","小程序","摇一摇","看一看","搜一搜","附近的人"));
    //-----------------------“发现”页面标识结束--------------------------------------

    //-----------------------“我”页面标识开始--------------------------------------
    //“我”页面，标识参数
    public static String Wx_WoUI_Node_ID = "android:id/title";
    public static String Wx_WoUI_Node_Class = "android.widget.TextView";
    public static List<String> Wx_WoUI_Node_Text = new ArrayList<>(Arrays.asList("钱包","收藏","相册","表情","设置"));
    //-----------------------“我”页面标识结束--------------------------------------

    //-----------------------“微信”页面 聊天记录 标识开始--------------------------------------
    public static String Wx_ChatRecord_Node_ID = "com.tencent.mm:id/ak1";
    public static String Wx_ChatRecord_Node_Class = "android.view.View";
    //-----------------------“微信”页面 聊天记录 标识结束--------------------------------------

    //-----------------------“朋友圈”页面 标识开始--------------------------------------
    public static String Wx_MomentsUI__Desc = "当前所在页面,朋友圈";
    public static String Wx_MomentsUI_Class = "android.widget.FrameLayout";
    //-----------------------“朋友圈”页面 标识结束--------------------------------------

    //-----------------------“返回” 标识开始--------------------------------------
    public static String Wx_BackBtn__Desc = "返回";
    public static String Wx_BackBtn_Class = "android.widget.ImageView";
    //-----------------------“返回” 标识结束--------------------------------------

    //-----------------------“当前所在页面” 标识开始--------------------------------------
    public static List<String> Wx_CurrentUI__Title = new ArrayList<>(Arrays.asList("新的朋友","朋友验证","详细资料","朋友圈","设置","与订阅号的聊天"));
    //-----------------------“当前所在页面” 标识结束--------------------------------------

    //-----------------------“正在处理...”等待对话框 标识开始--------------------------------------
    public static String Wx_DealingUI_ID = "com.tencent.mm:id/tj";
    public static String Wx_DealingUI_Class = "android.widget.TextView";
    public static String Wx_DealingUI_Text = "正在处理...";
    //-----------------------“正在处理...”等待对话框 标识结束--------------------------------------

    //-----------------------“请稍候...”等待对话框 标识开始--------------------------------------
    public static String Wx_WaitingUI_Class = "android.widget.TextView";
    public static String Wx_WaitingUI_Text = "请稍候...";
    //-----------------------“请稍候...”等待对话框 标识结束--------------------------------------


    //region 获取node的 属性 字符串 ，以防null错误；
    //endregion
    private String getNodeAttrString(AccessibilityNodeInfo node,String Attr){
        String ret_str = "";
        try {
            if (Attr.equalsIgnoreCase("text"))
                ret_str = node.getText() == null ? "" : node.getText().toString().trim();
            else if (Attr.equalsIgnoreCase("ClassName"))
                ret_str = node.getClassName() == null ? "" : node.getClassName().toString().trim();
            else if (Attr.equalsIgnoreCase("PackageName"))
                ret_str = node.getPackageName() == null ? "" : node.getPackageName().toString().trim();
            else if (Attr.equalsIgnoreCase("Desc"))
                ret_str = node.getContentDescription() == null ? "" : node.getContentDescription().toString().trim();
            else if (Attr.equalsIgnoreCase("ID"))
                ret_str = node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName().toString().trim();
        }catch (Exception e){
            LogToFile.write("run Fail!Error=" + e.getMessage());
        }finally {
            return ret_str;
        }
    }

    //region 从android.widget.LinearLayout节点查找“返回”按钮的node信息
    //endregion
    public static AccessibilityNodeInfo getBottomBtnNodeinLinearLayout(AccessibilityNodeInfo linearLayout_node,String btn_name) {
        AccessibilityNodeInfo backbtn_node = null;
        List<AccessibilityNodeInfo> all_node_list = new ArrayList<AccessibilityNodeInfo>();
        try {
            if (linearLayout_node != null) {
                getAllNodeInfo(linearLayout_node,all_node_list);
                for(AccessibilityNodeInfo node : all_node_list){
                    if(Wx_BottomMenu_Btn_ID.equalsIgnoreCase(getViewIdResourceName(node)) && Wx_BottomMenu_Btn_Class.equalsIgnoreCase(getClassName(node)) && btn_name.equalsIgnoreCase(getText(node)) && getPackageName(node).equalsIgnoreCase(Wx_PackageName) ){
                        backbtn_node = node;
                        break;
                    }
                }
            }
        }catch (Exception e){
            LogToFile.write("getLinearLayoutNodeinfo Fail!Error=" + e.getMessage());
            LogToFile.toast("getLinearLayoutNodeinfo Fail!Error=" + e.getMessage());
        }finally {
            return backbtn_node;
        }
    }
    //region 从android.widget.LinearLayout节点查找“返回”按钮的node信息
    //endregion
    public static AccessibilityNodeInfo getBackBtnNodeinLinearLayout(AccessibilityNodeInfo linearLayout_node) {
        AccessibilityNodeInfo backbtn_node = null;
        List<AccessibilityNodeInfo> all_node_list = new ArrayList<AccessibilityNodeInfo>();
        try {
            if (linearLayout_node != null) {
                getAllNodeInfo(linearLayout_node,all_node_list);
                for(AccessibilityNodeInfo node : all_node_list){
                    if("android.widget.ImageView".equalsIgnoreCase(getClassName(node)) && "返回".equalsIgnoreCase(getContentDescription(node)) && getPackageName(node).equalsIgnoreCase(Wx_PackageName) ){
                        backbtn_node = node;
                        break;
                    }
                }
            }
        }catch (Exception e){
            LogToFile.write("getLinearLayoutNodeinfo Fail!Error=" + e.getMessage());
            LogToFile.toast("getLinearLayoutNodeinfo Fail!Error=" + e.getMessage());
        }finally {
            return backbtn_node;
        }
    }
    //region 从界面的ContentDesc取出与TopLeft 的Text
    //endregion
    public static String getTopLeftText(String ui_title) {
        String text = "";
        try{
            if(ui_title.indexOf("当前所在页面,") == 0 && ui_title.contains("的聊天") == false){
                String[] array = ui_title.split(",");
                if(array.length >= 2) text = array[1];
            }
            else if(ui_title.indexOf("当前所在页面,") == 0 && ui_title.contains("的聊天") == true){
                String[] array = ui_title.split(",与|的聊天");
                if(array.length >= 2) {
                    text = array[1];
                    //LogToFile.write("array.length=" + String.valueOf(array.length) + ",array[1]=" + array[1] + ",ui_title=" + ui_title);
                }
            }
        }catch (Exception e){
            LogToFile.write("getTopLeftText Fail!Error=" + e.getMessage());
            LogToFile.toast("getTopLeftText Fail!Error=" + e.getMessage());
        }finally {
            return text;
        }
    }
    //region 获取android.widget.LinearLayout节点，且Rect同RootNode；
    //endregion
    public static AccessibilityNodeInfo getLinearLayoutNodeinfo(AccessibilityNodeInfo RootNode) {
        AccessibilityNodeInfo linearLayout_node = null;
        List<AccessibilityNodeInfo> all_node_list = new ArrayList<AccessibilityNodeInfo>();
        List<AccessibilityNodeInfo> linearLayout_node_list = new ArrayList<AccessibilityNodeInfo>();
        AccessibilityNodeInfo top_left_node = null;
        Rect root_rect = new Rect();
        Rect node_rect = new Rect();
        String ui_title = getContentDescription(RootNode);
        String top_left_text = "";
        try {
            if (RootNode == null) return null;
            top_left_text = getTopLeftText(ui_title);
            RootNode.getBoundsInScreen(root_rect);
            getAllNodeInfo(RootNode,all_node_list);
            for(AccessibilityNodeInfo node : all_node_list){
                String classname = getClassName(node);
                node.getBoundsInScreen(node_rect);
                if("android.widget.LinearLayout".trim().equalsIgnoreCase(classname) && node_rect.equals(root_rect) && getPackageName(node).equalsIgnoreCase(Wx_PackageName) ){
                    linearLayout_node_list.add(node);
                }
            }
            if(linearLayout_node_list.size() == 1) linearLayout_node = linearLayout_node_list.get(0);
            if(linearLayout_node_list.size() == 0) linearLayout_node = null;
            int i = 0;
            for(AccessibilityNodeInfo node : linearLayout_node_list){
                AccessibilityNodeInfo sub_node = findNodebyText_Class(node,top_left_text,"android.widget.TextView");
                if(sub_node != null) {
                    linearLayout_node = node;
                    break;
                }
                i++;
            }
            //LogToFile.write("UI_Title=" + ui_title + ",top_left_text=" + top_left_text + ",i=" + String.valueOf(i) + ",linearLayout_node_list.size()=" + String.valueOf(linearLayout_node_list.size()) + ",nodeinfo=" + getClassName(linearLayout_node));
        }catch (Exception e){
            LogToFile.write("getLinearLayoutNodeinfo Fail!Error=" + e.getMessage());
            LogToFile.toast("getLinearLayoutNodeinfo Fail!Error=" + e.getMessage());
        }finally {
            return linearLayout_node;
        }
    }
    //region 获取listView node的所有子节点；
    //endregion
    public static void getAllNodeIncudeListView(AccessibilityNodeInfo info,List<AccessibilityNodeInfo> nodeList) {
        AccessibilityNodeInfo listView_node = null;
        List<AccessibilityNodeInfo> nodeInfoList = new ArrayList<AccessibilityNodeInfo>();
        boolean f = true;
        nodeList.clear();
        if(info != null) {
            getAllNodeInfo(info,nodeList);
            for(AccessibilityNodeInfo node : nodeList){
                if("android.widget.ListView".equalsIgnoreCase(getClassName(node)) && node.isScrollable() && getPackageName(node).equalsIgnoreCase(Wx_PackageName) ){
                    listView_node = node;break;
                }
            }
            while (f && listView_node != null){
                getAllNodeInfo(listView_node,nodeInfoList);
                for(AccessibilityNodeInfo node : nodeInfoList){
                    if(nodeList.contains(node) == false){
                        nodeList.add(node);
                        LogToFile.write(getText(node));
                    }
                }
                f = listView_node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                LogToFile.write("f=" + String.valueOf(f));
            }
        }
    }
    //region 获取node的所有子节点；
    //endregion
    public static void getAllNodeInfo(AccessibilityNodeInfo info,List<AccessibilityNodeInfo> nodeList) {
        if(info != null) {
            nodeList.add(info);
            if (info.getChildCount() == 0) {

            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        getAllNodeInfo(info.getChild(i), nodeList);
                    }
                }
            }
        }
    }
    //region 根据text、ClassName、ContentDesc 获取NodeList,返回node数量
    //endregion
    public static int findNodebyClass_Desc(AccessibilityNodeInfo root_node , String ClassName, String ContentDesc,List<AccessibilityNodeInfo> ret_nodeList){
        AccessibilityNodeInfo node = null;
        ret_nodeList.clear();
        int node_count = 0;
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = new ArrayList<AccessibilityNodeInfo>();
            getAllNodeInfo(root_node,nodeInfoList);
            int i = 0 ;
            for(AccessibilityNodeInfo sub_node : nodeInfoList)
            {
                //LogToFile.write(sub_node.toString());
                String class_name = getClassName(sub_node);
                if(ClassName.trim().equalsIgnoreCase(class_name) && getContentDescription(sub_node).equalsIgnoreCase(ContentDesc) && getPackageName(sub_node).equalsIgnoreCase(Wx_PackageName) ){
                    node  = sub_node;
                    //LogToFile.write("根据 ContentDesc =" + ContentDesc + ",NodeClass=" + ClassName + " 发现节点！");
                    ret_nodeList.add(node);
                    node_count++;
                }
            }

        }
        return node_count;
    }
    //region 根据ClassName、ContentDesc 获取 Node
    //endregion
    public static AccessibilityNodeInfo findNodebyClass_Desc(AccessibilityNodeInfo root_node , String ClassName, String ContentDesc){
        AccessibilityNodeInfo node = null;
        int node_count = 0;
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = new ArrayList<AccessibilityNodeInfo>();
            getAllNodeInfo(root_node,nodeInfoList);
            int i = 0 ;
            for(AccessibilityNodeInfo sub_node : nodeInfoList)
            {
                String class_name = getClassName(sub_node);
                if(ClassName.trim().equalsIgnoreCase(class_name) && getContentDescription(sub_node).equalsIgnoreCase(ContentDesc) && getPackageName(sub_node).equalsIgnoreCase(Wx_PackageName) ){
                    node  = sub_node;
                    node_count++;
                }
            }

        }
        return node;
    }

    //region 根据text、ClassName获取NodeList,返回node数量
    //endregion
    public static int findNodebyText_Class(AccessibilityNodeInfo root_node , String text, String ClassName,List<AccessibilityNodeInfo> ret_nodeList){
        AccessibilityNodeInfo node = null;
        ret_nodeList.clear();
        int node_count = 0;
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = root_node.findAccessibilityNodeInfosByText(text);
            if(nodeInfoList.size() == 0){
                //LogToFile.write("根据Text=" + text + "未发现节点！");
            }
            else{
                //LogToFile.write("根据Text=" + text + " 发现节点！count=" + String.valueOf(nodeInfoList.size()));
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    //LogToFile.write(sub_node.toString());
                    String class_name = getClassName(sub_node);
                    if(ClassName.trim().equalsIgnoreCase(class_name) && getPackageName(sub_node).equalsIgnoreCase(Wx_PackageName) && getText(sub_node).equalsIgnoreCase(text)){
                        node  = sub_node;
                        //LogToFile.write("根据Text=" + text + ",NodeClass=" + ClassName + " 发现节点！");
                        ret_nodeList.add(node);
                        node_count++;
                    }
                }
            }

        }
        return node_count;
    }
    //region 根据text、AccessibilityNodeInfo
    //endregion
    public static AccessibilityNodeInfo findNodebyText_Class(AccessibilityNodeInfo root_node , String text, String ClassName){
        AccessibilityNodeInfo node = null;
        int node_count = 0;
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = root_node.findAccessibilityNodeInfosByText(text);
            if(nodeInfoList.size() == 0){
                //LogToFile.write("根据Text=" + text + "未发现节点！");
            }
            else{
                //LogToFile.write("根据Text=" + text + " 发现节点！count=" + String.valueOf(nodeInfoList.size()));
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    //LogToFile.write(sub_node.toString());
                    String class_name = getClassName(sub_node);
                    if(ClassName.trim().equalsIgnoreCase(class_name) && getPackageName(sub_node).equalsIgnoreCase(Wx_PackageName) && getText(sub_node).equalsIgnoreCase(text)){
                        node_count++;
                        node = sub_node;
                        break;
                    }
                }
            }

        }
        return node;
    }

    //region 根据ID、ClassName获取Node
    //endregion
    public static AccessibilityNodeInfo findNodebyID_Class(AccessibilityNodeInfo root_node , String resource_id, String ClassName){
        AccessibilityNodeInfo node = null;
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = root_node.findAccessibilityNodeInfosByViewId(resource_id);
            if(nodeInfoList.size() == 0){
                //LogToFile.write("根据ID=" + resource_id + "未发现节点！");
            }
            else{
                //LogToFile.write("根据ID=" + resource_id + " 发现节点！count=" + String.valueOf(nodeInfoList.size()));
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    String class_name = getClassName(sub_node);
                    if(ClassName.trim().equalsIgnoreCase(class_name) && getPackageName(sub_node).equalsIgnoreCase(Wx_PackageName)){
                        node  = sub_node;
                        //LogToFile.write("根据ID=" + resource_id + ",NodeText=" + ClassName + " 发现节点！");
                        break;
                    }
                }
            }

        }
        return node;
    }

    //region 根据ID、ClassName获取NodeList
    //endregion
    public static int findNodebyID_Class(AccessibilityNodeInfo root_node , String resource_id, String ClassName,List<AccessibilityNodeInfo> ret_nodeList){
        AccessibilityNodeInfo node = null;
        int count = 0;
        ret_nodeList.clear();
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = root_node.findAccessibilityNodeInfosByViewId(resource_id);
            if(nodeInfoList.size() == 0){
                //LogToFile.write("根据ID=" + resource_id + "未发现节点！" + ",ClassName=" + ClassName);
            }
            else{
                //LogToFile.write("根据ID=" + resource_id + " 发现节点！count=" + String.valueOf(nodeInfoList.size()) + ",ClassName=" + ClassName);
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    String class_name = getClassName(sub_node);
                    if(ClassName.trim().equalsIgnoreCase(class_name) && getPackageName(sub_node).equalsIgnoreCase(Wx_PackageName)){
                        node  = sub_node;
                        //LogToFile.write("根据ID=" + resource_id + ",ClassName=" + ClassName + " 发现节点！");
                        ret_nodeList.add(node);
                        count++;
                    }
                }
            }

        }
        return count;
    }

    //region 根据ID、Text获取Node
    //endregion
    public static AccessibilityNodeInfo findNodebyID_Text(AccessibilityNodeInfo root_node , String resource_id, String NodeText){
        AccessibilityNodeInfo node = null;
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = root_node.findAccessibilityNodeInfosByViewId(resource_id);
            if(nodeInfoList.size() == 0){
                //LogToFile.write("根据ID=" + resource_id + "未发现节点！");
            }
            else{
                //LogToFile.write("根据ID=" + resource_id + " 发现节点！count=" + String.valueOf(nodeInfoList.size()));
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    String nodetext = getText(sub_node);
                    if(NodeText.trim().equalsIgnoreCase(nodetext) && getPackageName(sub_node).equalsIgnoreCase(Wx_PackageName)){
                        node  = sub_node;
                        //LogToFile.write("根据ID=" + resource_id + ",NodeText=" + NodeText + " 发现节点！");
                        break;
                    }
                }
            }

        }
        return node;
    }

    //region 根据ID、Desc、ClassName获取Node
    //endregion
    public static AccessibilityNodeInfo findNodebyID_Class_Desc(AccessibilityNodeInfo root_node , String resource_id, String ClassName, String NodeDesc){
        AccessibilityNodeInfo node = null;
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = root_node.findAccessibilityNodeInfosByViewId(resource_id);
            if(nodeInfoList.size() == 0){
                //LogToFile.write("根据ID=" + resource_id + "未发现节点！" + ",ClassName=" + ClassName + ",NodeText=" + NodeText);
            }
            else{
                //LogToFile.write("根据ID=" + resource_id + " 发现节点！count=" + String.valueOf(nodeInfoList.size()) + ",ClassName=" + ClassName + ",NodeText=" + NodeText);
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    String nodedesc = getContentDescription(sub_node);
                    String class_name = getClassName(sub_node);
                    if(ClassName.trim().equalsIgnoreCase(class_name) && NodeDesc.trim().equalsIgnoreCase(nodedesc) && getPackageName(sub_node).equalsIgnoreCase(Wx_PackageName)){
                        node  = sub_node;
                        //LogToFile.write("根据ID=" + resource_id + ",ClassName=" + ClassName + ",NodeText=" + NodeText + " 发现节点！");
                        break;
                    }
                }
            }

        }
        return node;
    }

    //region 根据ID、Text、ClassName获取Node
    //endregion
    public static AccessibilityNodeInfo findNodebyID_Class_Text(AccessibilityNodeInfo root_node , String resource_id, String NodeText, String ClassName){
        AccessibilityNodeInfo node = null;
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = root_node.findAccessibilityNodeInfosByViewId(resource_id);
            if(nodeInfoList.size() == 0){
                //LogToFile.write("根据ID=" + resource_id + "未发现节点！" + ",ClassName=" + ClassName + ",NodeText=" + NodeText);
            }
            else{
                //LogToFile.write("根据ID=" + resource_id + " 发现节点！count=" + String.valueOf(nodeInfoList.size()) + ",ClassName=" + ClassName + ",NodeText=" + NodeText);
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    String nodetext = getText(sub_node);
                    String class_name = getClassName(sub_node);
                    if(ClassName.trim().equalsIgnoreCase(class_name) && NodeText.trim().equalsIgnoreCase(nodetext) && getPackageName(sub_node).equalsIgnoreCase(Wx_PackageName)){
                        node  = sub_node;
                        //LogToFile.write("根据ID=" + resource_id + ",ClassName=" + ClassName + ",NodeText=" + NodeText + " 发现节点！");
                        break;
                    }
                }
            }

        }
        return node;
    }

    //region 根据ID、Text、ClassName获取 NodeCount
    //endregion
    public static int findNodebyID_Class_Text(AccessibilityNodeInfo root_node , String resource_id, String NodeText, String ClassName,List<AccessibilityNodeInfo> ret_nodeList){
        AccessibilityNodeInfo node = null;
        int NodeCount = 0;
        ret_nodeList.clear();
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = root_node.findAccessibilityNodeInfosByViewId(resource_id);
            if(nodeInfoList.size() == 0){
                //LogToFile.write("根据ID=" + resource_id + "未发现节点！");
            }
            else{
                //LogToFile.write("根据ID=" + resource_id + " 发现节点！count=" + String.valueOf(nodeInfoList.size()));
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    String nodetext = getText(sub_node);
                    String class_name = getClassName(sub_node);
                    if(ClassName.trim().equalsIgnoreCase(class_name) && NodeText.trim().equalsIgnoreCase(nodetext) && getPackageName(sub_node).equalsIgnoreCase(Wx_PackageName)){
                        node  = sub_node;
                        ret_nodeList.add(node);
                        //LogToFile.write("根据ID=" + resource_id + ",ClassName=" + ClassName + ",NodeText=" + NodeText + " 发现节点！");
                        NodeCount++;
                    }
                }
            }

        }
        return NodeCount;
    }

    //region 获取可点击的父节点，并根据参数进行点击
    //endregion
    public static AccessibilityNodeInfo clickParentNode(AccessibilityNodeInfo node , String resource_id, String NodeText , boolean isClick){
        AccessibilityNodeInfo parent = null;
        if(node == null){
            parent = null;
        }
        else
        {
            List<AccessibilityNodeInfo> nodeInfoList = node.findAccessibilityNodeInfosByViewId(resource_id);
            if(nodeInfoList.size() == 0){
                LogToFile.write("根据ID=" + resource_id + "未发现节点！");
            }
            else{
                LogToFile.write("根据ID=" + resource_id + " 发现节点！count=" + String.valueOf(nodeInfoList.size()));
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    String nodetext = getText(sub_node);
                    if(NodeText.trim().equalsIgnoreCase(nodetext)){
                        parent = sub_node.getParent();
                        while(parent != null)
                        {
                            if(parent.isClickable() ) {
                                if(isClick){
                                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    FuncTools.delay(delay_after_click);
                                }
                                break;
                            }
                            parent = parent.getParent();
                        }
                        break;
                    }
                }
            }

        }
        return parent;
    }

    //region 获取可点击的父节点
    //endregion
    public static AccessibilityNodeInfo getClickableParentNode(AccessibilityNodeInfo node){
        AccessibilityNodeInfo parent = null;
        if(node == null){
            parent = null;
        }
        else{
            parent = node;
            while(parent != null){
                if(parent.isClickable() ) {
                    break;
                }
                parent = parent.getParent();
            }

        }
        return parent;
    }

    //region 获取node的 ContentDescription ，以防null错误；
    //endregion
    public static String getContentDescription(AccessibilityNodeInfo node){
        String ret_str = "";
        try {
            if (node != null) {
                ret_str = node.getContentDescription() == null ? "" : node.getContentDescription().toString().trim();
            }
        }catch (Exception e){
            LogToFile.write("run Fail!Error=" + e.getMessage());
        }finally {
            return ret_str;
        }
    }

    //region 获取node的 ClassName ，以防null错误；
    //endregion
    public static String getClassName(AccessibilityNodeInfo node){
        String ret_str = "";
        try {
            if (node != null) {
                ret_str = node.getClassName() == null ? "" : node.getClassName().toString().trim();
            }
        }catch (Exception e){
            LogToFile.write("run Fail!Error=" + e.getMessage());
        }finally {
            return ret_str;
        }
    }

    //region 获取node的Text ，以防null错误；
    //endregion
    public static String getText(AccessibilityNodeInfo node){
        String ret_str = "";
        try {
            if (node != null) {
                ret_str = node.getText() == null ? "" : node.getText().toString().trim();
            }
        }catch (Exception e){
            LogToFile.write("run Fail!Error=" + e.getMessage());
        }finally {
            return ret_str;
        }
    }

    //region 获取node的PackageName ，以防null错误；
    //endregion
    public static String getPackageName(AccessibilityNodeInfo node){
        String ret_str = "";
        try {
            if (node != null) {
                ret_str = node.getPackageName() == null ? "" : node.getPackageName().toString().trim();
            }
        }catch (Exception e){
            LogToFile.write("run Fail!Error=" + e.getMessage());
        }finally {
            return ret_str;
        }
    }

    //region 获取node的 ID  ，以防null错误；
    //endregion
    public static String getViewIdResourceName(AccessibilityNodeInfo node){
        String ret_str = "";
        try {
            if (node != null) {
                ret_str = node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName().toString().trim();
            }
        }catch (Exception e){
            LogToFile.write("run Fail!Error=" + e.getMessage());
        }finally {
            return ret_str;
        }
    }

}
