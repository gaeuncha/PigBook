package com.project.pigbook.data;

import java.util.ArrayList;

public class MessageDetectionData {
    private volatile static MessageDetectionData _instance = null;

    private ArrayList<String> items;

    /* 싱글톤 패턴 적용 */
    public static MessageDetectionData getInstance() {
        if (_instance == null) {
            synchronized (MessageDetectionData.class) {
                if (_instance == null) {
                    _instance = new MessageDetectionData();
                }
            }
        }

        return _instance;
    }

    private MessageDetectionData() {
        // 초기화 (데이터 생성)
        init();
    }

    /* 초기화 (데이터 생성) */
    private void init() {
        this.items = new ArrayList<>();

        // 우리카드 문자메시지 인식 형식
        this.items.add("#우리(5191)승인\n@none\n@money\n@date\n@none\n@memo");
    }

    public ArrayList<String> getItems() {
        return items;
    }
}
