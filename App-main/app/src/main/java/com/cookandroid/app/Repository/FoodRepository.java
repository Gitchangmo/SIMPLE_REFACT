package com.cookandroid.app.Repository;

import android.util.Log;

import androidx.annotation.Nullable;

import com.cookandroid.app.model.FoodItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class FoodRepository {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

    // 콜백 인터페이스
    public interface OnResultListener {
        void onSuccess(String docId, boolean isNew);
        void onFailure(String error);
    }

    // fridges/{uid}/items 컬렉션 참조
    CollectionReference itemsRef = db.collection("fridges")
            .document(uid)
            .collection("items");

    // 재고 추가
    public void addFood(FoodItem item, OnResultListener cb) {
        itemsRef.whereEqualTo("name", item.getName()).get()
                .addOnSuccessListener(query -> {
                    // 오늘 날짜. 넣은 날짜 처리하기 위함
                    LocalDate now = LocalDate.now();
                    String today = now.toString();
                    // 유통기한 설정
                    LocalDate plus = now.plusDays(item.getExpirationDate()); // 해당 음식의 유통기한 받아와서 추가
                    String plusDay = plus.toString(); // 유통기한 값을 문자열 형태로 바꿈

                    if (query.isEmpty()) {
                        // 없으면 새로 추가
                        Map<String,Object> newItem = new HashMap<>();
                        newItem.put("name",         item.getName());
                        newItem.put("category",     item.getCategory());
                        //newItem.put("info",         ""); // 여기에 사용자가 특이사항 같은 거 적게 해도 될 듯? -창모-
                        newItem.put("quantity",     1);
                        newItem.put("imageName",   item.getImageName());
                        newItem.put("addedDate", today);
                        newItem.put("expirationDate", plusDay);
                        newItem.put("storage", "냉장");

                        itemsRef.add(newItem)
                                .addOnSuccessListener(docRef -> {
                                    String docID = docRef.getId();
                                    // 새로 추가된 문서의 docId 필드에 해당 문서 ID 저장 PK라 생각하면 됨
                                    docRef.update("documentId", docID);
                                    logHistory(docRef.getId(), item.getName(), "put", 1, 1, "음식 추가");
                                    cb.onSuccess(docID, true);
                                })
                                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
                    } else {
                        // 있으면 quantity만 +1 하기
                        DocumentSnapshot doc = query.getDocuments().get(0);
                        doc.getReference().update("quantity", FieldValue.increment(1))
                                .addOnSuccessListener(aVoid -> {
                                    logHistory(doc.getId(), item.getName(), "put", 1, /*remainAfter*/2, "수량+1");
                                    cb.onSuccess(doc.getId(), false);
                                })
                                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    // 로그 기록 함수
    @Nullable
    public void logHistory(
            String foodDocId,
            String foodName,
            String action,       // "put", "remove", "use", "dispose", "delete"
            int quantity,
            int remainAfter,
            @Nullable String memo
    ) {
        Map<String, Object> log = new HashMap<>();
        log.put("foodDocId", foodDocId);
        log.put("foodName", foodName);
        log.put("action", action);        // "put", "remove", "use", "dispose", "delete"
        log.put("quantity", quantity);    // +N 또는 -N
        log.put("remainAfter", remainAfter);
        log.put("timestamp", FieldValue.serverTimestamp());
        if (memo != null)     log.put("memo", memo);

        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Firestore에 저장
        db.collection("fridges").document(userID).collection("history").add(log)
                .addOnSuccessListener(ref -> Log.d("History", "저장 성공"))
                .addOnFailureListener(e -> Log.e("History", "저장 실패: " + e.getMessage()));
    }
}
