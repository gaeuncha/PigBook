package com.project.pigbook.entity;

public class User {

    private String uid;                 // 구글 uid (document id 로 사용됨)
    private String email;               // 이메일
    private String name;                // 이름
    private String phoneNumber;         // 휴대번호

    private long joinTimeMillis;        // 가입일시를 millisecond 로 표현

    public User() {}

    public User(String uid, String email, String name, String phoneNumber, long joinTimeMillis) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.joinTimeMillis = joinTimeMillis;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public long getJoinTimeMillis() {
        return joinTimeMillis;
    }
}
