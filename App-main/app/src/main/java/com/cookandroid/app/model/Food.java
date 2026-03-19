package com.cookandroid.app.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

// DB에 저장하고 ValueFragment에서 DB 정보를 가지고 재고 목록에 생성할 재고 객체 생성하기 위한 용도
public class Food {
    private String name;
    private String info;
    private int quantity;
    private String imageName;
    private String expirationDate; // 유통기한
    private String documentId; // Firestore 문서 ID
    private String category; // 카테고리 추가
    private String storage; // ✅ 보관 방식 추가 ("냉장", "냉동", "실온")
    private String addedDate;

    public Food() {} // Firestore 자동 매핑용 기본 생성자

    public Food(String name, String info, int quantity, String category, String addedDate, String expirationDate, String imageName, String storage) {
        this.name = name;
        this.info = info;
        this.quantity = quantity;
        this.category = category; // 추가
        this.imageName = imageName;
        this.addedDate = addedDate; // 추가
        this.expirationDate = expirationDate;
        this.storage = storage; // 추가
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(String addedDate) {
        this.addedDate = addedDate;
    }

    // 카테고리 필드 getter/setter 추가
    public String getCategory() {
        return category;
    }

    public void setCategory(String category){ this.category = category;}

    public String getName() {
        return name;
    }

    public String getInfo() {
        return info;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getImageName() {return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    // ✅ 유통기한 기준 남은 날짜 계산
    public String getDaysLeft() {
        if (expirationDate == null || expirationDate.isEmpty() || expirationDate.equals("날짜 없음")) {
            return "-";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        try {
            Date expiration = sdf.parse(expirationDate);

            // 오늘 날짜 (0시 0분 0초 0밀리초 세팅)
            Calendar todayCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);

            // 만료 날짜 (0시 0분 0초 0밀리초 세팅)
            Calendar expCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
            expCal.setTime(expiration);
            expCal.set(Calendar.HOUR_OF_DAY, 0);
            expCal.set(Calendar.MINUTE, 0);
            expCal.set(Calendar.SECOND, 0);
            expCal.set(Calendar.MILLISECOND, 0);

            long diffMillis = expCal.getTimeInMillis() - todayCal.getTimeInMillis();
            long daysLeft = diffMillis / (24 * 60 * 60 * 1000);

            if (daysLeft > 0) return "D-" + daysLeft;
            else if (daysLeft == 0) return "D-DAY";
            else return "D+" + Math.abs(daysLeft);

        } catch (ParseException e) {
            e.printStackTrace();
            return "-";
        }
    }

}