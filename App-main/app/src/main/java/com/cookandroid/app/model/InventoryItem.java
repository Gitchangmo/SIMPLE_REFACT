package com.cookandroid.app.model;

// 일주일 식단 추천 시 전송할 데이터 형식 정의하는 모델 클래스
public class InventoryItem {
    private String name;           // 음식 이름
    private String expirationDate; // 남은 유통기한(일)
    private int quantity;          // 수량
    private String category;       // 카테고리

    public InventoryItem(String name, String expirationDate, int quantity, String category) {
        this.name = name;
        this.expirationDate = expirationDate;
        this.quantity = quantity;
        this.category = category;
    }

    public InventoryItem() {}

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getName() {
        return name;
    }
}
