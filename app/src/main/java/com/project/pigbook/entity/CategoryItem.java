package com.project.pigbook.entity;

public class CategoryItem {

    public String id;                   // Category Doc ID
    public Category category;           // 분류 객체

    public CategoryItem(String id, Category category) {
        this.id = id;
        this.category = category;
    }
}
