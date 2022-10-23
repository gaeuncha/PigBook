package com.project.pigbook.entity;

public class RegularPayment {

    private int assetsKind;                         // 자산종류 (현금 / 카드)

    private String category;                        // 카테고리
    private String name;                            // 정기결제 이름

    private long money;                             // 금액

    private String startDate;                       // 정기결제 시작일 (yyyy-MM-dd)
    private String applyDate;                       // 가계부에 적용해야되는 일

    // 파이어 스토어를 사용하기 위해 필요한 생성자
    public RegularPayment() {}

    public RegularPayment(int assetsKind, String category, String name,
                       long money, String startDate) {
        this.assetsKind = assetsKind;
        this.category = category;
        this.name = name;
        this.money = money;
        this.startDate = startDate;

        // 등록시 적용일을 시작일로 설정
        this.applyDate = startDate;
    }

    public int getAssetsKind() {
        return assetsKind;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public long getMoney() {
        return money;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getApplyDate() {
        return applyDate;
    }
}
