package com.project.pigbook.util;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {

    /* 숫자 콤마 표시 */
    public static String formatComma(long value) {
        DecimalFormat format = new DecimalFormat("#,###");
        return format.format(value);
    }

    /* 날자 구하기 */
    public static String getDate(String format, long timeMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        Date date = new Date(timeMillis);

        return dateFormat.format(date);
    }

    /* GoogleSignInOptions 얻기 */
    public static GoogleSignInOptions getGoogleSignInOptions(String tokenId) {
        // Google 로그인을 앱에 통합
        // GoogleSignInOptions 개체를 구성할 때 requestIdToken 을 호출
        return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(tokenId)
                .requestEmail()
                .build();
    }
}
