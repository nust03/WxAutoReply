package com.thbyg.wxautoreply;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Created by myxiang on 2017/10/10.
 */

public class PrintNode {
    String print(AccessibilityNodeInfo nodeInfo) {

        CharSequence text = nodeInfo.getText();
        CharSequence description = nodeInfo.getContentDescription();
        CharSequence packageName = nodeInfo.getPackageName();
        CharSequence className = nodeInfo.getClassName();
        boolean focusable = nodeInfo.isFocusable();
        boolean clickable = nodeInfo.isClickable();
        Rect rect = new Rect();
        nodeInfo.getBoundsInScreen(rect);

        return "| " +
                "text: " + text + " \t" +
                "description: " + description + " \t" +
                "location: " + rect + " \t" +
                "package name: " + packageName + " \t" +
                "class name: " + className + " \t" +
                "focusable: " + focusable + " \t" +
                "clickable: " + clickable + " \t" +
                '\n';

    }

    //传了参数就只打印这个节点下的所有自节点
    PrintNode(AccessibilityNodeInfo n) {
        LogToFile.init(AppContext.getContext());
        show(n);
    }

    private void show(AccessibilityNodeInfo n) {
        try {
            LogToFile.write("==============================================开始打印");
            LogToFile.write("\nv0                            " + print(n));
            int v1 = n.getChildCount();
            for (int i1 = 0; i1 < v1; i1++) {
                AccessibilityNodeInfo n1 = n.getChild(i1);
                LogToFile.write("\n    v1: " + i1 + "                     " + print(n1));
                int v2 = n1.getChildCount();
                for (int i2 = 0; i2 < v2; i2++) {
                    AccessibilityNodeInfo n2 = n1.getChild(i2);
                    LogToFile.write("\n        v2: " + i2 + "                 " + print(n2));
                    int v3 = n2.getChildCount();
                    for (int i3 = 0; i3 < v3; i3++) {
                        AccessibilityNodeInfo n3 = n2.getChild(i3);
                        LogToFile.write("\n            v3: " + i3 + "             " + print(n3));
                        int v4 = n3.getChildCount();
                        for (int i4 = 0; i4 < v4; i4++) {
                            AccessibilityNodeInfo n4 = n3.getChild(i4);
                            LogToFile.write("\n                v4: " + i4 + "         " + print(n4));
                            int v5 = n4.getChildCount();
                            for (int i5 = 0; i5 < v5; i5++) {
                                AccessibilityNodeInfo n5 = n4.getChild(i5);
                                LogToFile.write("\n                    v5: " + i5 + "     " + print(n5));
                                int v6 = n5.getChildCount();
                                for (int i6 = 0; i6 < v6; i6++) {
                                    AccessibilityNodeInfo n6 = n5.getChild(i6);
                                    LogToFile.write("\n                        v6: " + i6 + " " + print(n6));
                                }
                            }
                        }
                    }
                }
            }
            LogToFile.write("==============================================结束打印");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
