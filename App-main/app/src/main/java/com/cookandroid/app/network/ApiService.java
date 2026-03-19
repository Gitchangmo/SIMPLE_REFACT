package com.cookandroid.app.network;

import com.cookandroid.app.model.IngredientsRequest;
import com.cookandroid.app.model.MealPlanResponse;
import com.cookandroid.app.model.YoutubeRecipe;
import com.cookandroid.app.model.HomeRecipe;
import com.cookandroid.app.model.InventoryRequest;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
public interface ApiService {
    @POST("/search")
    Call<List<YoutubeRecipe>> getRecipes(@Body IngredientsRequest ingredients);

    // 만개의 레시피 오늘의 레시피추천
    @POST("/recommend")
    Call<List<HomeRecipe>> getRecommendedRecipes(@Body Map<String, String> body);

    // 일주일 식단 추천
    @POST("/recommend/mealplan")
    Call<MealPlanResponse> getMealPlan(@Body InventoryRequest request);
}
