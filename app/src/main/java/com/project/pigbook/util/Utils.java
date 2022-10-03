package com.project.pigbook.util;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class Utils {

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
