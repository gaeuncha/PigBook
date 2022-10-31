package com.project.pigbook.entity;

public class CategoryValue {

    private String name;            // 분류명
    private long value;             // 금액

    public CategoryValue(String name, long value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public long getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
