package com.project.pigbook.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.pigbook.AccountBookAddActivity;
import com.project.pigbook.AccountBookListActivity;
import com.project.pigbook.R;
import com.project.pigbook.adapter.CalendarAdapter;
import com.project.pigbook.entity.AccountBook;
import com.project.pigbook.entity.CalendarDay;
import com.project.pigbook.fragment.abstracts.ITaskFragment;
import com.project.pigbook.listener.OnItemClickListener;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.MarginDecoration;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AccountBookViewFragment extends Fragment implements ITaskFragment {
    //private static final String TAG = AccountBookViewFragment.class.getSimpleName();
    private static final String TAG = "PigBook";

    private Context context;

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
                selectWeek(position);
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

                // 일 클릭
                String day = items.get(position).day;

                if (TextUtils.isEmpty(day)) {
                    return;
                }

                // 선택일자
                String date = DateFormat.format("yyyy-MM", selectedCalendar) + "-" +
                        String.format(Locale.getDefault(), "%02d", Integer.parseInt(day));
                Log.d(TAG, "date:" + date);

                // 가계부 내역
                Intent intent = new Intent(context, AccountBookListActivity.class);
                intent.putExtra("search_kind", Constants.AccountBookSearchKind.DAY);
                intent.putExtra("search_date", date);
                activityLauncher.launch(intent);
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        }, this.items, this.layoutWidth, this.layoutHeight, this.displayDensity);
        this.recyclerView.setAdapter(this.adapter);

        // 수입/지출 표시 (리스트 구성된 다음에 변경사항 적용됨)
        setData(days, DateFormat.format("yyyy-MM", calendar).toString());
    }

    /* 수입/지출 표시 */
    private void setData(final ArrayList<CalendarDay> days, String month) {
        // 기간
        String date1 = month + "-01";
        String date2 = month + "-31";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 한달동안 가계부 내역 얻기
        Query query = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.ACCOUNT_BOOK)
                .whereGreaterThanOrEqualTo("inputDate", date1)
                .whereLessThanOrEqualTo("inputDate", date2)
                .orderBy("inputDate");

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    long totalIncome = 0;
                    long totalExpenditure = 0;

                    // 달력의 시작 위치
                    int pos = 0;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        // 가계부 내역
                        AccountBook accountBook = document.toObject(AccountBook.class);

                        for (int i = pos; i < days.size(); i++) {
                            if (!TextUtils.isEmpty(days.get(i).day)) {
                                // 일자 값 구하기
                                String day = accountBook.getInputDate().substring(8, 10);
                                if (day.startsWith("0")) {
                                    day = day.substring(1, 2);
                                }

                                // 달력의 해당 날자이면
                                if (day.equals(days.get(i).day)) {
                                    switch (accountBook.getKind()) {
                                        case Constants.AccountBookKind.INCOME:
                                            // 수입
                                            days.get(i).income += accountBook.getMoney();
                                            totalIncome += accountBook.getMoney();
                                            break;
                                        case Constants.AccountBookKind.EXPENDITURE:
                                            // 지출
                                            days.get(i).expenditure += accountBook.getMoney();
                                            totalExpenditure += accountBook.getMoney();
                                            break;
                                    }
                                    pos = i;
                                    break;
                                }
                            }
                        }
                    }

                    // 리스트에 적용
                    this.adapter.notifyDataSetChanged();

                    // 총 금액 표시
                    ((AccountBookFragment) getParentFragment()).displayTotalMoney(totalIncome, totalExpenditure);
                }
            } else {
                // 오류
                Log.d(TAG, "error:" + task.getException().toString());
                Toast.makeText(this.context, R.string.msg_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* 주 선택 */
    private void selectWeek(int position) {
        String week = this.txtValues[position].getText().toString();
        if (!Utils.isNumeric(week)) {
            return;
        }

        int w = Integer.parseInt(week);

        // 월 마지막 주
        int weekMax = this.selectedCalendar.getActualMaximum(Calendar.WEEK_OF_MONTH);

        String month = DateFormat.format("yyyy-MM", this.selectedCalendar).toString();
        String date1, date2;
        if (w == 1) {
            // 1째주
            date1 = month + "-01";
            date2 = month + "-" + String.format(Locale.getDefault(), "%02d", Integer.parseInt(this.items.get(((position + 1) * GRID_COL) - 1).day));
        } else if (w == weekMax) {
            // 마지막주
            date1 = month + "-" + String.format(Locale.getDefault(), "%02d", Integer.parseInt(this.items.get(position * GRID_COL).day));
            // 월 최대일
            int dayMax = this.selectedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            date2 = month + "-" + String.format(Locale.getDefault(), "%02d", dayMax);
        } else {
            date1 = month + "-" + String.format(Locale.getDefault(), "%02d", Integer.parseInt(this.items.get(position * GRID_COL).day));
            date2 = month + "-" + String.format(Locale.getDefault(), "%02d", Integer.parseInt(this.items.get(((position + 1) * GRID_COL) - 1).day));
        }

        // 가계부 내역 (주단위)
        Intent intent = new Intent(this.context, AccountBookListActivity.class);
        intent.putExtra("search_kind", Constants.AccountBookSearchKind.WEEK);
        intent.putExtra("search_week", week);
        intent.putExtra("search_date1", date1);
        intent.putExtra("search_date2", date2);
        this.activityLauncher.launch(intent);
    }

    /* 가계부 등록/수정 ActivityForResult */
    private final ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // 가계부 등록 및 수정 후

                    // 달력 페이지 만들기 (새로고침)
                    createCalendar(this.selectedCalendar);
                }
            });
}
