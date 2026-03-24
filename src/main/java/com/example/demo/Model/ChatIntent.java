// ChatIntent.java - add a type field
package com.example.demo.Model;

import java.util.List;

public class ChatIntent {
    private String food;
    private List<String> timeRefs;
    private int limit = 3;
    private String type;
    private String startDate;  // yyyy-MM-dd
    private String endDate;    // yyyy-MM-dd

    public String getFood() { return food; }
    public void setFood(String food) { this.food = food; }
    public List<String> getTimeRefs() { return timeRefs; }
    public void setTimeRefs(List<String> timeRefs) { this.timeRefs = timeRefs; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    private String compStartDate;
    private String compEndDate;

    public String getCompStartDate() { return compStartDate; }
    public void setCompStartDate(String compStartDate) { this.compStartDate = compStartDate; }
    public String getCompEndDate() { return compEndDate; }
    public void setCompEndDate(String compEndDate) { this.compEndDate = compEndDate; }
    private String mealType;  // breakfast, lunch, dinner, snack

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    private String timeOfDay;  // morning, afternoon, evening, night, all

    public String getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
}