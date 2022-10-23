package com.project.pigbook.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.project.pigbook.CategoryActivity;
import com.project.pigbook.IntroActivity;
import com.project.pigbook.R;
import com.project.pigbook.fragment.abstracts.IFragment;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.Utils;

public class SettingFragment extends Fragment implements IFragment {
    //private static final String TAG = SettingFragment.class.getSimpleName();
    private static final String TAG = "PigBook";

    private Context context;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        // 사용자 이메일
        ((TextView) view.findViewById(R.id.txtEmail)).setText(GlobalVariable.user.getEmail());

        // 로그아웃 button
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            // 로그아웃
            new AlertDialog.Builder(this.context)
                    .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                        // 로그아웃
                        logout();
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_logout)
                    .setMessage(R.string.dialog_msg_logout)
                    .show();
        });

        // 분류(지출) 편집 button
        view.findViewById(R.id.btnCategory1).setOnClickListener(v -> {
            // 분류(지출) 편집
            Intent intent = new Intent(this.context, CategoryActivity.class);
            intent.putExtra("kind", Constants.AccountBookKind.EXPENDITURE);
            startActivity(intent);
        });

        // 분류(수입) 편집 button
        view.findViewById(R.id.btnCategory2).setOnClickListener(v -> {
            // 분류(수입) 편집
            Intent intent = new Intent(this.context, CategoryActivity.class);
            intent.putExtra("kind", Constants.AccountBookKind.INCOME);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDetach() {
        this.context = null;
        super.onDetach();
    }

    @Override
    public boolean isExecuted() {
        return false;
    }

    /* 로그아웃 */
    private void logout() {
        // 구글 연동 logout
        // 파이어베이스 인증 sign out
        FirebaseAuth.getInstance().signOut();

        // 구글 api 클라이언트
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this.context,
                Utils.getGoogleSignInOptions(getString(R.string.default_web_client_id)));
        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener((Activity) this.context, task -> {

            // 인트로 화면으로 이동
            Intent intent = new Intent(this.context, IntroActivity.class);
            startActivity(intent);

            // 메인화면 닫기
            ((Activity) this.context).finish();
        });
    }
}
