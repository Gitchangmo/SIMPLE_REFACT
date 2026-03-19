package com.cookandroid.app.model;

import java.util.List;

public class Recipe {
    private String name;
    private List<String> ingredients;
    private String url;

    // 생성자
    public Recipe(String name, List<String> ingredients, String url) {
        this.name = name;
        this.ingredients = ingredients;
        this.url = url;
    }

    // 기본 생성자 (Firestore에서 자동 매핑 위해 필요)
    public Recipe() {}

    // Getter 메서드들
    public String getName() {
        return name;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public String getUrl() {
        return url;
    }
}
