package com.project.pigbook;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.pigbook.entity.Category;
import com.project.pigbook.entity.RegularPayment;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class RegularPaymentAddActivity extends AppCompatActivity {
    //private static final String TAG = RegularPaymentAddActivity.class.getSimpleName();
    private static final String TAG = "PigBook";

    private ProgressDialog progressDialog;      // 로딩 dialog

    private TextView txtStartDate;
    private Spinner spAssetsKind, spCategory;
    private EditText editName, editMoney;

    private InputMethodManager imm;             // 키보드를 숨기기 위해 필요함

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regular_payment_add);

        setTitle(R.string.title_regular_payment_add);

        // 홈버튼(<-) 표시
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // 로딩 dialog
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setMessage("처리중...");
        this.progressDialog.setCancelable(false);

        // 키보드를 숨기기 위해 필요함
        this.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        this.txtStartDate = findViewById(R.id.txtStartDate);
        this.txtStartDate.setText("정기결제 시작일");

        this.editName = findViewById(R.id.editName);
        this.editMoney = findViewById(R.id.editMoney);

        this.editName.setHint("정기결제 이름");
        this.editMoney.setHint("정기결제 금액");

        this.spAssetsKind = findViewById(R.id.spAssetsKind);
        this.spCategory = findViewById(R.id.spCategory);

        // 현금/카드 구성
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item,
                getResources().getStringArray(R.array.assets_kind_list));
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        this.spAssetsKind.setAdapter(adapter);

        findViewById(R.id.btnDate).setOnClickListener(view -> {
            // 날자 선택
            showDatePicker();
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

        // (지출)분류 구성하기
        createCategory();
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

    /* (지출)분류 구성하기 */
    private void createCategory() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // (지출)분류 목록 (분류명으로 정렬)
        Query query = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.CATEGORY)
                .whereEqualTo("kind", Constants.AccountBookKind.EXPENDITURE)
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
        Calendar calendar;
        String date = this.txtStartDate.getText().toString();

        if (Utils.isDate("yyyy-MM-dd", date)) {
            calendar = Utils.getCalendar("yyyy-MM-dd", date);
        } else {
            calendar = Calendar.getInstance();
        }

        DatePickerDialog dialog = new DatePickerDialog(this, (datePicker, year, monthOfYear, dayOfMonth) -> {
            // 월(monthOfYear)은 +1을 해야됨
            String d = year + "-" + String.format(Locale.getDefault(), "%02d", (monthOfYear + 1)) + "-" +
                    String.format(Locale.getDefault(), "%02d", dayOfMonth);

            this.txtStartDate.setText(d);
            this.txtStartDate.setTextColor(Color.BLACK);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));

        dialog.show();
    }

    /* 입력 데이터 체크 */
    private boolean checkData() {
        // 분류 선택 체크
        if (this.spCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, R.string.msg_category_select_empty, Toast.LENGTH_SHORT).show();
            return false;
        }

        // 정기결제 시작일 체크
        String startDate = this.txtStartDate.getText().toString();
        if (!Utils.isDate("yyyy-MM-dd", startDate)) {
            Toast.makeText(this, R.string.msg_regular_payment_start_date_check_empty, Toast.LENGTH_SHORT).show();
            return false;
        }

        // 정기결제 이름 체크
        String name = this.editName.getText().toString();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, R.string.msg_regular_payment_name_check_empty, Toast.LENGTH_SHORT).show();
            this.editName.requestFocus();
            return false;
        }

        // 금액 입력 체크
        String money = this.editMoney.getText().toString();
        if (!Utils.isNumeric(money)) {
            Toast.makeText(this, R.string.msg_money_check_empty, Toast.LENGTH_SHORT).show();
            this.editMoney.requestFocus();
            return false;
        }

        // 키보드 숨기기
        this.imm.hideSoftInputFromWindow(this.editMoney.getWindowToken(), 0);

        return true;
    }

    /* 저장 */
    private void save() {
        int assetsKind = this.spAssetsKind.getSelectedItemPosition();
        String category = this.spCategory.getSelectedItem().toString();
        String startDate = this.txtStartDate.getText().toString();
        String name = this.editName.getText().toString();
        String money = this.editMoney.getText().toString();

        // 정기결제 정보
        final RegularPayment regularPayment = new RegularPayment(assetsKind, category, name,
                Long.parseLong(money), startDate);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 정기결제 등록
        db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.REGULAR_PAYMENT)
                .add(regularPayment)
                .addOnSuccessListener(documentReference -> {
                    // 성공
                    this.progressDialog.dismiss();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // 등록 실패
                    this.progressDialog.dismiss();
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                });
    }
}
