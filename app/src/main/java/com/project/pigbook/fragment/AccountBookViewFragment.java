package com.project.pigbook.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Insets;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.pigbook.R;
import com.project.pigbook.adapter.CalendarAdapter;
import com.project.pigbook.entity.CalendarDay;
import com.project.pigbook.fragment.abstracts.ITaskFragment;
import com.project.pigbook.listener.OnItemClickListener;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.MarginDecoration;

import java.util.ArrayList;
import java.util.Calendar;

public class AccountBookViewFragment extends Fragment implements ITaskFragment {
    //private static final String TAG = AccountBookViewFragment.class.getSimpleName();
    private static final String TAG = "PigBook";

    private Context context;

    private ProgressDialog progressDialog;          // 로딩 dialog

    private RecyclerView recyclerView;
    private CalendarAdapter adapter;
    private ArrayList<CalendarDay> items;

    // 주 표시
    private LinearLayout[] layWeeks;
    private TextView[] txtTitles, txtValues;

    private Calendar selectedCalendar;              // 선택 Calendar

    // Calendar 객체의 현재 년/월/일 (월은 +1 해야 정확한 월이 구해짐) : 오늘을 표시하기 위함
    private int currentYear, currentMonth, currentDay;

    // 달력 일 레이아웃 사이즈
    private int layoutWidth;
    private int layoutHeight;

    private float displayDensity;                   // db 사이즈 구할 때 사용됨

    private static final int GRID_ROW = 6;
    private static final int GRID_COL = 7;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_book_view, container, false);

        // 로딩 dialog
        this.progressDialog = new ProgressDialog(this.context);
        this.progressDialog.setMessage("처리중...");
        this.progressDialog.setCancelable(false);

        // 리사이클러뷰 설정
        this.recyclerView = view.findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager lm = new GridLayoutManager(this.context, GRID_COL);
        this.recyclerView.setLayoutManager(lm);

        this.recyclerView.addItemDecoration(new MarginDecoration(this.context, 0));
        this.recyclerView.setHasFixedSize(true);

        // 주 LinearLayout
        int[] layRes = { R.id.lay1, R.id.lay2, R.id.lay3, R.id.lay4, R.id.lay5, R.id.lay6 };
        this.layWeeks = new LinearLayout[layRes.length];
        for (int i=0; i<layWeeks.length; i++) {
            this.layWeeks[i] = view.findViewById(layRes[i]);

            final int position = i;
            this.layWeeks[i].setOnClickListener(view1 -> {
                // 주 선택
                Log.d(TAG, "주선택:" + position);

            });
        }

        // 주 TextView
        int[] txtResT = { R.id.txtT1, R.id.txtT2, R.id.txtT3, R.id.txtT4, R.id.txtT5, R.id.txtT6 };
        int[] txtResV = { R.id.txtV1, R.id.txtV2, R.id.txtV3, R.id.txtV4, R.id.txtV5, R.id.txtV6 };
        this.txtTitles = new TextView[txtResT.length];
        this.txtValues = new TextView[txtResV.length];
        for (int i=0; i<txtTitles.length; i++) {
            this.txtTitles[i] = view.findViewById(txtResT[i]);
            this.txtValues[i] = view.findViewById(txtResV[i]);
            this.txtValues[i].setText("");
        }

        int position;

        // Argument 에서 값 얻기
        Bundle bundle = getArguments();
        if (bundle != null) {
            // 페이지 위치값 얻기
            position = bundle.getInt("position", 0);
        } else {
            position = 0;
        }

        view.post(() -> {
            // 가운데 페이지이면 (첫 실행시 한번만 실행됨)
            if (position == 1) {
                // 가운데 페이지이면 (첫 실행시 한번만 실행됨)
                Calendar calendar = Calendar.getInstance();

                // 첫 실행시 현재 년/월/일 구하기
                this.currentYear = calendar.get(Calendar.YEAR);
                this.currentMonth = calendar.get(Calendar.MONTH);
                this.currentDay = calendar.get(Calendar.DAY_OF_MONTH);

                // 달력 일 레이아웃 구하기

                /* 해상도 */
                int displayWidth, displayHeight;
                // API 30 이상이면
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowMetrics windowMetrics = ((Activity) this.context).getWindowManager().getCurrentWindowMetrics();
                    Insets insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());

                    Log.d(TAG, "insets:" + (insets.left + insets.right));

                    displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
                    displayHeight = windowMetrics.getBounds().height() - insets.top - insets.bottom;

                    this.displayDensity = getResources().getDisplayMetrics().density;
                } else {
                    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

                    displayWidth = displayMetrics.widthPixels;
                    displayHeight = displayMetrics.heightPixels;

                    this.displayDensity = displayMetrics.density;
                }

                Log.d(TAG, "w:" + displayWidth);
                Log.d(TAG, "h:" + displayHeight);

                Log.d(TAG, "d:" + this.displayDensity);

                // (width) dp 사이즈 구하기 (주간항목이 32dp 잡혀 있어서 (해상도 - 32) 의 사이즈를 구해야됨
                int space1 = Math.round(32 * this.displayDensity);
                // (height) dp 사이즈 구하기 (가로라인 5개)
                int space2 = Math.round((GRID_ROW - 1) * this.displayDensity);

                this.layoutWidth = (displayWidth - space1) / GRID_COL;
                this.layoutHeight = (this.recyclerView.getHeight() - space2) / GRID_ROW;

                // 달력 페이지 생성
                createCalendar(calendar);
            }
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
    public void task(int kind, Bundle bundle) {
        if (kind == Constants.FragmentTaskKind.REFRESH) {
            long timeMillis = bundle.getLong("time_millis");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeMillis);

            // 달력 페이지 만들기
            createCalendar(calendar);
        }
    }

    /* 달력 만들기 */
    private void createCalendar(Calendar calendar) {
        ArrayList<CalendarDay> days = new ArrayList<>();
        // 6라인
        int max = (GRID_COL * GRID_ROW);

        // 일 초기화
        for (int i=0; i<max; i++) {
            days.add(new CalendarDay("", 0, false));
        }

        // 해당월 1일의 요일 구하기 위한 Calendar 객체
        Calendar c = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        // 해당 년/월의 1일로 설정
        c.set(year, month, 1);
        // 요일 (1(일요일) ... 7(토요일)
        int week = c.get(Calendar.DAY_OF_WEEK);

        // 월 최대일
        int dayMax = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int w = week;
        int day = 1;
        for (int i=(week - 1); i<max; i++) {
            if (day <= dayMax) {
                days.get(i).day = String.valueOf(day);
            }

            // 요일 (마지막 세로라인 표시하지 않기 위해 날자 없는 영역도 요일 표시)
            if (w % 7 == 0) {
                days.get(i).week = w;
                w = 0;
            } else {
                days.get(i).week = w;
            }

            // 오늘인지 체크
            if (year == this.currentYear && month == this.currentMonth && day == this.currentDay) {
                days.get(i).today = true;
            }

            w++;
            day++;
        }

        // 월 마지막 주
        int weekMax = calendar.getActualMaximum(Calendar.WEEK_OF_MONTH);
        for (int i=0; i<GRID_ROW; i++) {
            if ((i+1) > weekMax) {
                // 없음
                this.layWeeks[i].setBackgroundResource(0);
                this.txtTitles[i].setVisibility(View.GONE);
                this.txtValues[i].setText("");
            } else {
                // 주 표시
                this.layWeeks[i].setBackgroundResource(R.drawable.list_item_selector);
                this.txtTitles[i].setVisibility(View.VISIBLE);
                this.txtValues[i].setText(String.valueOf(i + 1));
            }
        }

        // 선택 Calendar
        this.selectedCalendar = calendar;

        this.items = days;
        this.adapter = new CalendarAdapter(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // 선택

            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        }, this.items, this.layoutWidth, this.layoutHeight, this.displayDensity);
        this.recyclerView.setAdapter(this.adapter);
    }
}
