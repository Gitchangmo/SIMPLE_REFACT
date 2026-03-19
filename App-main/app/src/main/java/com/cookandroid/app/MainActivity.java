package com.cookandroid.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cookandroid.app.Fragment.FoodFragmentChart;
import com.cookandroid.app.Fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.cookandroid.app.Fragment.HomeFragment;
import com.cookandroid.app.Fragment.LikeFragment;
import com.cookandroid.app.Fragment.ValueFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }

        // 알림 채널 등록
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "default",
                    "SIMPLE 스마트냉장고",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        String uid = mAuth.getCurrentUser().getUid();
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    db.collection("users").document(uid)
                            .update("fcmToken", token);
                });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        // 앱 실행 안한 상태에서 알림 탭 시 해당 화면으로 이동
        handleIntent(getIntent());

        // 기본적으로 HomeFragment를 표시
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();

        // ColorStateList로 아이콘 색상 변경
        ColorStateList colorStateList = createColorStateList();

        // 아이콘 색상 초기 설정 (홈 아이콘을 초록색으로 설정)
        bottomNavigationView.setItemIconTintList(colorStateList);

        // 홈 아이콘을 선택된 상태로 설정
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // 하단 네비게이션 바 클릭 이벤트 처리
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // 아이콘 색상 변경
            bottomNavigationView.setItemIconTintList(colorStateList);

            if (item.getItemId() == R.id.nav_home) {
                // 홈 화면 표시
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
                return true;
            }
             else if (item.getItemId() == R.id.nav_value) {
                // Refrigerator 화면 표시
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ValueFragment()).commit();
                return true;
            } else if (item.getItemId() == R.id.nav_like) {
                // Recipe 화면 표시
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new LikeFragment()).commit();
                return true;
            } else if (item.getItemId() == R.id.nav_notice) {
                // Shopping 화면 표시
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new FoodFragmentChart()).commit();
                return true;
            } else if (item.getItemId() == R.id.nav_profile) {
                // Notice 화면 표시
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFragment()).commit();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    // 공통 Intent 처리 함수
    private void handleIntent(Intent intent) {
        String fragmentToOpen = intent.getStringExtra("fragment");
        if ("inventory".equals(fragmentToOpen)) {
            bottomNavigationView.setSelectedItemId(R.id.nav_value);
            Log.d("DEBUG", "handleIntent: " + fragmentToOpen);
        } else if ("recipe".equals((fragmentToOpen))) {
            bottomNavigationView.setSelectedItemId(R.id.nav_like);
        }
    }

    // ColorStateList 생성 (선택된 화면은 진한 초록, 선택되지 않은 화면은 연한 회색)
    private ColorStateList createColorStateList() {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked }, // 선택된 아이템
                new int[] {} // 선택되지 않은 아이템
        };

        int[] colors = new int[] {
                getResources().getColor(R.color.green_dark), // 진한 초록색
                getResources().getColor(R.color.gray_light)  // 연한 회색
        };

        return new ColorStateList(states, colors);
    }


}
