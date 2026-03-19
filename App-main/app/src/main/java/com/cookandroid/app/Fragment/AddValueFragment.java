package com.cookandroid.app.Fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.SearchView;


import com.cookandroid.app.R;
import com.cookandroid.app.Repository.FoodRepository;
import com.cookandroid.app.model.FoodItem;
import com.cookandroid.app.model.FoodListProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AddValueFragment extends Fragment {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AddValueAdapter adapter;
    private List<FoodItem> data = null;
    private String selectedCategory = "전체";
    private String searchQuery = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String mode = ""; // 기본값
        Bundle args = getArguments();
        if (args != null && args.containsKey("mode")) {
            mode = args.getString("mode");
        }

        View rootView = inflater.inflate(R.layout.activity_add_value, container, false);

        data = FoodListProvider.getFoodList(getContext());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        RecyclerView rv = rootView.findViewById(R.id.add_value_recycler);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));


        adapter = new AddValueAdapter(new ArrayList<>(data), mAuth, db, this, mode);
        // 원본 data 리스트 사용하지 않고 복사본 새로 생성해서 어댑터에 전달
        rv.setAdapter(adapter);

        setupCategoryButtons(rootView);

        SearchView searchView = rootView.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query;
                Log.d("검색", "검색어: " + searchQuery);
                filterItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText;
                Log.d("검색", "변경된 검색어: " + searchQuery);
                filterItems();
                return true;
            }
        });

        return rootView;
    }

    // 로그 생성 함수 동작 별 구분. 추가만 됨
    @Nullable
    public static void logHistory(
            String foodDocId,
            String foodName,
            String action,       // "put", "remove", "use", "dispose", "delete"
            int quantity,
            int remainAfter,
            @Nullable String recipeId,
            @Nullable String memo
    ) {
        Map<String, Object> log = new HashMap<>();
        log.put("foodDocId", foodDocId);
        log.put("foodName", foodName);
        log.put("action", action);        // "put", "remove", "use", "dispose", "delete"
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

    private void setupCategoryButtons(View rootView) {
        int[] buttonIds = new int[]{
                R.id.category_all,
                R.id.category_products,
                R.id.category_meat,
                R.id.category_grain,
                R.id.category_fruit,
                R.id.category_noodles,
                R.id.category_bread_ricecake,
                R.id.category_drinks_alcohol,
                R.id.category_vegetable,
                R.id.category_beans_nuts,
                R.id.category_seafood,
                R.id.category_seasoning,
                R.id.category_etc
        };

        String[] categories = new String[]{
                "전체", "가공/유제품", "고기", "곡물", "과일", "면",
                "빵/떡", "음료/주류", "채소", "콩/견과류", "해산물", "조미료/양념", "기타"
        };

        for (int i = 0; i < buttonIds.length; i++) {
            Button btn = rootView.findViewById(buttonIds[i]);
            String category = categories[i];
            btn.setOnClickListener(v -> {
                selectedCategory = category;
                Log.d("필터", "카테고리: " + selectedCategory);
                filterItems();
            });
        }
    }

    private void filterItems() {
        List<FoodItem> filtered = new ArrayList<>(); // 여기도 마찬가지로 복사본 생성
        String query = searchQuery.toLowerCase().trim();

        for (FoodItem item : data) {
            boolean matchesCategory = selectedCategory.equals("전체") || item.getCategory().equals(selectedCategory);
            boolean matchesSearch = item.getName().toLowerCase().contains(query);

            if (matchesCategory && matchesSearch) {
                filtered.add(item);
            }
        }
        Log.d("필터", "필터 결과 개수: " + filtered.size());
        adapter.updateList(filtered);
    }


    // 어댑터: 원래 쓰시던 생성자 서명 그대로
    static class AddValueAdapter
            extends RecyclerView.Adapter<AddValueAdapter.ViewHolder> {
        private List<FoodItem>    list;
        private final FirebaseAuth      mAuth;
        private final FirebaseFirestore db;
        private final Fragment          parentFragment;
        private final String mode;

        AddValueAdapter(List<FoodItem> list, FirebaseAuth mAuth, FirebaseFirestore db, Fragment parentFragment, String mode) {
            this.list           = list;
            this.mAuth          = mAuth;
            this.db             = db;
            this.parentFragment = parentFragment;
            this.mode           = mode;
        }

        public void updateList(List<FoodItem> newList) {
            this.list = new ArrayList<>(newList);
            notifyDataSetChanged();
        }


        // 리사이클러 뷰에 띄울 카드의 형태 설정
        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView foodImage;
            TextView foodName;
            Button    addButton;
            ViewHolder(View v) {
                super(v);
                foodImage = v.findViewById(R.id.food_image);
                foodName = v.findViewById(R.id.food_name);
                addButton = v.findViewById(R.id.add_button);
            }
        }

        // item_add_value의 UI에 따라 카드들을 띄움
        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                             int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_add_value,
                            parent, false);
            return new ViewHolder(view);
        }

        // 카드에 들어갈 데이터들을 바인드 함. 이름, 사진, 버튼
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FoodItem item = list.get(position); // FoodItem 객체 리스트 생성 item이라는 이름
            Log.d("fooddebug", "imageName=" + item.getImageName());
            int resId = holder.itemView.getContext().getResources().getIdentifier(
                    item.getImageName(), "drawable", holder.itemView.getContext().getPackageName()
            );
            if (resId != 0) {
                holder.foodImage.setImageResource(resId);
            } else {
                // 리소스 없으면 예비 이미지
                holder.foodImage.setImageResource(R.drawable.refrigerator);
            }
            holder.foodName.setText(item.getName());

            String mode_type = mode.equals("cart") ? "cart" : "stock";


            holder.addButton.setOnClickListener(v -> {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(v.getContext(),
                            "로그인 후 이용해주세요.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                String uid = user.getUid();
                // 오늘 날짜. 넣은 날짜 처리하기 위함
                LocalDate now = LocalDate.now();
                String today = now.toString();
                // 유통기한 설정
                LocalDate plus = now.plusDays(item.getExpirationDate()); // 해당 음식의 유통기한 받아와서 추가
                String plusDay = plus.toString(); // 유통기한 값을 문자열 형태로 바꿈

                if (mode_type.equals("cart")) {
                    // cart/{uid}/cart_items 컬렉션 참조
                    CollectionReference cartRef = db.collection("cart")
                            .document(uid)
                            .collection("cart_items");
                    // 같은 name 문서가 있는지 조회
                    cartRef.whereEqualTo("name", item.getName())
                            .get()
                            .addOnSuccessListener(query -> {
                                if (query.isEmpty()) {
                                    // 없으면 새로 추가
                                    Map<String,Object> newCartItem = new HashMap<>();
                                    newCartItem.put("name",         item.getName());
                                    newCartItem.put("quantity",     1);
                                    newCartItem.put("category",     item.getCategory());
                                    newCartItem.put("imageName",   item.getImageName());
                                    newCartItem.put("expirationDate", item.getExpirationDate()); // plusDay

                                    cartRef.add(newCartItem)
                                            .addOnSuccessListener(docRef -> {
                                                Toast.makeText(v.getContext(),
                                                        item.getName() + " 추가 완료!",
                                                        Toast.LENGTH_SHORT).show();
                                                parentFragment
                                                        .getParentFragmentManager()
                                                        .popBackStack();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(v.getContext(),
                                                            "추가 실패: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show()
                                            );
                                } else {
                                    // 있으면 quantity 필드만 +1
                                    DocumentSnapshot doc = query.getDocuments().get(0);
                                    doc.getReference()
                                            .update("quantity", FieldValue.increment(1))
                                            .addOnSuccessListener(aVoid -> {
                                                doc.getReference().get().addOnSuccessListener(updatedDoc -> {
                                                    Toast.makeText(v.getContext(),
                                                            item.getName() + " 수량 +1",
                                                            Toast.LENGTH_SHORT).show();
                                                    parentFragment
                                                            .getParentFragmentManager()
                                                            .popBackStack();
                                                });
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(v.getContext(),
                                                            "수량 업데이트 실패: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show()
                                            );
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(v.getContext(),
                                            "재고 조회 실패: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                }

                else if (mode_type.equals("stock")) {
                    FoodRepository foodRepo = new FoodRepository();
                    foodRepo.addFood(item, new FoodRepository.OnResultListener() {
                        @Override
                        public void onSuccess(String docId, boolean isNew) {
                            if (isNew) {
                                Toast.makeText(v.getContext(), item.getName() + " 추가 완료!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(v.getContext(), item.getName() + " 수량 +1!", Toast.LENGTH_SHORT).show();
                            }
                            parentFragment.getParentFragmentManager().popBackStack();
                        }
                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(v.getContext(), "추가 실패: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });



//                    // fridges/{uid}/items 컬렉션 참조
//                    CollectionReference itemsRef = db.collection("fridges")
//                            .document(uid)
//                            .collection("items");
//                    // 같은 name 문서가 있는지 조회
//                    itemsRef.whereEqualTo("name", item.getName())
//                            .get()
//                            .addOnSuccessListener(query -> {
//                                if (query.isEmpty()) {
//                                    // 없으면 새로 추가
//                                    Map<String,Object> newItem = new HashMap<>();
//                                    newItem.put("name",         item.getName());
//                                    newItem.put("category",     item.getCategory());
//                                    //newItem.put("info",         ""); // 여기에 사용자가 특이사항 같은 거 적게 해도 될 듯? -창모-
//                                    newItem.put("quantity",     1);
//                                    newItem.put("imageName",   item.getImageName());
//                                    newItem.put("addedDate", today);
//                                    newItem.put("expirationDate", plusDay);
//                                    newItem.put("storage", "냉장");
//
//
//                                    itemsRef.add(newItem)
//                                            .addOnSuccessListener(docRef -> {
//                                                String docID = docRef.getId();
//                                                // 새로 추가된 문서의 docId 필드에 해당 문서 ID 저장 PK라 생각하면 됨
//                                                docRef.update("documentId", docID);
//                                                // 사용자 행동 로그 기록
//                                                logHistory(docRef.getId(), item.getName(), "put", 1,
//                                                        1, null, "음식 추가");
//                                                Toast.makeText(v.getContext(),
//                                                        item.getName() + " 추가 완료!",
//                                                        Toast.LENGTH_SHORT).show();
//                                                parentFragment
//                                                        .getParentFragmentManager()
//                                                        .popBackStack();
//                                            })
//                                            .addOnFailureListener(e ->
//                                                    Toast.makeText(v.getContext(),
//                                                            "추가 실패: " + e.getMessage(),
//                                                            Toast.LENGTH_SHORT).show()
//                                            );
//                                } else {
//                                    // 있으면 quantity 필드만 +1
//                                    DocumentSnapshot doc = query.getDocuments().get(0);
//                                    doc.getReference()
//                                            .update("quantity", FieldValue.increment(1))
//                                            .addOnSuccessListener(aVoid -> {
//                                                doc.getReference().get().addOnSuccessListener(updatedDoc -> {
//                                                    Long remainAfter = updatedDoc.getLong("quantity");
//                                                    logHistory(doc.getId(), item.getName(), "put", 1,
//                                                            remainAfter.intValue(), null, "음식 추가");
//                                                    Toast.makeText(v.getContext(),
//                                                            item.getName() + " 수량 +1",
//                                                            Toast.LENGTH_SHORT).show();
//                                                    parentFragment
//                                                            .getParentFragmentManager()
//                                                            .popBackStack();
//                                                });
//                                            })
//                                            .addOnFailureListener(e ->
//                                                    Toast.makeText(v.getContext(),
//                                                            "수량 업데이트 실패: " + e.getMessage(),
//                                                            Toast.LENGTH_SHORT).show()
//                                            );
//                                }
//                            })
//                            .addOnFailureListener(e ->
//                                    Toast.makeText(v.getContext(),
//                                            "재고 조회 실패: " + e.getMessage(),
//                                            Toast.LENGTH_SHORT).show()
//                            );
                }
            });
        }


        @Override
        public int getItemCount() {
            return list.size();
        }
    }
}
