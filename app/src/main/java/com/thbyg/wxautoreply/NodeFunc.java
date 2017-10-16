package com.thbyg.wxautoreply;

import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;

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

    /**
     * 根据text、ClassName获取Node
     */
    public static AccessibilityNodeInfo findNodebyText_Class(AccessibilityNodeInfo root_node , String text, String ClassName)
    {
        AccessibilityNodeInfo node = null;
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = root_node.findAccessibilityNodeInfosByText(text);
            if(nodeInfoList.size() == 0){
                LogToFile.write("根据Text=" + text + "未发现节点！");
            }
            else{
                LogToFile.write("根据Text=" + text + " 发现节点！count=" + String.valueOf(nodeInfoList.size()));
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    String class_name = sub_node.getClassName().toString().trim();
                    if(ClassName.trim().equalsIgnoreCase(class_name) && sub_node.getPackageName().toString().trim().equalsIgnoreCase(Wx_PackageName) && sub_node.getText().toString().trim().equalsIgnoreCase(text)){
                        node  = sub_node;
                        LogToFile.write("根据Text=" + text + ",NodeText=" + ClassName + " 发现节点！node=" + node.toString());
                        //break;
                    }
                }
            }

        }
        return node;
    }
    /**
     * 根据ID、ClassName获取Node
     */
    public static AccessibilityNodeInfo findNodebyID_Class(AccessibilityNodeInfo root_node , String resource_id, String ClassName)
    {
        AccessibilityNodeInfo node = null;
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = root_node.findAccessibilityNodeInfosByViewId(resource_id);
            if(nodeInfoList.size() == 0){
                LogToFile.write("根据ID=" + resource_id + "未发现节点！");
            }
            else{
                LogToFile.write("根据ID=" + resource_id + " 发现节点！count=" + String.valueOf(nodeInfoList.size()));
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    String class_name = sub_node.getClassName().toString().trim();
                    if(ClassName.trim().equalsIgnoreCase(class_name) && sub_node.getPackageName().toString().trim().equalsIgnoreCase(Wx_PackageName)){
                        node  = sub_node;
                        LogToFile.write("根据ID=" + resource_id + ",NodeText=" + ClassName + " 发现节点！node=" + node.toString());
                        break;
                    }
                }
            }

        }
        return node;
    }
    /**
     * 根据ID、Text获取Node
     */
    public static AccessibilityNodeInfo findNodebyID_Text(AccessibilityNodeInfo root_node , String resource_id, String NodeText)
    {
        AccessibilityNodeInfo node = null;
        if(root_node != null){
            List<AccessibilityNodeInfo> nodeInfoList = root_node.findAccessibilityNodeInfosByViewId(resource_id);
            if(nodeInfoList.size() == 0){
                LogToFile.write("根据ID=" + resource_id + "未发现节点！");
            }
            else{
                LogToFile.write("根据ID=" + resource_id + " 发现节点！count=" + String.valueOf(nodeInfoList.size()));
                int i = 0 ;
                for(AccessibilityNodeInfo sub_node : nodeInfoList)
                {
                    String nodetext = sub_node.getText().toString().trim();
                    if(NodeText.trim().equalsIgnoreCase(nodetext) && sub_node.getPackageName().toString().trim().equalsIgnoreCase(Wx_PackageName)){
                        node  = sub_node;
                        LogToFile.write("根据ID=" + resource_id + ",NodeText=" + NodeText + " 发现节点！node=" + node.toString());
                        break;
                    }
                }
            }

        }
        return node;
    }

    /**
     *获取可点击的父节点，并根据参数进行点击
     */
    public static AccessibilityNodeInfo clickParentNode(AccessibilityNodeInfo node , String resource_id, String NodeText , boolean isClick)
    {
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
                    String nodetext = sub_node.getText().toString().trim();
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

}
