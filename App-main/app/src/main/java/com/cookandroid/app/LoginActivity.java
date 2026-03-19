package com.cookandroid.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {
    private EditText idEditText, passwordEditText;
    private Button completeButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // firebase auth 인스턴스 초기화
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        TextView FindId_Button = findViewById(R.id.find_email_Button);
        TextView FindPassword_Button = findViewById(R.id.find_password_Button);
        TextView FindSignUp_Button = findViewById(R.id.find_Signup_Button);

        //2.11 수정
        idEditText = findViewById(R.id.email_EText);
        passwordEditText = findViewById(R.id.password_EText);
        completeButton = findViewById(R.id.complete_Button);


        // 아이디 찾기 화면 전환
        FindId_Button.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, FindIDActivity.class);
            startActivity(intent);
        });
        // 비밀번호 찾기 화면 전환
        FindPassword_Button.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, FindPasswordActivity.class);
            startActivity(intent);
        });

        // 회원가입 화면 전환
        FindSignUp_Button.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        completeButton.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String id = idEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(id)) {
            Toast.makeText(this, "아이디를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase 로그인 처리
        mAuth.signInWithEmailAndPassword(id, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 로그인 성공
                        FirebaseUser user = mAuth.getCurrentUser();
                        String uid = user.getUid();
                        // FCM 토큰 받아서 Firestore에 업데이트 (로그인 할 때마다 업뎃됨)
                        FirebaseMessaging.getInstance().getToken()
                                .addOnSuccessListener(token -> {
                                    db.collection("users")
                                            .document(uid)
                                            .update("fcmToken", token);
                                });
                        Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                        // MainActivity로 이동
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish(); // 현재 액티비티 종료
                    } else {
                        // 로그인 실패
                        Toast.makeText(LoginActivity.this, "로그인 실패. 아이디와 비밀번호를 확인하세요.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}

