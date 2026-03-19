package com.cookandroid.app.model;

import java.util.List;

// 응답용 일주일치 식단 데이터
public class MealPlanResponse {
    public List<WeekDayPlan> weekPlan;
    // WeekDayPlan: day(요일명), meals(List<MealItem>)
    public List<WeekDayPlan> getWeek() {
        return weekPlan;
    }
    public void setWeek(List<WeekDayPlan> weekPlan) {
        this.weekPlan = weekPlan;
    }
}
