package com.cookandroid.app.model;

import java.util.List;

// 요청용 (유저 재고)
public class InventoryRequest {
    private String userID;
    public List<InventoryItem> inventory;
    public InventoryRequest(String userID, List<InventoryItem> inventory) {
        this.inventory = inventory;
        this.userID = userID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }
}

