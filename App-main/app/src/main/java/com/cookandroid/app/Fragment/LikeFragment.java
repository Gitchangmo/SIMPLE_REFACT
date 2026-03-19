package com.cookandroid.app.Fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.R;
import com.cookandroid.app.adapter.LikeFoodAdapter;
import com.cookandroid.app.model.Food;
import com.cookandroid.app.model.SharedViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class LikeFragment extends Fragment {
    private RecyclerView recyclerView;
    private LikeFoodAdapter adapter;
    private List<Food> selectedFoods = new ArrayList<>();
    private SharedViewModel sharedViewModel;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private View btnMeal;
    private View btnList;
    private View btnRecipe;
    private View btnFavorite;

    public LikeFragment() {
        super(R.layout.fragment_like);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        recyclerView = view.findViewById(R.id.like_food_list);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));

        // 탭 텍스트 및 인디케이터 참조
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

        // 선택된 탭에 따라 스타일을 적용하는 함수
        View.OnClickListener tabStyleUpdater = clickedView -> {
            for (int i = 0; i < tabTexts.length; i++) {
                boolean isSelected = tabTexts[i].getId() == clickedView.getId();
                tabIndicators[i].setVisibility(isSelected ? View.VISIBLE : View.GONE);
            }
        };


        // ✅ 전달받은 인덱스 값으로 밑줄 이동
        int selectedTabIndex = 0; // 기본값은 0 (레시피)
        if (getArguments() != null) {
            selectedTabIndex = getArguments().getInt("selectedTabIndex", 0);
        }
        tabStyleUpdater.onClick(tabTexts[selectedTabIndex]);


        // 프래그먼트 전환 버튼에 스타일 동기화 추가
        btnRecipe = view.findViewById(R.id.btn_recipe);
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



        btnMeal = view.findViewById(R.id.btn_meal);
        btnMeal.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("selectedTabIndex", 1); // 식단 = index 1

            DietFragment fragment = new DietFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        });



        btnList = view.findViewById(R.id.btn_list);
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



        btnFavorite = view.findViewById(R.id.btn_favorites);
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



        // 식재료 선택 후 레시피 보기 버튼
        Button recipeButton = view.findViewById(R.id.recipe_button);
        recipeButton.setOnClickListener(v -> {
            sharedViewModel.setSelectedFoodList(new ArrayList<>(selectedFoods));

            List<String> ingredientNames = new ArrayList<>();
            for (Food food : selectedFoods) {
                ingredientNames.add(food.getName());
            }
            sharedViewModel.setSelectedIngredients(ingredientNames);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new RecipeFragment())
                    .addToBackStack(null)
                    .commit();
        });

        loadFridgeItems();
    }


    // Firestore에서 사용자 냉장고 음식 불러오기
    private void loadFridgeItems() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("fridges")
                .document(uid)
                .collection("items")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {

                        selectedFoods.clear();
                        List<Food> foodList = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            try {
                                String name = doc.getString("name");
                                String info = doc.getString("info");
                                int qty = doc.getLong("quantity") != null
                                        ? doc.getLong("quantity").intValue()
                                        : 1;

//                                String imageUrl = doc.getString("imageUrl");
//                                Long resIdLong = doc.getLong("imageResId");
//                                int imageResId = resIdLong != null
//                                        ? resIdLong.intValue()
//                                        : R.drawable.check;

                                String expirationDate = doc.getString("expirationDate");
                                String addedDate = doc.getString("addedDate");
                                String docId = doc.getId();
                                String imageName = doc.getString("imageName");
                                String storage = doc.getString("storage");
                                String category = doc.getString("category");

                                Food food = new Food(
                                        name != null ? name : "이름 없음",
                                        info != null ? info : "정보 없음",
                                        qty,
                                        category != null ? category : "기타",
                                        addedDate != null ? addedDate : "날짜 없음",
                                        expirationDate != null ? expirationDate : "날짜 없음",
                                        imageName != null ? imageName : "이미지 없음",
                                        storage != null ? storage : "냉장"
                                );
                                Log.d("FoodDebug", "imageName=" + food.getImageName() + ", expDate=" + food.getExpirationDate());

                                food.setDocumentId(docId);
                                foodList.add(food);

                            } catch (Exception e) {
                                Log.e("LikeFragment", "문서 파싱 중 오류", e);
                            }
                        }

                        adapter = new LikeFoodAdapter(getContext(), foodList, food -> {
                            if (selectedFoods.contains(food)) {
                                selectedFoods.remove(food);
                            } else {
                                selectedFoods.add(food);
                            }

                            // ViewModel에 선택된 식재료 반영
                            sharedViewModel.setSelectedFoodList(new ArrayList<>(selectedFoods));

                            List<String> names = new ArrayList<>();
                            for (Food f : selectedFoods) {
                                names.add(f.getName());
                            }
                            sharedViewModel.setSelectedIngredients(names);

                            adapter.setSelectedFoods(selectedFoods);
                        });

                        recyclerView.setAdapter(adapter);
                        adapter.setSelectedFoods(selectedFoods);

                        Log.d("LikeFragment", "Firestore 로드 완료: " + foodList.size() + "개");

                    } else {
                        Log.e("LikeFragment", "Firestore 로드 실패", task.getException());
                    }
                });

    }
}
