package com.example.tiktokcloneproject.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.tiktokcloneproject.R;

public class SignupChoiceActivity extends Activity implements View.OnClickListener {
    Button btnChoicePhone, btnChoiceEmail;
    LinearLayout llSignupChoice;
    TextView txvTitle, txvAlt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_choice);

        llSignupChoice = (LinearLayout) findViewById(R.id.llSignupChoice);
        btnChoicePhone = (Button) llSignupChoice.findViewById(R.id.btnChoicePhone);
        btnChoiceEmail = (Button) llSignupChoice.findViewById(R.id.btnChoiceEmail);
        txvTitle = (TextView) llSignupChoice.findViewById(R.id.txvTitle);
        txvAlt = (TextView) llSignupChoice.findViewById(R.id.txv_alternative);

        txvTitle.setText(getString(R.string.sign_up));
        txvAlt.setText(getString(R.string.sign_up_alt));

        btnChoicePhone.setOnClickListener(this);
        btnChoiceEmail.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        // 1. 手机注册按钮 -> 跳转到评论区
        if(id == btnChoicePhone.getId()) {
            Log.d("TEST_FLOW", "尝试进入 CommentActivity");
            Intent intent = new Intent(SignupChoiceActivity.this, CommentActivity.class);
            startActivity(intent);
        }

        // 2. 邮箱注册按钮 -> 跳转到 Profile 页面
        if(id == btnChoiceEmail.getId()) {
            Log.d("TEST_FLOW", "尝试进入 ProfileActivity");

            // --- 核心改动：极致简化跳转，先不传任何 Extra 数据 ---
            Intent intent = new Intent(SignupChoiceActivity.this, ProfileActivity.class);

            // 加上这一行：确保在跳转前，当前 Activity 不会自作主张 finish
            // 如果你的 Base 类里有 finish 的逻辑，这里可以防止它运行
            startActivity(intent);
        }

        // 3. 底部文字 -> 原样保留
        if(id == txvAlt.getId()) {
            Intent intent = new Intent(SignupChoiceActivity.this, SigninChoiceActivity.class);
            startActivity(intent);
        }
    }
}