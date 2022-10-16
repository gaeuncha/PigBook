package com.project.pigbook.entity;

public class AccountBookItem {

    public String id;                   // AccountBook Doc ID
    public AccountBook accountBook;     // 가계부 객체

    public AccountBookItem(String id, AccountBook accountBook) {
        this.id = id;
        this.accountBook = accountBook;
    }
}
