package com.project.pigbook;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.project.pigbook.fragment.AccountBookFragment;
import com.project.pigbook.fragment.AnalysisFragment;
import com.project.pigbook.fragment.RegularPaymentFragment;
import com.project.pigbook.fragment.SettingFragment;
import com.project.pigbook.fragment.abstracts.IFragment;
import com.project.pigbook.util.Constants;

public class MainActivity extends AppCompatActivity {
    //private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG = "PigBook";

    private BackPressHandler backPressHandler;

    private Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 종료 핸들러
        this.backPressHandler = new BackPressHandler(this);

        // 네비게이션 뷰 (하단에 표시되는 메뉴)
        BottomNavigationView bottomNavigationView = findViewById(R.id.navigationView);
        bottomNavigationView.setOnItemSelectedListener(mItemSelectedListener);

        // 가계부가 디폴트로 표시됨
        setTitle(R.string.menu_account_book);
        // Fragment 메니저를 이용해서 layContent 레이아웃에 Fragment 넣기
        this.fragment = new AccountBookFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.layContent, this.fragment).commit();
    }

    @Override
    public void onBackPressed() {
        this.backPressHandler.onBackPressed();
    }

    @SuppressLint("NonConstantResourceId")
    private final NavigationBarView.OnItemSelectedListener mItemSelectedListener = item -> {
        // 실행중인지 체크
        if (((IFragment) fragment).isExecuted()) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.menu_button_account_book:
                // 가계부
                setTitle(R.string.menu_account_book);
                this.fragment = new AccountBookFragment();
                break;
            case R.id.menu_button_regular_payment:
                // 정기결제
                setTitle(R.string.menu_regular_payment);
                this.fragment = new RegularPaymentFragment();
                break;
            case R.id.menu_button_analysis:
                // 분석
                setTitle(R.string.menu_analysis);
                this.fragment = new AnalysisFragment();
                break;
            case R.id.menu_button_setting:
                // 설정
                setTitle(R.string.menu_setting);
                this.fragment = new SettingFragment();
                break;
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.layContent, this.fragment).commit();

        return true;
    };

    /* Back Press Class */
    private class BackPressHandler {
        private final Context context;
        private Toast toast;

        private long backPressedTime = 0;

        public BackPressHandler(Context context) {
            this.context = context;
        }

        public void onBackPressed() {
            if (System.currentTimeMillis() > this.backPressedTime + (Constants.LoadingDelay.LONG * 2)) {
                this.backPressedTime = System.currentTimeMillis();

                this.toast = Toast.makeText(this.context, R.string.msg_back_press_end, Toast.LENGTH_SHORT);
                this.toast.show();
                return;
            }

            if (System.currentTimeMillis() <= this.backPressedTime + (Constants.LoadingDelay.LONG * 2)) {
                // 종료
                moveTaskToBack(true);
                finish();
                this.toast.cancel();
            }
        }
    }
}