package com.project.pigbook;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.pigbook.entity.Category;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class AccountBookAddActivity extends AppCompatActivity {
    //private static final String TAG = AccountBookAddActivity.class.getSimpleName();
    private static final String TAG = "PigBook";

    private ProgressDialog progressDialog;      // 로딩 dialog

    private TextView txtDate, txtTime, txtWeek;
    private Spinner spAssetsKind, spCategory;
    private EditText editMoney, editMemo;

    private Calendar calendar;

    private InputMethodManager imm;             // 키보드를 숨기기 위해 필요함

    private int kind;                           // 지출 / 수입

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_book_add);

        setTitle(R.string.title_account_book_add);

        // 홈버튼(<-) 표시
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // 로딩 dialog
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setMessage("처리중...");
        this.progressDialog.setCancelable(false);

        // 키보드를 숨기기 위해 필요함
        this.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        this.txtDate = findViewById(R.id.txtDate);
        this.txtTime = findViewById(R.id.txtTime);
        this.txtWeek = findViewById(R.id.txtWeek);

        this.editMoney = findViewById(R.id.editMoney);
        this.editMemo = findViewById(R.id.editMemo);

        this.editMoney.setHint("금액");
        this.editMemo.setHint("간단한 내용을 입력하세요.");

        this.spAssetsKind = findViewById(R.id.spAssetsKind);
        this.spCategory = findViewById(R.id.spCategory);

        // 현금/카드 구성
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item,
                getResources().getStringArray(R.array.assets_kind_list));
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        this.spAssetsKind.setAdapter(adapter);

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

        findViewById(R.id.txtDate).setOnClickListener(view -> {
            // 날자 선택
            showDatePicker();
        });

        findViewById(R.id.txtTime).setOnClickListener(view -> {
            // 시간 클릭
            showTimePicker();
        });

        findViewById(R.id.btnOk).setOnClickListener(view -> {
            // 저장

        });

        // 초기화
        init();
    }

    @Override
    public void onBackPressed() {
        // 처리중이면 닫기 취소
        if (this.progressDialog.isShowing()) {
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* 초기화 */
    private void init() {
        this.calendar = Calendar.getInstance();

        this.txtDate.setText(Utils.getDate("yyyy-MM-dd", this.calendar.getTimeInMillis()));     // 현재일 표시
        this.txtTime.setText(Utils.getDate("HH:mm", this.calendar.getTimeInMillis()));          // 현재시간 표시
        this.txtWeek.setText(Utils.getDate("EE", this.calendar.getTimeInMillis()));             // 요일 표시

        // 분류 구성하기
        createCategory(this.kind);
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
    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(this, (DatePickerDialog.OnDateSetListener) (datePicker, year, monthOfYear, dayOfMonth) -> {
            this.calendar.set(Calendar.YEAR, year);
            this.calendar.set(Calendar.MONTH, monthOfYear);
            this.calendar.set(Calendar.DATE, dayOfMonth);

            this.txtDate.setText(Utils.getDate("yyyy-MM-dd", this.calendar.getTimeInMillis()));
            this.txtWeek.setText(Utils.getDate("EE", this.calendar.getTimeInMillis()));
        }, this.calendar.get(Calendar.YEAR), this.calendar.get(Calendar.MONTH), this.calendar.get(Calendar.DATE));

        dialog.show();
    }

    /* TimePickerDialog 호출 */
    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(this, (timePicker, hourOfDay, minute) -> {
            this.calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            this.calendar.set(Calendar.MINUTE, minute);

            // 시간 표시
            this.txtTime.setText(Utils.getDate("HH:mm", this.calendar.getTimeInMillis()));
        }, this.calendar.get(Calendar.HOUR_OF_DAY), this.calendar.get(Calendar.MINUTE), false);

        dialog.show();
    }

}