package com.project.pigbook;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.pigbook.entity.Category;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class AccountBookSearchActivity extends AppCompatActivity {
    //private static final String TAG = AccountBookSearchActivity.class.getSimpleName();
    private static final String TAG = "PigBook";

    private TextView txtDate1, txtDate2;
    private Spinner spCategory;
    private EditText editMoney1, editMoney2;

    private InputMethodManager imm;             // 키보드를 숨기기 위해 필요함

    private int kind;                           // 지출 / 수입

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_book_search);

        setTitle(R.string.title_account_book_search);

        // 홈버튼(<-) 표시
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // 키보드를 숨기기 위해 필요함
        this.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        this.txtDate1 = findViewById(R.id.txtDate1);
        this.txtDate2 = findViewById(R.id.txtDate2);

        this.txtDate1.setText("기간");
        this.txtDate2.setText("기간");

        this.editMoney1 = findViewById(R.id.editMoney1);
        this.editMoney2 = findViewById(R.id.editMoney2);

        this.editMoney1.setHint("금액");
        this.editMoney2.setHint("금액");

        this.spCategory = findViewById(R.id.spCategory);

        // 종류 기본값
        this.kind = Constants.AccountBookKind.EXPENDITURE;
        ((RadioButton) findViewById(R.id.rdExpenditure)).setChecked(true);
        ((RadioGroup) findViewById(R.id.rdgKind)).setOnCheckedChangeListener((group, checkedId) -> {
            // 선택
            switch (checkedId) {
                case R.id.rdExpenditure:
                    // 지출
                    this.kind = Constants.AccountBookKind.EXPENDITURE;
                    break;
                case R.id.rdIncome:
                    // 수입
                    this.kind = Constants.AccountBookKind.INCOME;
                    break;
            }

            // 분류 구성하기
            createCategory(this.kind);
        });

        findViewById(R.id.btnDate1).setOnClickListener(view -> {
            // 기간1
            showDatePicker(0);
        });

        findViewById(R.id.btnDate2).setOnClickListener(view -> {
            // 기간2
            showDatePicker(1);
        });

        findViewById(R.id.btnOk).setOnClickListener(view -> {
            // 검색
            search();
        });

        // 분류 구성하기
        createCategory(this.kind);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* 분류 구성하기 */
    private void createCategory(int kind) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 분류 목록 (분류명으로 정렬)
        Query query = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.CATEGORY)
                .whereEqualTo("kind", kind)
                .orderBy("name");

        query.get().addOnCompleteListener(task -> {
            ArrayList<String> items = new ArrayList<>();
            items.add("분류");

            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Category category = document.toObject(Category.class);
                        // 분류 추가
                        items.add(category.getName());
                    }
                }
            }

            // 분류 Adapter 구성
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, items);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

            this.spCategory.setAdapter(adapter);
        });
    }

    /* DatePickerDialog 호출 */
    private void showDatePicker(final int kind) {
        Calendar calendar;
        String date;

        if (kind == 0) {
            // 기간1
            date = this.txtDate1.getText().toString();
        } else {
            // 기간2
            date = this.txtDate2.getText().toString();
        }

        if (Utils.isDate("yyyy-MM-dd", date)) {
            calendar = Utils.getCalendar("yyyy-MM-dd", date);
        } else {
            calendar = Calendar.getInstance();
        }

        DatePickerDialog dialog = new DatePickerDialog(this, (datePicker, year, monthOfYear, dayOfMonth) -> {
            // 월(monthOfYear)은 +1을 해야됨
            String d = year + "-" + String.format(Locale.getDefault(), "%02d", (monthOfYear + 1)) + "-" +
                    String.format(Locale.getDefault(), "%02d", dayOfMonth);

            if (kind == 0) {
                this.txtDate1.setText(d);
                this.txtDate1.setTextColor(Color.BLACK);
            } else {
                this.txtDate2.setText(d);
                this.txtDate2.setTextColor(Color.BLACK);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));

        dialog.show();
    }

    /* 검색 */
    private void search() {
        // 가계부 내역
        Intent intent = new Intent(this, AccountBookListActivity.class);
        intent.putExtra("search_kind", Constants.AccountBookSearchKind.DETAIL);

        intent.putExtra("kind", this.kind);         // 지출/수입

        // 분류 선택 체크
        if (this.spCategory.getSelectedItemPosition() > 0) {
            intent.putExtra("search_category", this.spCategory.getSelectedItem().toString());
        }

        // 금액 입력 체크 (시작)
        String money1 = this.editMoney1.getText().toString();
        if (Utils.isNumeric(money1)) {
            intent.putExtra("search_money1", Long.parseLong(money1));
        }

        // 금액 입력 체크 (끝)
        String money2 = this.editMoney2.getText().toString();
        if (Utils.isNumeric(money2)) {
            intent.putExtra("search_money2", Long.parseLong(money2));
        }

        // 기간 입력 체크 (시작)
        String date1 = this.txtDate1.getText().toString();
        if (Utils.isDate("yyyy-MM-dd", date1)) {
            intent.putExtra("search_date1", date1);
        }

        // 기간 입력 체크 (끝)
        String date2 = this.txtDate2.getText().toString();
        if (Utils.isDate("yyyy-MM-dd", date2)) {
            intent.putExtra("search_date2", date2);
        }

        // 키보드 숨기기
        this.imm.hideSoftInputFromWindow(this.editMoney2.getWindowToken(), 0);

        startActivity(intent);

        finish();
    }
}