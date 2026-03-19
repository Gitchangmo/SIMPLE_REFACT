package com.cookandroid.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
//import com.google.firebase.auth.FirebaseAuth;

public class FindPasswordActivity extends AppCompatActivity {
    private EditText email_EText, name_EText;
    private Button complete_Button;
    //private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_password);

        ImageButton Back_IButton = findViewById(R.id.back_IButton);
        email_EText = findViewById(R.id.email_EText);
        name_EText = findViewById(R.id.name_EText);
        complete_Button = findViewById(R.id.complete_Button);
       // mAuth = FirebaseAuth.getInstance();

        // 뒤로가기 화면 전환
        Back_IButton.setOnClickListener(v -> {
            Intent intent = new Intent(FindPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        complete_Button.setOnClickListener(view -> {
            String name = name_EText.getText().toString().trim();
            String email = email_EText.getText().toString().trim();
            String password = "123";
            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (name.isEmpty()) {
                Toast.makeText(this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            else {
                Toast.makeText(this, "비밀번호 찾기\n비밀번호는" + password + "입니다", Toast.LENGTH_SHORT).show();
            }
            // Firebase 비밀번호 재설정 이메일 전송
//            mAuth.sendPasswordResetEmail(email)
//                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "비밀번호 재설정 이메일을 보냈습니다.", Toast.LENGTH_LONG).show())
//                    .addOnFailureListener(e -> Toast.makeText(this, "오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}