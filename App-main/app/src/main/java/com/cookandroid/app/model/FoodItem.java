package com.cookandroid.app.model;

// FoodListProvider에서 Food객체 생성할 때 사용 기본 음식 별 정보 매핑하는 용도
public class FoodItem {
    private String name;
    private int imageResId;
    private String category;
    private String imageName;
    private int expirationDate;

    public FoodItem(String name, int imageResId, String category, String imageName, int expirationDate) {
        this.name = name;
        this.imageResId = imageResId;
        this.category = category;
        this.imageName = imageName;
        this.expirationDate = expirationDate;
    }
    public String getCategory() { return category; }
    public String getImageName() { return imageName; }
    public String getName() { return name; }

    public int getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(int expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setName(String name) {
        this.name = name;
    }
}