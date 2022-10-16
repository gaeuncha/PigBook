package com.project.pigbook.util;

public class Constants {

    /* Fire store Collection 이름 */
    public static class FirestoreCollectionName {
        public static final String USER = "users";              // 사용자(회원)
        public static final String CATEGORY = "categorys";      // 가계부 분류
        public static final String ACCOUNT_BOOK = "accountBooks";   // 가계부
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
