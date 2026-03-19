package com.cookandroid.app.model;

public class Meal {
    private String type;      // 아침, 점심, 저녁 등
    private String name;      // 메뉴명
    private String time;      // 시간
    private String desc;      // 설명
    private String imageUrl;  // 이미지 URL

    public Meal(String type, String name, String time, String desc, String imageUrl) {
        this.type = type;
        this.name = name;
        this.time = time;
        this.desc = desc;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
