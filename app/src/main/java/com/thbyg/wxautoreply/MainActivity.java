package com.thbyg.wxautoreply;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

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

        radiogroup = (RadioGroup)findViewById(R.id.radiogroup1);
        radiogroup.setOnCheckedChangeListener(mylistener);
        BaseAccessibilityService.getInstance().init(this);
        mPackageManager = this.getPackageManager();
        mPackages = new String[]{"com.tencent.mm"};
        //LogToFile.write("sample text");
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

    RadioGroup.OnCheckedChangeListener mylistener=new RadioGroup.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(RadioGroup Group, int Checkid) {
            // TODO Auto-generated method stub
            //设置TextView的内容显示CheckBox的选择结果
            String str;
            EditText edit_text = (EditText) findViewById(R.id.et_InputCMD);
            edit_text.setText("");
            RadioButton radioButton = (RadioButton)findViewById(radiogroup.getCheckedRadioButtonId());
            str=radioButton.getText().toString();
            edit_text.setText(str);
        }
    };
    public void btn_GetRoot(View view) {
        RootUtil.get_root();
    }

    public void btn_execShellCmd(View view) {
        EditText edit_text = (EditText) findViewById(R.id.et_InputCMD);
        String cmd = edit_text.getText().toString().trim();
        RootShellCmd.execShellCmd(cmd);

    }

    /*
btn_Open_Accessibility按钮点击事件，用来判断辅助服务是否开启。
 */
    public void btn_Open_Accessibility(View view) {
        if (BaseAccessibilityService.getInstance().checkAccessibilityEnabled("com.thbyg.wxautoreply/.AutomationService")) {
            Toast.makeText(this, "com.thbyg.wxautoreply/.AutomationService 辅助服务已开启。", Toast.LENGTH_SHORT).show();
        } else {
            BaseAccessibilityService.getInstance().goAccess();
        }
    }

    /*
    btn_Open_App按钮点击事件，用来启动微信。
     */
    public void btn_Open_App(View view) {
        Intent intent = mPackageManager.getLaunchIntentForPackage("com.tencent.mm");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        //startActivity(intent);

        //BaseAccessibilityService.getInstance().printNodeInfo();
    }


    public void btn_SendLogMail(View view) {
        String path = LogToFile.getLogFile().getPath();
        MailManager.getInstance().sendMailWithFile("15651499096@wo.cn", "APP运行日志", "APP run log.", path);
        Toast.makeText(this, "发送邮件到15651499096@wo.cn成功。", Toast.LENGTH_SHORT).show();
    }
}