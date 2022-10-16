package com.project.pigbook.entity;

import android.os.Parcel;
import android.os.Parcelable;

/*
Parcelable 타입의 객체만 Intent 을 통해 Activity 간 데이터를 넘길 수 있음
 */
public class AccountBook implements Parcelable {

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

    public AccountBook(Parcel in) {
        readFromParcel(in);
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

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.kind);
        dest.writeInt(this.assetsKind);
        dest.writeString(this.category);
        dest.writeString(this.memo);
        dest.writeLong(this.money);
        dest.writeString(this.inputDate);
        dest.writeString(this.inputTime);
    }

    private void readFromParcel(Parcel in){
        this.kind = in.readInt();
        this.assetsKind = in.readInt();
        this.category = in.readString();
        this.memo = in.readString();
        this.money = in.readLong();
        this.inputDate = in.readString();
        this.inputTime = in.readString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public AccountBook createFromParcel(Parcel in) {
            return new AccountBook(in);
        }

        public AccountBook[] newArray(int size) {
            return new AccountBook[size];
        }
    };
}
