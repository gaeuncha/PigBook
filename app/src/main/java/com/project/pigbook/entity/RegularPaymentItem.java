package com.project.pigbook.entity;

public class RegularPaymentItem {

    public String id;                       // RegularPayment Doc ID
    public RegularPayment regularPayment;   // 정기결제 객체

    public RegularPaymentItem(String id, RegularPayment regularPayment) {
        this.id = id;
        this.regularPayment = regularPayment;
    }
}
