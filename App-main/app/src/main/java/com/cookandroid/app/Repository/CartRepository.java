package com.cookandroid.app.Repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class CartRepository {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    // 콜백 인터페이스
    public interface OnResultListener {
        void onSuccess(String docId, boolean isNew);
        void onFailure(String error);
    }

    CollectionReference cartRef = db.collection("cart")
            .document(uid)
            .collection("cart_items");


}
