package com.project.pigbook.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.project.pigbook.AccountBookAddActivity;
import com.project.pigbook.AccountBookSearchActivity;
import com.project.pigbook.MessageListActivity;
import com.project.pigbook.R;
import com.project.pigbook.adapter.MyFragmentStateAdapter;
import com.project.pigbook.fragment.abstracts.IFragment;
import com.project.pigbook.fragment.abstracts.ITaskFragment;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;

public class AccountBookFragment extends Fragment implements IFragment {
    //private static final String TAG = AccountBookFragment.class.getSimpleName();
    private static final String TAG = "PigBook";

    private Context context;

    private ArrayList<Fragment> fragments;
    private ViewPager2 viewPager;
    private TextView txtMonth, txtIncome, txtExpenditure, txtBalance;

    private Calendar calendar;
    private int pagePosition = 1;               // 디폴트 포지션

    private static final int PAGE_MIDDLE = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_book, container, false);

        // 프레그먼트에 메뉴 구성
        setHasOptionsMenu(true);

        this.viewPager = view.findViewById(R.id.viewPager);

        // 유지되는 페이지수를 설정
        // (3개의 페이지를 초반에 미리로딩한다. 페이지를 이동할때 마다 View 를 지우고 새로만드는 작업은 하지않게 된다)
        this.viewPager.setOffscreenPageLimit(3);

        // month 3개를 생성 (이전월, 현재월, 다음월)
        this.fragments = new ArrayList<>();
        for (int i=0; i<3; i++) {
            Fragment fragment = new AccountBookViewFragment();
            // 현재 위치값 전달
            Bundle bundle = new Bundle();
            bundle.putInt("position", i);
            fragment.setArguments(bundle);
            this.fragments.add(fragment);
        }

        MyFragmentStateAdapter adapter = new MyFragmentStateAdapter(this, this.fragments);
        this.viewPager.setAdapter(adapter);

        this.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);

                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    // 스크롤이 정지되어 있는 상태
                    if (pagePosition < PAGE_MIDDLE) {
                        // 이전월
                        prevMonth();
                    } else if (pagePosition > PAGE_MIDDLE) {
                        // 다음월
                        nextMonth();
                    } else {
                        return;
                    }

                    // 페이지를 다시 가운데로 맞춘다 (3페이지로 계속 이전 / 다음 할 수 있게 하기위함)
                    viewPager.setCurrentItem(PAGE_MIDDLE, false);

                    // 월 만들기
                    Bundle bundle = new Bundle();
                    bundle.putLong("time_millis", calendar.getTimeInMillis());
                    ((ITaskFragment) fragments.get(PAGE_MIDDLE)).task(Constants.FragmentTaskKind.REFRESH, bundle);
                }
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                pagePosition = position;
            }
        });

        this.txtIncome = view.findViewById(R.id.txtIncome);
        this.txtExpenditure = view.findViewById(R.id.txtExpenditure);
        this.txtBalance = view.findViewById(R.id.txtBalance);

        this.txtIncome.setText("");
        this.txtExpenditure.setText("");
        this.txtBalance.setText("");

        this.txtMonth = view.findViewById(R.id.txtMonth);

        this.calendar = Calendar.getInstance();
        this.txtMonth.setText(DateFormat.format("yyyy.MM", this.calendar));

        view.findViewById(R.id.btnPrev).setOnClickListener(view1 -> {
            // 이전월
            this.viewPager.setCurrentItem(PAGE_MIDDLE - 1, true);
        });

        view.findViewById(R.id.btnNext).setOnClickListener(view1 -> {
            // 다음월
            this.viewPager.setCurrentItem(PAGE_MIDDLE + 1, true);
        });

        view.findViewById(R.id.btnAdd).setOnClickListener(view1 -> {
            // 가계부 추가
            Intent intent = new Intent(this.context, AccountBookAddActivity.class);
            this.activityLauncher.launch(intent);
        });

        this.viewPager.setCurrentItem(PAGE_MIDDLE, false);

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
    public void onResume() {
        super.onResume();
        ((Activity) this.context).invalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.account_book, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_message:
                // 문자 메시지 확인
                intent = new Intent(this.context, MessageListActivity.class);
                this.activityLauncher.launch(intent);
                return true;
            case R.id.menu_search:
                // 검색
                intent = new Intent(this.context, AccountBookSearchActivity.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean isExecuted() {
        return false;
    }

    /* 이전달 */
    private void prevMonth() {
        this.calendar.add(Calendar.MONTH, -1);
        this.txtMonth.setText(DateFormat.format("yyyy.MM", this.calendar));
    }

    /* 다음달 */
    private void nextMonth() {
        this.calendar.add(Calendar.MONTH, 1);
        this.txtMonth.setText(DateFormat.format("yyyy.MM", this.calendar));
    }

    /* 총 금액 표시 (AccountBookViewFragment 에서 호출) */
    public void displayTotalMoney(long income, long expenditure) {
        this.txtIncome.setText(Utils.formatComma(income) + "원");
        this.txtExpenditure.setText(Utils.formatComma(expenditure) + "원");
        this.txtBalance.setText(Utils.formatComma(income - expenditure) + "원");
    }

    /* 가계부 등록 ActivityForResult */
    private final ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // 가계부 등록 후

                    // 달력 만들기 (새로고침)
                    Bundle bundle = new Bundle();
                    bundle.putLong("time_millis", this.calendar.getTimeInMillis());
                    ((ITaskFragment) this.fragments.get(PAGE_MIDDLE)).task(Constants.FragmentTaskKind.REFRESH, bundle);
                }
            });
}
