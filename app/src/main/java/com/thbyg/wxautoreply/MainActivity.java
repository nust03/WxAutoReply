package com.thbyg.wxautoreply;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import static com.thbyg.wxautoreply.RootShellCmd.execShellCmd;

public class MainActivity extends AppCompatActivity {
    RadioGroup radiogroup;
    EditText edit_text;
    private PackageManager mPackageManager;
    private String[] mPackages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LogToFile.init(this);
        //设置所有Radiogroup的状态改变监听器

        radiogroup = (RadioGroup) findViewById(R.id.radiogroup1);
        radiogroup.setOnCheckedChangeListener(mylistener);
        mPackageManager = this.getPackageManager();
        mPackages = new String[]{"com.tencent.mm"};
        BaseAccessibilityService.getInstance().init(this);
        final String service = "com.thbyg.wxautoreply/.AutomationService";
        LogToFile.write("检查辅助服务是否开启？service=" + service);
        if (BaseAccessibilityService.getInstance().checkAccessibilityEnabled(service)) {
            Toast.makeText(this, service + " 辅助服务已开启。", Toast.LENGTH_SHORT).show();
        } else {
            BaseAccessibilityService.getInstance().goAccess();

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        edit_text = (EditText) findViewById(R.id.et_AutoInput);
        edit_text.setText("");
        edit_text.setFocusable(true);
        edit_text.setFocusableInTouchMode(true);
        edit_text.requestFocus();
    }

    RadioGroup.OnCheckedChangeListener mylistener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup Group, int Checkid) {
            // TODO Auto-generated method stub
            //设置TextView的内容显示CheckBox的选择结果
            String str;
            EditText edit_text = (EditText) findViewById(R.id.et_InputCMD);
            edit_text.setText("");
            RadioButton radioButton = (RadioButton) findViewById(radiogroup.getCheckedRadioButtonId());
            str = radioButton.getText().toString();
            edit_text.setText(str);
        }
    };

    public void btn_GetRoot(View view) {
        RootUtil.get_root();
        //this.getSystemService()
    }
    /*
     * "模拟输入"按钮点击事件，根据点选框的字符串进行模拟输入。
     */
    public void btn_execShellCmd(View view) {
        EditText edit_text = (EditText) findViewById(R.id.et_InputCMD);
        String cmd = edit_text.getText().toString().trim();
        RootShellCmd.execShellCmd(cmd);

    }
    public void btn_Adb_Accessibility_Reboot(View view) {
        final String my_service = getPackageName() + "/" + AutomationService.class.getCanonicalName();
        if(Valid_Open_Accessibility()) LogToFile.toast(my_service + " 辅助服务已开启。");
        else LogToFile.toast("Valid_Open_Accessibility return false.");
        String command_str = "";
        ShellUtils.CommandResult cr;
        command_str = "ls -l";
        cr = ShellUtils.execCommand(new String[]{command_str},true,true);
        LogToFile.write("successMsg=" + cr.successMsg + ",errorMsg=" + cr.errorMsg + ",accessibility_enabled=" + cr.accessibility_enabled + ",enabled_accessibility_services=" + cr.enabled_accessibility_services);

    }
    /*
     * btn_Open_Accessibility按钮点击事件，用来判断辅助服务是否开启。
     */
    public void btn_Open_Accessibility(View view) {

        final String service = "com.thbyg.wxautoreply/.AutomationService";
        LogToFile.write("检查辅助服务是否开启？service=" + service);
        if (BaseAccessibilityService.getInstance().checkAccessibilityEnabled(service)) {
            Toast.makeText(this, service + " 辅助服务已开启。", Toast.LENGTH_SHORT).show();
        } else {
            BaseAccessibilityService.getInstance().goAccess();
        }

    }
    /*
     * 校验辅助服务是否开启，如未开启，后台自动打开。
     */
    public boolean Valid_Open_Accessibility() {
        final String my_service = getPackageName() + "/" + AutomationService.class.getCanonicalName();
        boolean f = false;
        try {
            if (BaseAccessibilityService.getInstance().isAccessibilitySettingsOn(this, my_service)) {
                LogToFile.write(my_service + " 辅助服务已开启。");
                f = true;
                //LogToFile.toast(my_service + " 辅助服务已开启。");
            }
            else {
                LogToFile.write(my_service + " 辅助服务未已开启。后台开启...");
                //LogToFile.toast("I am running...从settings.db获取辅助服务参数");
                String command_str = "";
                ShellUtils.CommandResult cr;
                command_str = "sqlite3  /data/data/com.android.providers.settings/databases/settings.db";
                String select_str = "select * from secure;";
                cr = ShellUtils.execCommand(new String[]{command_str,".headers on",select_str},true,true);
                //LogToFile.write("successMsg=" + cr.successMsg + ",errorMsg=" + cr.errorMsg + ",accessibility_enabled=" + cr.accessibility_enabled + ",enabled_accessibility_services=" + cr.enabled_accessibility_services);
                if(cr.result == 2){
                    String value = "";
                    if(cr.enabled_accessibility_services.contains(my_service) == false) {
                        if(cr.enabled_accessibility_services.isEmpty()){
                            value = my_service;
                        }
                        else value = cr.enabled_accessibility_services + ":" + my_service;
                        command_str = "sqlite3  /data/data/com.android.providers.settings/databases/settings.db";
                        String update_str1 = "update secure set value='" + value.trim() + "' where name='enabled_accessibility_services';";
                        String update_str2 = "update secure set value=1 where name='accessibility_enabled';";
                        cr = ShellUtils.execCommand(new String[]{command_str, ".headers on", update_str1, update_str2, select_str}, true, true);
                        //LogToFile.write("successMsg=" + cr.successMsg + ",errorMsg=" + cr.errorMsg + ",accessibility_enabled=" + cr.accessibility_enabled + ",enabled_accessibility_services=" + cr.enabled_accessibility_services);
                        FuncTools.delay(300);
                        if (BaseAccessibilityService.getInstance().isAccessibilitySettingsOn(this, my_service)) {
                            LogToFile.write(my_service + " 辅助服务已开启。");
                            f = true;
                            //LogToFile.toast(my_service + " 辅助服务已开启。");
                        }
                    }
                }

            }
        }catch (Exception e){
            LogToFile.write("Valid_Open_Accessibility Fail!Error=" + e.getMessage());
            LogToFile.toast("Valid_Open_Accessibility Fail!Error=" + e.getMessage());
            f = false;
        }
        return  f;
    }
    /*
    btn_Open_App按钮点击事件，用来启动微信。
     */
    public void btn_Open_App(View view) {
/*
        Intent intent = mPackageManager.getLaunchIntentForPackage("com.tencent.mm");
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        startActivity(intent);


        boolean f = FuncTools.isAppForeground(NodeFunc.Wx_PackageName);
        LogToFile.toast("f=" + String.valueOf(f));
        FuncTools.bring2Front(NodeFunc.Wx_PackageName);
        */

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName componentName = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
        intent.setComponent(componentName);
        startActivity(intent);

        //BaseAccessibilityService.getInstance().printNodeInfo();
    }


    public void btn_SendLogMail(View view) {
        String path = LogToFile.getLogFile().getPath();
        MailManager.getInstance().sendMailWithFile("nust03@163.com", "APP运行日志", "APP run log.", path);
        Toast.makeText(this, "发送邮件到nust03@163.com成功。", Toast.LENGTH_SHORT).show();
    }

    public void btn_DelLogFile(View view) {
        if (LogToFile.delLogFile())
            LogToFile.toast("日志文件删除成功！file=" + LogToFile.logFile.getPath() + "/" + LogToFile.logFile.getName());
    }


}
