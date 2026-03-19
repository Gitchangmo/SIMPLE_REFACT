package com.cookandroid.app.Fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.R;
import com.cookandroid.app.adapter.MealAdapter;
import com.cookandroid.app.model.InventoryItem;
import com.cookandroid.app.model.InventoryRequest;
import com.cookandroid.app.model.Meal;
import com.cookandroid.app.model.MealItem;
import com.cookandroid.app.model.MealPlanResponse;
import com.cookandroid.app.model.WeekDayPlan;
import com.cookandroid.app.network.ApiService;
import com.cookandroid.app.network.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 주간 식단 페이지

public class DietFragment extends Fragment {
    private View btnRecipe;
    private View btnList;
    private View btnFavorite;
    private RecyclerView recyclerView;
    private MealAdapter mealAdapter;
    private Map<Integer, List<MealItem>> weekMealMap; // 0~6 : 월~일
    private ApiService api; // 일주일 식단 추천 api

    public DietFragment() {
        super(R.layout.fragment_diet);

    }

    private Map<Integer, List<MealItem>> convertWeekPlanToMap(List<Map<String, Object>> weekPlanList) {
        Map<Integer, List<MealItem>> map = new HashMap<>();
        String[] days = {"월", "화", "수", "목", "금", "토", "일"};
        for (int i = 0; i < weekPlanList.size(); i++) {
            Map<String, Object> dayPlan = weekPlanList.get(i);
            List<Map<String, Object>> mealsRaw = (List<Map<String, Object>>) dayPlan.get("meals");
            List<MealItem> meals = new ArrayList<>();
            for (Map<String, Object> meal : mealsRaw) {
                String type = (String) meal.get("type");
                String title = (String) meal.get("title");
                String desc = (String) meal.get("desc");
                String imageUrl = (String) meal.get("imageUrl");
                String link = (String) meal.get("link");
                // 안전하게 변환 (Firestore에서 Array는 List로 내려옴)
                List<String> usedIngredients = meal.get("usedIngredients") != null ?
                        (List<String>) meal.get("usedIngredients") : new ArrayList<>();

                meals.add(new MealItem(type, title, desc, imageUrl, link, usedIngredients));
            }
            map.put(i, meals);
        }
        return map;
    }


    private void loadLatestWeeklyPlanFromDB(String userID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("weekly_meal")
                .document(userID)
                .collection("meals")
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1) // 현재는 제일 최신에 저장된 정보 가져오는 형식임
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // 문서 1개만 가져옴
                        Map<String, Object> doc = querySnapshot.getDocuments().get(0).getData();
                        if (doc != null && doc.containsKey("week_plan")) {
                            // week_plan이 실제로 List<Map<String, Object>> 형식임
                            Object weekPlanObj = doc.get("week_plan");
                            List<Map<String, Object>> weekPlanList = (List<Map<String, Object>>) weekPlanObj;
                            weekMealMap = convertWeekPlanToMap(weekPlanList);
                            mealAdapter.setMealList(weekMealMap.get(0));
                        }
                    } else {
                        // 저장된 식단 없음 (초기 상태 안내)
                        mealAdapter.setMealList(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    // DB 통신 실패
                    mealAdapter.setMealList(new ArrayList<>());
                });
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvRecipe = view.findViewById(R.id.tv_recipe);
        TextView tvMeal = view.findViewById(R.id.tv_meal);
        TextView tvList = view.findViewById(R.id.tv_list);
        TextView tvFavorites = view.findViewById(R.id.tv_favorites);

        TextView tvWeekLabel = view.findViewById(R.id.tvWeekLabel);
        TextView tvWeekRange = view.findViewById(R.id.tvWeekRange);

        View indicatorRecipe = view.findViewById(R.id.indicator_recipe);
        View indicatorMeal = view.findViewById(R.id.indicator_meal);
        View indicatorList = view.findViewById(R.id.indicator_list);
        View indicatorFavorites = view.findViewById(R.id.indicator_favorites);

        TextView[] tabTexts = {tvRecipe, tvMeal, tvList, tvFavorites};
        View[] tabIndicators = {indicatorRecipe, indicatorMeal, indicatorList, indicatorFavorites};

        tvWeekLabel.setText(getCurrentWeekLabel());
        tvWeekRange.setText(getCurrentWeekRange());

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


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userID = FirebaseAuth.getInstance().getUid();

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

        btnList = view.findViewById(R.id.btn_list); // 버튼 ID 확인 필요
        btnList.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("selectedTabIndex", 2); // 장보기 = index 2

            ListFragment fragment = new ListFragment();
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

        // 일주일 식단 데이터 받아오기
        loadLatestWeeklyPlanFromDB(userID);

        // 일주일 식단 추천 받기 버튼 클릭 리스너
        LinearLayout btnRecommend = view.findViewById(R.id.btnRecommend);
        btnRecommend.setOnClickListener(v -> {
            api = RetrofitClient.getClient().create(ApiService.class);

            List<InventoryItem> inventory = new ArrayList<>(); // Firestore 등에서 받아온 재고 리스트

            db.collection("fridges")
                    .document(userID)
                    .collection("items")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            // 각 도큐먼트에서 필드 읽기 (이름/수량/유통기한/카테고리)
                            String name = doc.getString("name");
                            Long quantity = doc.getLong("quantity");
                            String expiryDate = doc.getString("expirationDate");
                            String category = doc.getString("category");
                            inventory.add(new InventoryItem(name, String.valueOf(expiryDate),
                                    quantity != null ? quantity.intValue() : 1, category));
                           }

                        // 이제 inventory 리스트 완성 → 서버로 전송
                        String userId = FirebaseAuth.getInstance().getUid();
                        InventoryRequest request = new InventoryRequest(userId, inventory);
                        // Retrofit POST 요청 (비동기)
                        Call<MealPlanResponse> call = api.getMealPlan(request);
                        call.enqueue(new Callback<MealPlanResponse>() {
                            @Override
                            public void onResponse(Call<MealPlanResponse> call, Response<MealPlanResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    MealPlanResponse mealPlan = response.body();

                                    Log.d("DietFragment", "response.body(): " + new Gson().toJson(mealPlan));
                                    Log.d("DietFragment", "mealPlan.getWeek(): " + mealPlan.getWeek());

                                    // weekPlan null 체크!
                                    if (mealPlan.getWeek() == null) {
                                        Log.e("DietFragment", "mealPlan.getWeek() is null!!");
                                        return;
                                    } else {
                                        Log.d("DietFragment", "mealPlan.getWeek().size(): " + mealPlan.getWeek().size());
                                    }
                                    Log.d("DietFragment", "response.body(): " + new Gson().toJson(response.body()));

                                    // 서버에서 받은 데이터 → 요일별 Map<Integer, List<Meal>>으로 변환
                                    weekMealMap = new HashMap<>();
                                    List<WeekDayPlan> weekList = mealPlan.getWeek(); // getWeek()가 List<WeekDayPlan> 리턴

                                    for (int i = 0; i < weekList.size(); i++) {
                                        WeekDayPlan dayPlan = weekList.get(i);
                                        weekMealMap.put(i, dayPlan.getMeals());  // 0:월, 1:화...
                                    }
//                                    // 기본 월요일 식단 표시
//                                    mealAdapter.setMealList(weekMealMap.get(0));
                                    // 기본 월요일 식단 표시 (null-safe)
                                    List<MealItem> mondayList = weekMealMap.get(0);
                                    if (mondayList != null)
                                        mealAdapter.setMealList(mondayList);
                                    else
                                        mealAdapter.setMealList(new ArrayList<>());
                                }
                            }

                            @Override
                            public void onFailure(Call<MealPlanResponse> call, Throwable t) {
                                Log.e("DietFragment", "일주일 식단 호출 실패");
                                t.printStackTrace();
                                // 실패 처리
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        // 실패 처리
                    });
        });

        // RecyclerView 세팅
        recyclerView = view.findViewById(R.id.recyclerMeals);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mealAdapter = new MealAdapter();
        recyclerView.setAdapter(mealAdapter);


        // 요일 버튼 세팅 (findViewById로 7개 버튼 참조)
        TextView[] btns = {
                view.findViewById(R.id.btnMon),
                view.findViewById(R.id.btnTue),
                view.findViewById(R.id.btnWed),
                view.findViewById(R.id.btnThu),
                view.findViewById(R.id.btnFri),
                view.findViewById(R.id.btnSat),
                view.findViewById(R.id.btnSun)
        };
        View[] tabDayIndicators = {
                view.findViewById(R.id.indicatorMon),
                view.findViewById(R.id.indicatorTue),
                view.findViewById(R.id.indicatorWed),
                view.findViewById(R.id.indicatorThu),
                view.findViewById(R.id.indicatorFri),
                view.findViewById(R.id.indicatorSat),
                view.findViewById(R.id.indicatorSun)
        };

        for (int i = 0; i < btns.length; i++) {
            final int idx = i;
            btns[i].setOnClickListener(v -> {
                for (int j = 0; j < 7; j++) {
                    tabDayIndicators[j].setVisibility(View.INVISIBLE);
                    btns[j].setTextColor(Color.parseColor("#222222")); // 기본색
                }
                tabDayIndicators[idx].setVisibility(View.VISIBLE);
                btns[idx].setTextColor(Color.parseColor("#3582F5"));

                List<MealItem> dayList = (weekMealMap != null) ? weekMealMap.get(idx) : null;
                if (dayList != null)
                    mealAdapter.setMealList(dayList);
                else
                    mealAdapter.setMealList(new ArrayList<>()); // 비워줌
            });
        }


    }

    public static String getCurrentWeekLabel() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1; // 1~12
        int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH); // 1~5
        return month + "월 " + weekOfMonth + "주차";
    }

    public static String getCurrentWeekRange() {
        Calendar cal = Calendar.getInstance();
        // 이번주 월요일로 이동
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        SimpleDateFormat sdf = new SimpleDateFormat("M/d");
        String start = sdf.format(cal.getTime());
        cal.add(Calendar.DATE, 6);
        String end = sdf.format(cal.getTime());
        return start + " ~ " + end;
    }

}
