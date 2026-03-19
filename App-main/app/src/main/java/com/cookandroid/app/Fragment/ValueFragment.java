package com.cookandroid.app.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.R;
import com.cookandroid.app.adapter.FoodAdapter;
import com.cookandroid.app.model.Food;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class ValueFragment extends Fragment {
    private RecyclerView    recyclerView;
    private FoodAdapter      adapter;
    private List<Food>       foodList; // 생성된 Food 객체들 저장할 리스트
    private FirebaseAuth     mAuth;
    private FirebaseFirestore db;
    private String searchQuery = "";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_value,
                container, false);


        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.food_recycler_view);
        recyclerView
                .setLayoutManager(new GridLayoutManager(getContext(), 1));

        foodList = new ArrayList<>();
        adapter = new FoodAdapter(getContext(), foodList);
        recyclerView.setAdapter(adapter);

        // 검색창
        SearchView searchView = view.findViewById(R.id.food_search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query;
                Log.d("검색", "검색어: " + searchQuery);
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText;
                Log.d("검색", "변경된 검색어: " + searchQuery);
                adapter.filter(newText);
                return true;
            }
        });

        // + 재료추가 버튼 클릭 리스너
        Button addButton = view.findViewById(R.id.add_ingredient_button);
        addButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("mode", "stock");  // "재고" 추가용
            AddValueFragment fragment = new AddValueFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager()
                    .beginTransaction()
                    // 호스트 컨테이너 ID를 명시 (Activity layout 에 선언된 FrameLayout ID)
                    .replace(R.id.fragment_container,
                            new AddValueFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // ? 도움말 버튼 클릭 리스너
        Button helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HelpFragment())
                    .addToBackStack(null)
                    .commit();
        });

        loadFridgeData();
        return view;
    }


    void loadFridgeData() {
        String uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid()
                : null;
        if (uid == null) {
            Log.e("ValueFragment", "사용자 UID가 null입니다.");
            return;
        }

        db.collection("fridges")
                .document(uid)
                .collection("items")
                .addSnapshotListener((querySnapshot, e) -> { // 데이터에 추가/수정/삭제 시 리스너 콜백 -> 최신 스냅샷 전달
                    if (e != null || querySnapshot == null) {
                        Log.e("ValueFragment", "Firestore 실시간 로드 실패", e);
                        return;
                    }

                    foodList.clear();
                    int count = 0;
                    for (DocumentSnapshot doc : querySnapshot) { // 현재 시점의 저장된 파이어베이스 객체
                        try { // 스냅샷에 저장된 정보들 매핑
                            String imageName = doc.getString("imageName");
                            Log.d("ValueFragment", "imageName=" + imageName);
                            String docId   = doc.getId(); // 해당 음식 문서ID 가져오기

                            Food food = doc.toObject(Food.class); // 역직렬화로 객체 바로 생성
                            food.setDocumentId(docId);
                            foodList.add(food); // 생성한 객체를 foodList에 추가
                            count++;

                        } catch (Exception ex) {
                            Log.e("ValueFragment", "문서 파싱 중 오류", ex);
                        }
                    }

                    Log.d("ValueFragment", "파싱한 문서 수: " + count);
                    adapter.setData(foodList);
                    sortAndRefresh();

                    Log.d("ValueFragment", "로드 완료: " + foodList.size() + "개");
                });

    }

    public void sortAndRefresh() {
        Collections.sort(foodList, (f1, f2) -> {
            String d1 = f1.getDaysLeft();
            String d2 = f2.getDaysLeft();
            int score1 = getDdaySortScore(d1);
            int score2 = getDdaySortScore(d2);
            return Integer.compare(score1, score2);
        });
        adapter.notifyDataSetChanged();
    }

    private int getDdaySortScore(String dday) {
        if (dday == null || dday.isEmpty() || dday.equals("-")) return Integer.MAX_VALUE;

        try {
            if (dday.equals("D-DAY")) {
                return 1000;  // 가운데 정렬 기준
            } else if (dday.startsWith("D+")) {
                int num = Integer.parseInt(dday.substring(2));
                return -num;  // 큰 숫자가 앞쪽 (작은 값이 먼저)
            } else if (dday.startsWith("D-")) {
                int num = Integer.parseInt(dday.substring(2));
                return 1000 + num; // D-DAY보다 뒤로 정렬
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Integer.MAX_VALUE;
    }


}
