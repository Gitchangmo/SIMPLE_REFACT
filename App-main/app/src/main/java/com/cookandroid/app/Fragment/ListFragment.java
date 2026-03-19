package com.cookandroid.app.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.R;
import com.cookandroid.app.adapter.CartAdapter;
import com.cookandroid.app.model.CartItem;
import com.cookandroid.app.model.Food;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 장바구니 페이지

public class ListFragment extends Fragment {
    private View btnRecipe;
    private View btnDiet;
    private View btnFavorite;
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private List<CartItem> cartList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public ListFragment() {
        super(R.layout.fragment_cart);

    }

    // 로그 생성 함수 동작 별 구분. 추가만 됨
    @Nullable
    public void logHistory(
            String foodDocId,
            String foodName,
            String category,
            String action,       // "put", "remove", "use", "discard", "delete"
            int quantity,
            int remainAfter,
            @Nullable String recipeId,
            @Nullable String memo
    ) {
        Map<String, Object> log = new HashMap<>();
        log.put("foodDocId", foodDocId);
        log.put("foodName", foodName);
        log.put("category", category);
        log.put("action", action);        // 예: "put", "remove", "use", "discard", "delete"
        log.put("quantity", quantity);    // +N 또는 -N
        log.put("remainAfter", remainAfter);
        log.put("timestamp", FieldValue.serverTimestamp());
        if (recipeId != null) log.put("recipeId", recipeId);
        if (memo != null)     log.put("memo", memo);

        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Firestore에 저장
        FirebaseFirestore.getInstance()
                .collection("fridges").document(userID)
                .collection("history")
                .add(log)
                .addOnSuccessListener(ref -> Log.d("History", "저장 성공"))
                .addOnFailureListener(e -> Log.e("History", "저장 실패: " + e.getMessage()));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvRecipe = view.findViewById(R.id.tv_recipe);
        TextView tvMeal = view.findViewById(R.id.tv_meal);
        TextView tvList = view.findViewById(R.id.tv_list);
        TextView tvFavorites = view.findViewById(R.id.tv_favorites);

        View indicatorRecipe = view.findViewById(R.id.indicator_recipe);
        View indicatorMeal = view.findViewById(R.id.indicator_meal);
        View indicatorList = view.findViewById(R.id.indicator_list);
        View indicatorFavorites = view.findViewById(R.id.indicator_favorites);

        TextView[] tabTexts = {tvRecipe, tvMeal, tvList, tvFavorites};
        View[] tabIndicators = {indicatorRecipe, indicatorMeal, indicatorList, indicatorFavorites};

        View.OnClickListener tabStyleUpdater = clickedView -> {
            for (int i = 0; i < tabTexts.length; i++) {
                boolean isSelected = tabTexts[i].getId() == clickedView.getId();
                tabIndicators[i].setVisibility(isSelected ? View.VISIBLE : View.GONE);
            }
        };

        int selectedTabIndex = 0;
        if (getArguments() != null) {
            selectedTabIndex = getArguments().getInt("selectedTabIndex", 0);
        }
        tabStyleUpdater.onClick(tabTexts[selectedTabIndex]);


        btnRecipe = view.findViewById(R.id.btn_recipe); // 버튼 ID 확인 필요
        btnRecipe.setOnClickListener(v -> {
            Bundle bundle = new Bundle();                  // ✅ 1. 번들 생성
            bundle.putInt("selectedTabIndex", 0);         // ✅ 2. 선택한 탭 번호 저장 (레시피 = 0)

            LikeFragment fragment = new LikeFragment();   // ✅ 3. 프래그먼트 생성
            fragment.setArguments(bundle);                // ✅ 4. 번들 연결

            getParentFragmentManager()                    // ✅ 5. 프래그먼트 전환
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        });

        btnDiet = view.findViewById(R.id.btn_meal); // 버튼 ID 확인 필요
        btnDiet.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("selectedTabIndex", 1); // 식단 = index 1

            DietFragment fragment = new DietFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        });

        btnFavorite = view.findViewById(R.id.btn_favorites); // 버튼 ID 확인 필요
        btnFavorite.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("selectedTabIndex", 3); // 즐겨찾기 = index 3

            FavoriteFragment fragment = new FavoriteFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        });



        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.cart_recycler_view);
        recyclerView
                .setLayoutManager(new LinearLayoutManager(getContext()));

        cartList = new ArrayList<>();
        adapter = new CartAdapter(getContext(), cartList);
        recyclerView.setAdapter(adapter);

        // + 재료추가 버튼 클릭 리스너
        Button add_cart_list_button = view.findViewById(R.id.add_cart_list_button);
        add_cart_list_button.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("mode", "cart");  // "장바구니" 추가용
            AddValueFragment fragment = new AddValueFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container,
                            fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // 재고에 바로 추가 버튼
        Button btnAddToInventory = view.findViewById(R.id.btn_add_to_inventory);
        btnAddToInventory.setOnClickListener(v -> {
            List<CartItem> checkedItems = new ArrayList<>();
            for (CartItem cart_item : cartList) {
                if (cart_item.isChecked()) {
                    checkedItems.add(cart_item);
                }
            }
            // 체크된 음식이 없을 때
            if (checkedItems.isEmpty()) {
                Toast.makeText(getContext(), "음식을 선택해주세요!", Toast.LENGTH_SHORT).show();
                return; // 함수 종료
            }

            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // 오늘 날짜. 넣은 날짜 처리하기 위함
            LocalDate now = LocalDate.now();
            String today = now.toString();


            CollectionReference itemRef = db.collection("fridges")
                    .document(userID)
                    .collection("items");
            CollectionReference cartRef = db.collection("cart")
                    .document(userID).collection("cart_items");


            for (CartItem cart_item : checkedItems) {
                itemRef.whereEqualTo("name", cart_item.getName())
                        .get()
                        .addOnSuccessListener(query -> {
                            // 유통기한 설정
                            int expireDays = cart_item.getExpirationDate(); // 이 부분 다시 확인하기 getter문젠가?
                            Log.d("DEBUG", "expireDays = " + expireDays);
                            Log.d("DEBUG", "getExpirationDate = " + cart_item.getExpirationDate());
                            LocalDate plus = now.plusDays(expireDays);
                            String plusDay = plus.toString();
                            if (query.isEmpty()) {
                                Map<String, Object> newItem = new HashMap<>();
                                newItem.put("name", cart_item.getName());
                                newItem.put("category", cart_item.getCategory());
                                newItem.put("quantity", cart_item.getQuantity());
                                newItem.put("imageName", cart_item.getImageName());
                                newItem.put("addedDate", today);
                                newItem.put("expirationDate", plusDay);
                                newItem.put("storage", "냉장");

                                itemRef.add(newItem).addOnSuccessListener(docRef -> {
                                    logHistory(docRef.getId(), cart_item.getName(), cart_item.getCategory(), "put",
                                            cart_item.getQuantity(), cart_item.getQuantity(), null, "음식 추가");
                                });
                                String docID = cart_item.getDocumentId();

                                cartRef.document(docID).delete();
                            }
                            else {
                                // 있으면 quantity 필드만 증가
                                DocumentSnapshot doc = query.getDocuments().get(0);
                                Long nowQty = doc.getLong("quantity");
                                int newQty = nowQty.intValue() + cart_item.getQuantity(); // 기존 재고와 장바구니 재고 수량 합
                                doc.getReference()
                                        .update("quantity", newQty)
                                        .addOnSuccessListener(aVoid -> {
                                            // 사용자 행동 로그 기록
                                            doc.getReference().get().addOnSuccessListener(updatedDoc -> {
                                                Log.d("HistoryDebug", "userID=" + userID);
                                                Log.d("HistoryDebug", "foodDocId=" + doc.getId()
                                                        + ", foodName=" + cart_item.getName());
                                                Long remainAfter = updatedDoc.getLong("quantity");
                                                logHistory(doc.getId(), cart_item.getName(), cart_item.getCategory(), "put",
                                                        cart_item.getQuantity(),
                                                        remainAfter.intValue(), null, "음식 추가");
                                                Toast.makeText(v.getContext(),
                                                        cart_item.getName() + " 수량 +" + cart_item.getQuantity(),
                                                        Toast.LENGTH_SHORT).show();
                                                getParentFragmentManager()
                                                        .popBackStack();
                                            });
                                            String docID = cart_item.getDocumentId();
                                            cartRef.document(docID).delete();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(v.getContext(),
                                                        "수량 업데이트 실패: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show()
                                        );
                            }

                        });
            }
            cartList.removeAll(checkedItems);
            adapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "재고에 추가되었습니다!", Toast.LENGTH_SHORT).show();
        });

        loadCartData();
    }
    private void loadCartData() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Log.e("ValueFragment", "사용자 UID가 null입니다.");
            return;
        }

        db.collection("cart")
                .document(uid)
                .collection("cart_items")
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Log.e("ValueFragment",
                                "Firestore 로드 실패",
                                task.getException());
                        return;
                    }
                    cartList.clear();
                    for (DocumentSnapshot doc : task.getResult()) {
                        try {
                            String documentId = doc.getId();
                            String name = doc.getString("name");
                            Long qtyLong = doc.getLong("quantity");
                            String imageName = doc.getString("imageName");
                            String category = doc.getString("category");
                            Long exp = doc.getLong("expirationDate");
                            int qty = (qtyLong != null) ? qtyLong.intValue() : 1;
                            int expirationDate = (exp != null) ? exp.intValue() : 1;

                            CartItem cartItem = new CartItem(
                                    documentId,
                                    imageName,
                                    name  != null ? name  : "이름 없음",
                                    qty,
                                    category,
                                    false,
                                    expirationDate
                            );
                            cartList.add(cartItem);
                        } catch (Exception e) {
                            Log.e("ValueFragment",
                                    "문서 파싱 중 오류", e);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("ValueFragment",
                            "로드 완료: " + cartList.size() + "개");
                });
    }

}
