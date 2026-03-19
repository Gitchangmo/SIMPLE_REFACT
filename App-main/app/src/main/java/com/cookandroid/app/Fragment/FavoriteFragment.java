package com.cookandroid.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.R;
import com.cookandroid.app.adapter.RecipeAdapter;
import com.cookandroid.app.model.YoutubeRecipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

// 레시피 즐겨찾기 페이지

public class FavoriteFragment extends Fragment {
    private View btnRecipe;
    private View btnDiet;
    private View btnList;
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private List<YoutubeRecipe> favoriteList = new ArrayList<>();

    public FavoriteFragment() {
        super(R.layout.fragment_favorite);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        recyclerView = view.findViewById(R.id.favorite_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new RecipeAdapter(favoriteList, getContext());
        recyclerView.setAdapter(adapter);

        loadFavoriteRecipes();  // Firestore 데이터 불러오기 함수 호출

    }

    private void loadFavoriteRecipes() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("fridges")
                .document(userId)
                .collection("recipe")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    favoriteList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        YoutubeRecipe recipe = new YoutubeRecipe();
                        recipe.setTitle(doc.getString("title"));
                        recipe.setVideo_url(doc.getString("video_url"));
                        recipe.setThumbnail_url(doc.getString("thumbnail_url"));
                        recipe.setDescription(doc.getString("description"));
                        favoriteList.add(recipe);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
