package com.project.pigbook.entity;

public class AccountBook {

    private int kind;                               // 수입 / 지출
    private int assetsKind;                         // 자산종류 (현금 / 카드)

    private String category;                        // 카테고리
    private String memo;                            // 메모

    private long money;                             // 금액

    private String inputDate;                       // 등록일 (yyyy-MM-dd)
    private String inputTime;                       // 등록시간 (HH:mm)

    // 파이어 스토어를 사용하기 위해 필요한 생성자
    public AccountBook() {}

    public AccountBook(int kind, int assetsKind, String category, String memo,
                       long money, String inputDate, String inputTime) {
        this.kind = kind;
        this.assetsKind = assetsKind;
        this.category = category;
        this.memo = memo;
        this.money = money;
        this.inputDate = inputDate;
        this.inputTime = inputTime;
    }

    public int getKind() {
        return this.kind;
    }

    public int getAssetsKind() {
        return this.assetsKind;
    }

    public String getCategory() {
        return this.category;
    }

    public String getMemo() {
        return this.memo;
    }

    public long getMoney() {
        return this.money;
    }

    public String getInputDate() {
        return this.inputDate;
    }

    public String getInputTime() {
        return this.inputTime;
    }

    public void setKind(int kind) {
        this.kind = kind;
    }

    public void setAssetsKind(int assetsKind) {
        this.assetsKind = assetsKind;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public void setInputDate(String inputDate) {
        this.inputDate = inputDate;
    }

    public void setInputTime(String inputTime) {
        this.inputTime = inputTime;
    }
}
