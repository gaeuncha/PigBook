package com.project.pigbook.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.pigbook.R;
import com.project.pigbook.entity.AccountBook;
import com.project.pigbook.entity.CalendarDay;
import com.project.pigbook.entity.CategoryValue;
import com.project.pigbook.entity.GraphItem;
import com.project.pigbook.fragment.abstracts.ITaskFragment;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

public class AnalysisViewFragment extends Fragment implements ITaskFragment {
    //private static final String TAG = AnalysisViewFragment.class.getSimpleName();
    private static final String TAG = "PigBook";

    private Context context;

    private TextView txtExpenditure, txtIncome, txtBalance;

    private static final int GRAPH_SIZE = 4;

    // 그래프 관련 변수
    private LinearLayout layGraphAe, layGraphAi;
    private View[] viewLegendes, viewLegendis, viewGraphes, viewGraphis;
    private TextView[] txtLegendes, txtLegendis;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analysis_view, container, false);

        this.txtExpenditure = view.findViewById(R.id.txtExpenditure);
        this.txtIncome = view.findViewById(R.id.txtIncome);
        this.txtBalance = view.findViewById(R.id.txtBalance);

        this.layGraphAe = view.findViewById(R.id.layGraphAe);
        this.layGraphAi = view.findViewById(R.id.layGraphAi);

        // 범례 View (지출)
        int[] legendVeRes = { R.id.viewLegend1e, R.id.viewLegend2e, R.id.viewLegend3e, R.id.viewLegend4e };
        this.viewLegendes = new View[legendVeRes.length];
        for (int i=0; i<legendVeRes.length; i++) {
            this.viewLegendes[i] = view.findViewById(legendVeRes[i]);
        }

        // 범례 View (수입)
        int[] legendViRes = { R.id.viewLegend1i, R.id.viewLegend2i, R.id.viewLegend3i, R.id.viewLegend4i };
        this.viewLegendis = new View[legendViRes.length];
        for (int i=0; i<legendViRes.length; i++) {
            this.viewLegendis[i] = view.findViewById(legendViRes[i]);
        }

        // 범례 Text (지출)
        int[] legendTeRes = { R.id.txtLegend1e, R.id.txtLegend2e, R.id.txtLegend3e, R.id.txtLegend4e };
        this.txtLegendes = new TextView[legendTeRes.length];
        for (int i=0; i<legendTeRes.length; i++) {
            this.txtLegendes[i] = view.findViewById(legendTeRes[i]);
        }

        // 범례 Text (수입)
        int[] legendTiRes = { R.id.txtLegend1i, R.id.txtLegend2i, R.id.txtLegend3i, R.id.txtLegend4i };
        this.txtLegendis = new TextView[legendTiRes.length];
        for (int i=0; i<legendTiRes.length; i++) {
            this.txtLegendis[i] = view.findViewById(legendTiRes[i]);
        }

        // 그래프 View (지출)
        int[] graphVeRes = { R.id.viewGraph1e, R.id.viewGraph2e, R.id.viewGraph3e, R.id.viewGraph4e };
        this.viewGraphes = new View[graphVeRes.length];
        for (int i=0; i<graphVeRes.length; i++) {
            this.viewGraphes[i] = view.findViewById(graphVeRes[i]);
        }

        // 그래프 View (수입)
        int[] graphViRes = { R.id.viewGraph1i, R.id.viewGraph2i, R.id.viewGraph3i, R.id.viewGraph4i };
        this.viewGraphis = new View[graphViRes.length];
        for (int i=0; i<graphViRes.length; i++) {
            this.viewGraphis[i] = view.findViewById(graphViRes[i]);
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
                // 월 페이지 생성
                createMonth(System.currentTimeMillis());
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
            // init
            this.layGraphAe.setVisibility(View.INVISIBLE);
            this.layGraphAi.setVisibility(View.INVISIBLE);

            for (View view : this.viewLegendes) {
                view.setVisibility(View.VISIBLE);
            }

            for (View view : this.viewLegendis) {
                view.setVisibility(View.VISIBLE);
            }

            for (TextView textView : this.txtLegendes) {
                textView.setVisibility(View.VISIBLE);
            }

            for (TextView textView : this.txtLegendis) {
                textView.setVisibility(View.VISIBLE);
            }

            for (View view : this.viewGraphes) {
                view.setVisibility(View.VISIBLE);
            }

            for (View view : this.viewGraphis) {
                view.setVisibility(View.VISIBLE);
            }

            // 월 페이지 생성
            createMonth(bundle.getLong("time_millis"));
        }
    }

    /* 월 페이지 생성 */
    private void createMonth(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);

        this.txtExpenditure.setText("");
        this.txtIncome.setText("");
        this.txtBalance.setText("");

        // 가계부 내역
        setData(DateFormat.format("yyyy-MM", calendar).toString());
    }

    /* 가계부 내역 */
    private void setData(String month) {
        // 기간
        String date1 = month + "-01";
        String date2 = month + "-31";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 한달동안 가계부 내역 얻기
        Query query = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.ACCOUNT_BOOK)
                .whereGreaterThanOrEqualTo("inputDate", date1)
                .whereLessThanOrEqualTo("inputDate", date2);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {

                    ArrayList<CategoryValue> expenditures = new ArrayList<>();
                    ArrayList<CategoryValue> incomes = new ArrayList<>();
                    long totalExpenditure = 0;
                    long totalIncome = 0;

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        // 가계부 내역
                        AccountBook accountBook = document.toObject(AccountBook.class);
                        boolean exist = false;
                        if (accountBook.getKind() == Constants.AccountBookKind.EXPENDITURE) {
                            // 지출
                            for (CategoryValue category : expenditures) {
                                // 분류가 존재하면
                                if (category.getName().equals(accountBook.getCategory())) {
                                    // 금액
                                    category.setValue(category.getValue() + accountBook.getMoney());
                                    exist = true;
                                }
                            }

                            Log.d(TAG, "c:" + accountBook.getCategory());

                            // 존재하지 않으면
                            if (!exist) {
                                expenditures.add(new CategoryValue(accountBook.getCategory(), accountBook.getMoney()));
                            }

                            totalExpenditure += accountBook.getMoney();
                        } else {
                            // 수입
                            for (CategoryValue category : incomes) {
                                // 분류가 존재하면
                                if (category.getName().equals(accountBook.getCategory())) {
                                    // 금액
                                    category.setValue(category.getValue() + accountBook.getMoney());
                                    exist = true;
                                }
                            }

                            // 존재하지 않으면
                            if (!exist) {
                                incomes.add(new CategoryValue(accountBook.getCategory(), accountBook.getMoney()));
                            }

                            totalIncome += accountBook.getMoney();
                        }
                    }

                    this.txtExpenditure.setText(Utils.formatComma(totalExpenditure) + "원");
                    this.txtIncome.setText(Utils.formatComma(totalIncome) + "원");
                    this.txtBalance.setText(Utils.formatComma(totalIncome - totalExpenditure) + "원");

                    // 지출 그래프 표시
                    displayGraph(expenditures, totalExpenditure, Constants.AccountBookKind.EXPENDITURE);

                    // 수입 그래프 표시
                    displayGraph(incomes, totalIncome, Constants.AccountBookKind.INCOME);
                }
            } else {
                // 오류
                Log.d(TAG, "error:" + task.getException().toString());
                Toast.makeText(this.context, R.string.msg_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* 그래프 표시 */
    private void displayGraph(ArrayList<CategoryValue> categoryValues, long total, int kind) {
        // 금액이 0인 분류는 삭제
        for (int i=0; i<categoryValues.size(); i++) {
            if (categoryValues.get(i).getValue() == 0) {
                categoryValues.remove(i);
                i--;
            }
        }

        if (categoryValues.size() == 0) {
            return;
        }

        // 그래프영역 VISIBLE
        if (kind == Constants.AccountBookKind.EXPENDITURE) {
            // 지출
            this.layGraphAe.setVisibility(View.VISIBLE);
        } else {
            // 수입
            this.layGraphAi.setVisibility(View.VISIBLE);
        }

        // 금액이 많은순으로 정렬
        Collections.sort(categoryValues, getComparator());

        // 그래프 구성에 사용
        ArrayList<GraphItem> graphItems = new ArrayList<>();
        int percentSum = 0;
        int count = 0;
        for (CategoryValue categoryValue : categoryValues) {
            if (count == 3) {
                String name;
                if (categoryValues.size() > GRAPH_SIZE) {
                    // 기타 항목 만들기
                    name = "...";
                } else {
                    name = categoryValue.getName();
                }
                graphItems.add(new GraphItem(name, (1000 - percentSum)));
                break;
            } else {
                // 소수점 한자리까지 표시 (반올림) * 10
                int percent = (int) Math.round((categoryValue.getValue() * 100.0 / total) * 10);
                percentSum += percent;

                graphItems.add(new GraphItem(categoryValue.getName(), percent));
            }
            count++;
        }

        // 그래프 표시 항목이 4개보다 작을 경우 나머지는 표시안함
        if (graphItems.size() < GRAPH_SIZE) {
            if (kind == Constants.AccountBookKind.EXPENDITURE) {
                // 지출
                for (int i=this.viewLegendes.length-1; i>=graphItems.size(); i--) {
                    this.viewLegendes[i].setVisibility(View.INVISIBLE);
                    this.txtLegendes[i].setVisibility(View.INVISIBLE);
                    this.viewGraphes[i].setVisibility(View.GONE);
                }
            } else {
                // 수입
                for (int i=this.viewLegendis.length-1; i>=graphItems.size(); i--) {
                    this.viewLegendis[i].setVisibility(View.INVISIBLE);
                    this.txtLegendis[i].setVisibility(View.INVISIBLE);
                    this.viewGraphis[i].setVisibility(View.GONE);
                }
            }
        }

        for (int i=0; i<graphItems.size(); i++) {
            final GraphItem item = graphItems.get(i);

            if (kind == Constants.AccountBookKind.EXPENDITURE) {
                // 지출

                // 색상 res
                int[] colorRes = { R.color.expenditure_color_1, R.color.expenditure_color_2, R.color.expenditure_color_3, R.color.expenditure_color_4 };

                // 범례
                String text = item.getName() + "(" + (item.getPercent() / 10.0) + "%)";
                this.txtLegendes[i].setText(text);

                // 그래프 구성
                final int position = i;
                this.viewGraphes[i].post(() -> {
                    // 색상 지정
                    this.viewGraphes[position].setBackgroundResource(colorRes[position]);

                    // 넓이 조절
                    ((LinearLayout.LayoutParams) this.viewGraphes[position].getLayoutParams()).weight = item.getPercent();
                    this.viewGraphes[position].requestLayout();
                });
            } else {
                // 수입

                // 색상 res
                int[] colorRes = { R.color.income_color_1, R.color.income_color_2, R.color.income_color_3, R.color.income_color_4 };

                // 범례
                String text = item.getName() + "(" + (item.getPercent() / 10.0) + "%)";
                this.txtLegendis[i].setText(text);

                // 그래프 구성
                final int position = i;
                this.viewGraphis[i].post(() -> {
                    // 색상 지정
                    this.viewGraphis[position].setBackgroundResource(colorRes[position]);

                    // 넓이 조절
                    ((LinearLayout.LayoutParams) this.viewGraphis[position].getLayoutParams()).weight = item.getPercent();
                    this.viewGraphis[position].requestLayout();
                });
            }
        }
    }

    /* 데이터 정렬을 위한 Comparator (금액 DESC) */
    private Comparator<CategoryValue> getComparator() {
        Comparator<CategoryValue> comparator = (sort1, sort2) -> {
            // 정렬
            return Long.compare(sort2.getValue(), sort1.getValue());
        };

        return comparator;
    }
}
