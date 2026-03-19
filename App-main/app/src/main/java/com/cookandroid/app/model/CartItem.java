package com.cookandroid.app.model;

public class CartItem {
    private String documentId;
    private int quantity;
    private String imageName; // 이미지 리소스 id
    private String name;    // 식품명
    private String category;
    private boolean isChecked = false;
    private int expirationDate;


    public CartItem(String documentId, String imageName, String name, int quantity, String category, boolean isChecked, int expirationDate) {
        this.documentId = documentId;
        this.imageName = imageName;
        this.name = name;
        this.quantity = quantity;
        this.category = category;
        this.isChecked = isChecked;
        this.expirationDate = expirationDate;
    }
//    public CartItem(String documentId, String imageName, String name, int quantity, String category, int expirationDate) {
//        this(documentId, imageName, name, quantity, category, false, expirationDate);
//    }

    public void setExpirationDate(int expirationDate) {
        this.expirationDate = expirationDate;
    }

    public int getExpirationDate() {
        return expirationDate;
    }

    public String getImageName( ){
        return imageName;
    }
    public void setChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isChecked() { return isChecked; }
    public void setDocumentId(String documentId){
        this.documentId = documentId;
    }
    public String getDocumentId(){
        return documentId;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public int getQuantity(){
        return quantity;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
