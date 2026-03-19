package com.cookandroid.app.Fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.R;
import com.cookandroid.app.adapter.RecipeAdapter;
import com.cookandroid.app.model.IngredientsRequest;
import com.cookandroid.app.model.SharedViewModel;
import com.cookandroid.app.model.YoutubeRecipe;
import com.cookandroid.app.network.ApiService;
import com.cookandroid.app.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeFragment extends Fragment {

    private RecipeAdapter recipeAdapter;
    private RecyclerView recipeRecyclerView;
    private SharedViewModel sharedViewModel;
    private LinearLayout selectedIngredientsContainer;

    // 레시피 필터링
    private static final int FILTER_REQUEST_CODE = 1001;
    private List<YoutubeRecipe> originalList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe, container, false);
        // 필터링 버튼 클릭 시 filtering.java로 이동
        View filterButton = view.findViewById(R.id.filter_button);

        filterButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.cookandroid.app.filtering.class);
            startActivityForResult(intent, FILTER_REQUEST_CODE); // 수정
        });


        selectedIngredientsContainer = view.findViewById(R.id.selected_ingredients_container);
        recipeRecyclerView = view.findViewById(R.id.recipeRecyclerView);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recipeAdapter = new RecipeAdapter(new ArrayList<>(), getContext());
        recipeRecyclerView.setAdapter(recipeAdapter);

        // 선택된 재료 ViewModel에서 받아오기
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        List<String> selectedIngredients = sharedViewModel.getSelectedIngredients();

        // 선택한 재료 UI로 보여주기
        for (String ingredient : selectedIngredients) {
            TextView textView = new TextView(getContext());
            textView.setText(ingredient);
            textView.setPadding(20, 10, 20, 10);
            textView.setBackgroundResource(R.drawable.rounded_background); // 선택된 재료 스타일
            selectedIngredientsContainer.addView(textView);
        }

        // 서버에 요청 보내기
        fetchRecipesFromServer(selectedIngredients);

        return view;
    }
    //필터링 결과 처리
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILTER_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            String keyword = data.getStringExtra("categoryKeyword");
            if (keyword != null) {
                List<YoutubeRecipe> filtered = applyCategoryFilter(originalList, keyword);
                recipeAdapter.setRecipes(filtered);
            }
        }
    }

    private void fetchRecipesFromServer(List<String> selectedIngredients) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        IngredientsRequest request = new IngredientsRequest(selectedIngredients);

        apiService.getRecipes(request).enqueue(new Callback<List<YoutubeRecipe>>() {
            @Override
            public void onResponse(Call<List<YoutubeRecipe>> call, Response<List<YoutubeRecipe>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    originalList = response.body();
                    recipeAdapter.setRecipes(response.body());
                } else {
                    Log.e("RecipeFragment", "서버 응답 실패");
                }
            }

            @Override
            public void onFailure(Call<List<YoutubeRecipe>> call, Throwable t) {
                Log.e("RecipeFragment", "서버 요청 실패", t);
            }
        });
        Log.d("선택된 재료", "선택된 개수: " + selectedIngredients.size());
        for (String ing : selectedIngredients) {
            Log.d("선택된 재료", ing);
        }

    }
    // 필터링 함수
    private List<YoutubeRecipe> applyCategoryFilter(List<YoutubeRecipe> source, String keyword) {
        List<YoutubeRecipe> filtered = new ArrayList<>();
        for (YoutubeRecipe item : source) {
            if (item.getTitle().contains(keyword) || item.getDescription().contains(keyword)) {
                filtered.add(item);
            }
        }
        return filtered;
    }
}