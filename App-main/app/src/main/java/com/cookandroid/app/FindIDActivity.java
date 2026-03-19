// 아이디 찾기 기능
package com.cookandroid.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class FindIDActivity extends AppCompatActivity {
    private EditText password_EText, name_EText;
    private TextView complete_Button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id);

        ImageButton Back_IButton = findViewById(R.id.back_IButton);
        password_EText = findViewById(R.id.password_EText);
        name_EText = findViewById(R.id.name_EText);
        complete_Button = findViewById(R.id.complete_Button);

        // 뒤로가기 화면 전환 수정
        Back_IButton.setOnClickListener(v -> {
            Intent intent = new Intent(FindIDActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        complete_Button.setOnClickListener(view -> {
            String password = password_EText.getText().toString().trim();
            String name = name_EText.getText().toString().trim();
            String email = "123";
            if (password.isEmpty()) {
                Toast.makeText(this, "회원가입 시 사용한 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (name.isEmpty()) {
                Toast.makeText(this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            else {
                Toast.makeText(this, "이메일 찾기\n이메일은" + email + "입니다", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

