package com.cookandroid.app.network;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cookandroid.app.MainActivity;
import com.cookandroid.app.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

// 파이어베이스 정보로 FCM에서 알림 받아오기
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String title = "";
        String body = "";
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        // 입출고 알림
        if (remoteMessage.getData().isEmpty()) {
            sendNotification(title, body, MainActivity.class);
        }
        // 2) 임박 유통기한 알림
        else {
            Map<String, String> data = remoteMessage.getData();
            String type = data.get("type");
            String foodNames = data.get("food_names");
            String dday = data.get("dday");
            String detail = data.get("detail");        // 알림 전체 본문(그룹별)

            // 알림별 화면 전환
            if ("expiry_alert".equals(type)) {
                sendNotification(title, detail, MainActivity.class, foodNames, dday);
            } else {
                // 기타 데이터 알림(미정의 시 메인으로)
                sendNotification(title, body, MainActivity.class);
            }
        }
    }

    // 오버로딩1 기본 알림: 클릭 시 MainActivity 등 단순 화면 이동
    private void sendNotification(String title, String body, Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.refrigerator)
                .setContentTitle(title)
                .setContentText(body)
                .setSubText("재고")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            manager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e("FCM_ERROR", "알림 권한 없음으로 notify 실패", e);
        }
    }

    // 오버로딩2 확장 알림: extra 데이터(Intent로 전달)
    private void sendNotification(String title, String body, Class<?> targetActivity, String foodNames, String dday) {
        Intent intent = new Intent(this, targetActivity);
        intent.putExtra("food_names", foodNames);
        intent.putExtra("dday", dday);
        intent.putExtra("detail", body); // 본문 전체 전달
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // 알림 클릭 시 재고 관리 화면 이동
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra("fragment", "inventory");
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 추천 받기 버튼 클릭 시 레시피 추천 화면 이동 (해당 재료들 전달)
        Intent recipeIntent = new Intent(this, MainActivity.class);
        recipeIntent.putExtra("fragment", "recipe");
        recipeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent recipePendingIntent = PendingIntent.getActivity(
                this, 1, recipeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.refrigerator)
                .setContentTitle(title)
                .setContentText(body.split("\n")[0])  // 첫 줄만 요약으로
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body)) // 전체 본문(여러 줄) 표시
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // 알림 자체 클릭 시 재고 관리 화면
                .addAction(R.drawable.recipe, "추천 받기", recipePendingIntent); // 추천 버튼

        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            manager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e("FCM_ERROR", "알림 권한 없음으로 notify 실패", e);
        }
    }
}