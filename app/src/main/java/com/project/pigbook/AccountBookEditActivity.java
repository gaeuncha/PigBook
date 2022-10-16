package com.project.pigbook;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
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
import com.project.pigbook.entity.AccountBook;
import com.project.pigbook.entity.Category;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class AccountBookEditActivity extends AppCompatActivity {
    //private static final String TAG = AccountBookEditActivity.class.getSimpleName();
    private static final String TAG = "PigBook";

    private ProgressDialog progressDialog;      // 로딩 dialog

    private TextView txtDate, txtTime, txtWeek;
    private Spinner spAssetsKind, spCategory;
    private EditText editMoney, editMemo;

    private Calendar calendar;

    private InputMethodManager imm;             // 키보드를 숨기기 위해 필요함

    private int kind;                           // 지출 / 수입

    private String accountBookDocId;            // 가계부 doc id
    private AccountBook accountBook;            // 가계부 객체

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_book_form);

        // 가계부 doc id, 객체
        Intent intent = getIntent();
        this.accountBookDocId = intent.getStringExtra("account_book_doc_id");
        this.accountBook = intent.getParcelableExtra("account_book");

        setTitle(R.string.title_account_book_edit);

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
            createCategory(this.kind, false);
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
            // 입력 체크 후 저장
            if (checkData()) {
                this.progressDialog.show();

                // 로딩 dialog 를 표시하기 위해 딜레이를 줌
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // 저장
                    save();
                }, Constants.LoadingDelay.SHORT);
            }
        });

        // 가계부 정보
        infoAccountBook();
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

    /* 가계부 정보 */
    private void infoAccountBook() {
        if (this.accountBook == null) {
            return;
        }

        // 지출 / 수입
        this.kind = this.accountBook.getKind();
        if (this.kind == Constants.AccountBookKind.EXPENDITURE) {
            ((RadioButton) findViewById(R.id.rdExpenditure)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rdIncome)).setChecked(true);
        }

        // 등록일시
        this.calendar = Utils.getCalendar("yyyy-MM-dd HH:mm", this.accountBook.getInputDate() +
                " " + this.accountBook.getInputTime());
        this.txtDate.setText(Utils.getDate("yyyy-MM-dd", this.calendar.getTimeInMillis()));
        this.txtTime.setText(Utils.getDate("HH:mm", this.calendar.getTimeInMillis()));
        this.txtWeek.setText(Utils.getDate("EE", this.calendar.getTimeInMillis()));

        // 현금/카드 구성
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item,
                getResources().getStringArray(R.array.assets_kind_list));
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        this.spAssetsKind.setAdapter(adapter);
        this.spAssetsKind.setSelection(this.accountBook.getAssetsKind());

        this.editMoney.setText(String.valueOf(this.accountBook.getMoney()));
        this.editMemo.setText(this.accountBook.getMemo());

        // 분류 구성하기
        createCategory(this.accountBook.getKind(), true);
    }

    /* 분류 구성하기 */
    private void createCategory(int kind, final boolean first) {
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

            int position = 0;
            if (task.isSuccessful()) {
                if (task.getResult() != null) {

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Category category = document.toObject(Category.class);
                        // 분류 추가
                        items.add(category.getName());

                        // 첫 구성이면
                        if (first && position == 0) {
                            if (this.accountBook.getCategory().equals(category.getName())) {
                                position = items.size() - 1;
                            }
                        }
                    }
                }
            }

            // 분류 Adapter 구성
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, items);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

            this.spCategory.setAdapter(adapter);

            // 첫 구성이면
            if (first) {
                this.spCategory.setSelection(position);
            }
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

    /* 입력 데이터 체크 */
    private boolean checkData() {
        // 분류 선택 체크
        if (this.spCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, R.string.msg_category_select_empty, Toast.LENGTH_SHORT).show();
            return false;
        }

        // 금액 입력 체크
        String money = this.editMoney.getText().toString();
        if (!Utils.isNumeric(money)) {
            Toast.makeText(this, R.string.msg_money_check_empty, Toast.LENGTH_SHORT).show();
            this.editMoney.requestFocus();
            return false;
        }

        // 내용 체크
        String memo = this.editMemo.getText().toString();
        if (TextUtils.isEmpty(memo)) {
            Toast.makeText(this, R.string.msg_memo_check_empty, Toast.LENGTH_SHORT).show();
            this.editMemo.requestFocus();
            return false;
        }

        // 키보드 숨기기
        this.imm.hideSoftInputFromWindow(this.editMemo.getWindowToken(), 0);

        return true;
    }

    /* 저장 */
    private void save() {
        int assetsKind = this.spAssetsKind.getSelectedItemPosition();
        String category = this.spCategory.getSelectedItem().toString();
        String money = this.editMoney.getText().toString();
        String memo = this.editMemo.getText().toString();
        String date = this.txtDate.getText().toString();
        String time = this.txtTime.getText().toString();

        // 가계부 정보
        final AccountBook accountBook0 = new AccountBook(this.kind, assetsKind, category,
                memo, Long.parseLong(money), date, time);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 가계부 정보 수정 (set() 을 이용해서 덮어쓰기)
        db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.ACCOUNT_BOOK)
                .document(this.accountBookDocId)
                .set(accountBook0)
                .addOnSuccessListener(aVoid -> {
                    // 성공
                    this.progressDialog.dismiss();

                    // 수정한 내용을 달력에 적용하기 위함
                    setResult(Activity.RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // 실패
                    this.progressDialog.dismiss();
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                });
    }
}