package com.cookandroid.app.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.cookandroid.app.model.Food;

import java.util.ArrayList;
import java.util.List;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<List<Food>> foodListLiveData = new MutableLiveData<>();
    private final List<String> selectedIngredients = new ArrayList<>();

    private final MutableLiveData<List<Food>> selectedFoodListLiveData = new MutableLiveData<>();

    // ✅ foodList를 저장하는 메서드
    public void setFoodList(List<Food> foodList) {
        foodListLiveData.setValue(foodList);
    }

    // ✅ foodList를 가져오는 메서드
    public LiveData<List<Food>> getFoodList() {
        return foodListLiveData;
    }

    public void setSelectedFoodList(List<Food> selectedFoodList) {
        selectedFoodListLiveData.setValue(selectedFoodList);
    }
    public void setSelectedIngredients(List<String> ingredients) {
        selectedIngredients.clear();
        selectedIngredients.addAll(ingredients);
    }
    public LiveData<List<Food>> getSelectedFoodList() {
        return selectedFoodListLiveData;
    }
    public List<String> getSelectedIngredients() {
        return selectedIngredients;
    }
}