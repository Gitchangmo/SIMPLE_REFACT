package com.cookandroid.app.model;

public class HomeRecipe {
    private String title;
    private String image_url;
    private String url;
    private String difficulty;
    private String time;
    private String ingredient;
    private String selectedIngredient;
    public String getTitle() { return title; }
    public String getImage_url() { return image_url; }
    public String getUrl() { return url; }
    public String getDifficulty() { return difficulty; }
    public String getTime() { return time; }
    public String getIngredient() { return ingredient; }
    public String getSelectedIngredient() { return selectedIngredient; }

    public void setSelectedIngredient(String selectedIngredient) {
        this.selectedIngredient = selectedIngredient;
    }
}
