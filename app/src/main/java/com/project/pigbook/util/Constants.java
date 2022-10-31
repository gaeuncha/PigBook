package com.project.pigbook.util;

public class Constants {

    public static final String REGULAR_PAYMENT_TIME = "09:00";  // 정기결제를 통해 등록될 때 적용할 시간
    public static final int MESSAGE_SEARCH_MONTH = 2;           // 문자 메시지 검색 월

    /* Fire store Collection 이름 */
    public static class FirestoreCollectionName {
        public static final String USER = "users";              // 사용자(회원)
        public static final String CATEGORY = "categorys";      // 가계부 분류
        public static final String ACCOUNT_BOOK = "accountBooks";       // 가계부
        public static final String REGULAR_PAYMENT = "regularPayments"; // 정기결제
    }

    /* 액티비티에서 프레그먼트에 요청할 작업 종류 */
    public static class FragmentTaskKind {
        public static final int REFRESH = 0;                    // 새로고침
    }

    /* 가계부 종류 (지출/수입) */
    public static class AccountBookKind {
        public static final int EXPENDITURE = 0;                // 지출
        public static final int INCOME = 1;                     // 수입
    }

    /* 가계부 자산 종류 (현금/카드) */
    public static class AccountBookAssetsKind {
        public static final int CASH = 0;                       // 현금
        public static final int CARD = 1;                       // 카드
    }

    /* 가계부 검색 종류 */
    public static class AccountBookSearchKind {
        public static final int DAY = 0;                        // 일
        public static final int WEEK = 1;                       // 주
        public static final int DETAIL = 2;                     // 상세검색
    }

    /* 문자 메시지 감지 타입 */
    public static class MessageDetectionType {
        public static final String NONE = "none";               // 감지 안함
        public static final String DATE = "date";               // 일시
        public static final String MONEY = "money";             // 금액
        public static final String MEMO = "memo";               // 내용
    }

    /* 편집 모드 (등록/수정) */
    public static class EditMode {
        public static final int N = 0;                          // 신규 등록
        public static final int U = 1;                          // 수정
    }

    /* 로딩 딜레이 */
    public static class LoadingDelay {
        public static final int SHORT = 500;
        public static final int LONG = 1000;
    }
}
