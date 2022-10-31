package com.project.pigbook.entity;

public class GraphItem {

    private String name;        // 영양성분
    private int percent;        // 퍼센트 (소수점 1자리에서 *10) 45.9 => 459

    public GraphItem(String name, int percent) {
        this.name = name;
        this.percent = percent;
    }

    public String getName() {
        return name;
    }

    public int getPercent() {
        return percent;
    }
}
