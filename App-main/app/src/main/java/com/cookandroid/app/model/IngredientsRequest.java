package com.cookandroid.app.model;

import java.util.List;

public class IngredientsRequest {
    private List<String> ingredients;

    public IngredientsRequest(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }
}

