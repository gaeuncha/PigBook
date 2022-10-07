package com.project.pigbook.entity;

public class CalendarDay {

    public String day;
    public int week;

    public long income;                         // 수입
    public long expenditure;                    // 지출

    public boolean today;

    public CalendarDay(String day, int week, boolean today) {
        this.day = day;
        this.week = week;
        this.today = today;

        this.income = 0;
        this.expenditure = 0;
    }
}
