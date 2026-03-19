package com.cookandroid.app.model;

import com.google.firebase.Timestamp;

public class LogItem {
    private String foodName;
    private String action;
    private int quantity;
    private String time;

    public LogItem() {}  // Firebase용 기본 생성자 필수

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}