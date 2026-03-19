package com.cookandroid.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

import com.google.firebase.messaging.FirebaseMessaging;

import com.google.firebase.auth.SignInMethodQueryResult;



public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isEmailValid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // 파이어베이스 초기화
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ImageButton Back_IButton = findViewById(R.id.back_IButton);
        Button Complete_Button = findViewById(R.id.complete_Button);
        EditText Name_EText = findViewById(R.id.name_EText);
        EditText Email_EText = findViewById(R.id.email_EText);
        EditText Password_EText = findViewById(R.id.password_EText);
        EditText Password_Retry_EText = findViewById(R.id.password_retry_EText);
        TextView Password_Warning_Text = findViewById(R.id.password_warning_Text);
        TextView Name_Warning_Text = findViewById(R.id.name_warning_Text);
        TextView Email_Warning_Text = findViewById(R.id.email_warning_Text);

        Name_EText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String name = editable.toString();
                if (name.isEmpty()) {
                    Name_Warning_Text.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    Name_Warning_Text.setText("이름을 입력하세요.");
                } else {
                    Name_Warning_Text.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    Name_Warning_Text.setText("올바른 이름");
                }
            }
        });

        Password_EText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String password = editable.toString();
                if (!isValidPassword(password)) {
                    Password_Warning_Text.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    Password_Warning_Text.setText("비밀번호는 6자-20자 이하, 대소문자, 숫자, 특수문자(._!) 포함해야 합니다.");
                } else {
                    Password_Warning_Text.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
                    Password_Warning_Text.setText("올바른 비밀번호");
                }
            }
        });

        Password_Retry_EText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                checkPasswordMatch(Password_EText, Password_Retry_EText);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        Email_EText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String email = editable.toString();

                // 🔍 이메일 형식 유효성 먼저 체크
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    isEmailValid = false;
                    Email_Warning_Text.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    Email_Warning_Text.setText("올바른 이메일 형식이 아닙니다.");
                    return;
                }

                mAuth.fetchSignInMethodsForEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                SignInMethodQueryResult result = task.getResult();
                                if (result != null && result.getSignInMethods() != null && result.getSignInMethods().isEmpty()) {
                                    isEmailValid = true;
                                    Email_Warning_Text.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                    Email_Warning_Text.setText("사용 가능한 이메일입니다.");
                                } else {
                                    isEmailValid = false;
                                    Email_Warning_Text.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                                    Email_Warning_Text.setText("이미 사용 중인 이메일입니다.");
                                }
                            } else {
                                isEmailValid = false;
                                Email_Warning_Text.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                                Email_Warning_Text.setText("이메일 확인 실패: " + task.getException().getMessage());
                            }
                        });
            }
        });


        // 뒤로가기 화면 전환
        Back_IButton.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // 회원가입 완료 후 로그인 화면으로 전환 -> 완료 후 파이어베이스에 저장
        Complete_Button.setOnClickListener(v -> {
            String name = Name_EText.getText().toString().trim();
            String password = Password_EText.getText().toString().trim();
            String passwordCheck = Password_Retry_EText.getText().toString().trim();
            String email = Email_EText.getText().toString().trim();

            if (name.isEmpty() || password.isEmpty() || passwordCheck.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(passwordCheck)) {
                Toast.makeText(SignUpActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPassword(password)) {
                Toast.makeText(SignUpActivity.this, "비밀번호 형식을 확인하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isEmailValid) {
                Toast.makeText(SignUpActivity.this, "사용 가능한 이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (email.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!checkPasswordMatch(Password_EText, Password_Retry_EText)) {
                Toast.makeText(SignUpActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔹 Firebase를 이용한 회원가입
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            saveUserToFirestore(uid, name, email);

                            Toast.makeText(SignUpActivity.this, "회원가입 성공!", Toast.LENGTH_SHORT).show();

                            Log.d("SignUpActivity", "회원가입 성공!");

                            // 로그인 화면으로 이동
                            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(SignUpActivity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            Log.d("가입요청", "입력된 이메일: " + name);

                        }
                    });
        });
    }

    //오류문구 이상으로 수정
    private boolean checkPasswordMatch(EditText Password_EText, EditText Password_Retry_EText) {
        String password = Password_EText.getText().toString();
        String passwordCheck = Password_Retry_EText.getText().toString();
        return password.equals(passwordCheck);
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6 && password.length() <= 20 &&
                password.matches(".*[A-Za-z].*") &&
                password.matches(".*[0-9].*") &&
                password.matches(".*[!._].*");
    }

    private void saveUserToFirestore(String uid, String name, String email) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    HashMap<String, Object> user = new HashMap<>();
                    user.put("uid", uid);
                    user.put("name", name);
                    user.put("email", email);
                    user.put("createdAt", System.currentTimeMillis());
                    user.put("fcmToken", token); // 토큰 저장
                    Log.d("TOKEN", token);
                    db.collection("users")
                            .document(uid)  // uid로 문서 저장하는 걸 추천
                            .set(user)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(SignUpActivity.this, "회원 정보 저장 완료", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(SignUpActivity.this, "회원 정보 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this, "FCM 토큰 받기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

