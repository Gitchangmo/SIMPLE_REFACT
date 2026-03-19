package com.cookandroid.app.model;

import java.util.List;

// MealItem(하루치 식단) 형태의 일주일치 식단 리스트 저장 모델
public class WeekDayPlan {
    private String day;
    public List<MealItem> meals;

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }
    public List<MealItem> getMeals() { return meals; }
    public void setMeals(List<MealItem> meals) { this.meals = meals; }
}
