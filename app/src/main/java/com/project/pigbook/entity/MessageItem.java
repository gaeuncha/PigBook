package com.project.pigbook.entity;

public class MessageItem {

    public String dateTime;
    public String number;
    public String message;

    public MessageItem(String dateTime, String number, String message) {
        this.dateTime = dateTime;
        this.number = number;
        this.message = message;
    }
}
