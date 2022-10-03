package com.project.pigbook.entity;

public class Category {

    private int kind;                               // 수입 / 지출
    private String name;                            // 카테고리명

    // 파이어 스토어를 사용하기 위해 필요한 생성자
    public Category() {}

    public Category(int kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    public int getKind() {
        return this.kind;
    }

    public String getName() {
        return this.name;
    }

    public void setKind(int kind) {
        this.kind = kind;
    }

    public void setName(String name) {
        this.name = name;
    }
}
