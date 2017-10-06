package com.thbyg.wxautoreply;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import static com.thbyg.wxautoreply.RootShellCmd.execShellCmd;

public class MainActivity extends AppCompatActivity {
    RadioGroup radiogroup;
    EditText edit_text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LogToFile.init(this);
        //设置所有Radiogroup的状态改变监听器

        radiogroup = (RadioGroup)findViewById(R.id.radiogroup1);
        radiogroup.setOnCheckedChangeListener(mylistener);
        //LogToFile.write("sample text");
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
}
