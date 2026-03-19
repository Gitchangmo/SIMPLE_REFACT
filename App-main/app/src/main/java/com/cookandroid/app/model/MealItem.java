package com.cookandroid.app.model;

import java.util.List;

// 하루치 식단 저장 형태 정의 . UI에 표현할 내용들
public class MealItem {
    private String type;          // "아침", "점심", "저녁"
    private String title;         // 메뉴명
    private String desc;          // 재료 등 설명
    private String imageUrl;      // 레시피 이미지 URL
    private String link;          // 상세 레시피 링크
    private List<String> usedIngredients;

    public MealItem(String type, String title, String desc, String imageUrl, String link, List<String> usedIngredients) {
        this.type = type;
        this.title = title;
        this.desc = desc;
        this.imageUrl = imageUrl;
        this.link = link;
        this.usedIngredients = usedIngredients;
    }
    public MealItem() { }
    public List<String> getUsedIngredients() { return usedIngredients; }
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }
}
