package com.project.pigbook;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.pigbook.adapter.AccountBookAdapter;
import com.project.pigbook.adapter.CategoryAdapter;
import com.project.pigbook.entity.AccountBook;
import com.project.pigbook.entity.AccountBookItem;
import com.project.pigbook.entity.Category;
import com.project.pigbook.entity.CategoryItem;
import com.project.pigbook.fragment.abstracts.ITaskFragment;
import com.project.pigbook.listener.OnItemClickListener;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;
import java.util.Objects;

public class AccountBookListActivity extends AppCompatActivity {
    //private static final String TAG = AccountBookListActivity.class.getSimpleName();
    private static final String TAG = "PigBook";

    private ProgressDialog progressDialog;      // 로딩 dialog

    private RecyclerView recyclerView;
    private AccountBookAdapter adapter;
    private ArrayList<AccountBookItem> items;

    private TextView txtIncome, txtExpenditure, txtBalance, txtNone;

    private long totalIncome, totalExpenditure;

    private int searchKind;                     // 검색종류
    private String searchDate;                  // 검색일
    private String searchDate1, searchDate2;    // 시작일, 마지막일

    private int selectedPosition = -1;          // 분류 리스트 위치

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_book_list);

        // 검색종류 및 조건
        Intent intent = getIntent();
        this.searchKind = intent.getIntExtra("search_kind", Constants.AccountBookSearchKind.DAY);

        TextView txtSearchRequirement = findViewById(R.id.txtSearchRequirement);

        switch (this.searchKind) {
            case Constants.AccountBookSearchKind.DAY:
                // 일 검색
                this.searchDate = intent.getStringExtra("search_date");
                setTitle(this.searchDate);

                // 검색조건 숨기기
                findViewById(R.id.laySearchRequirement).setVisibility(View.GONE);
                break;
            case Constants.AccountBookSearchKind.WEEK:
                // 주 검색
                this.searchDate1 = intent.getStringExtra("search_date1");
                this.searchDate2 = intent.getStringExtra("search_date2");
                String week = intent.getStringExtra("search_week");
                setTitle(this.searchDate1.substring(0, 7).replace("-", ".") + " (" + week + "째주)");

                txtSearchRequirement.setText(this.searchDate1 + " ~ " + this.searchDate2);
                break;
            case Constants.AccountBookSearchKind.DETAIL:
                // 상세 검색

                break;
        }

        // 홈버튼(<-) 표시
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // 로딩 dialog
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setMessage("처리중...");
        this.progressDialog.setCancelable(false);

        // 리사이클러뷰
        this.recyclerView = findViewById(R.id.recyclerView);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        this.txtNone = findViewById(R.id.txtNone);
        this.txtIncome = findViewById(R.id.txtIncome);
        this.txtExpenditure = findViewById(R.id.txtExpenditure);
        this.txtBalance = findViewById(R.id.txtBalance);

        this.txtIncome.setText("");
        this.txtExpenditure.setText("");
        this.txtBalance.setText("");

        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            // 가계부 추가
            Intent intent1 = new Intent(this, AccountBookAddActivity.class);
            intent1.putExtra("date", this.searchDate);
            this.activityLauncher.launch(intent1);
        });

        // 가계부 목록
        list();
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

    /* 가계부 목록 */
    private void list() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 가계부 목록 (일자/시간 정렬 DESC)
        Query query = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.ACCOUNT_BOOK);

        switch (this.searchKind) {
            case Constants.AccountBookSearchKind.DAY:
                // 일 검색
                query = query.whereEqualTo("inputDate", this.searchDate)
                        .orderBy("inputTime", Query.Direction.DESCENDING);
                break;
            case Constants.AccountBookSearchKind.WEEK:
                // 주 검색
                query = query.whereGreaterThanOrEqualTo("inputDate", this.searchDate1)
                        .whereLessThanOrEqualTo("inputDate", this.searchDate2)
                        .orderBy("inputDate", Query.Direction.DESCENDING)
                        .orderBy("inputTime", Query.Direction.DESCENDING);
                break;
            case Constants.AccountBookSearchKind.DETAIL:
                // 상세 검색

                break;
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    this.items = new ArrayList<>();

                    this.totalIncome = 0;
                    this.totalExpenditure = 0;

                    // 가계부 목록
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        // 가계부 정보
                        AccountBook accountBook = document.toObject(AccountBook.class);
                        this.items.add(new AccountBookItem(document.getId(), accountBook));

                        switch (accountBook.getKind()) {
                            case Constants.AccountBookKind.INCOME:
                                // 수입
                                this.totalIncome += accountBook.getMoney();
                                break;
                            case Constants.AccountBookKind.EXPENDITURE:
                                // 지출
                                this.totalExpenditure += accountBook.getMoney();
                                break;
                        }
                    }

                    // 총금액 표시
                    displayTotalMoney();

                    if (items.size() == 0) {
                        // 목록이 없으면
                        this.txtNone.setVisibility(View.VISIBLE);
                    } else {
                        this.txtNone.setVisibility(View.GONE);
                    }

                    // 리스트에 어뎁터 설정
                    this.adapter = new AccountBookAdapter(new OnItemClickListener() {
                        @Override
                        public void onItemClick(View view, int position) {
                            // 클릭 (편집)
                            Intent intent = new Intent(AccountBookListActivity.this, AccountBookEditActivity.class);
                            intent.putExtra("account_book_doc_id", items.get(position).id);
                            intent.putExtra("account_book", items.get(position).accountBook);
                            activityLauncher.launch(intent);
                        }

                        @Override
                        public void onItemLongClick(View view, final int position) {
                            // 롤클릭 (삭제)
                            selectedPosition = position;

                            new AlertDialog.Builder(AccountBookListActivity.this)
                                    .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                                        // 삭제
                                        progressDialog.show();

                                        // 로딩 dialog 를 표시하기 위해 딜레이를 줌
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                            // 가계부 삭제
                                            deleteAccountBook(items.get(position).id);
                                        }, Constants.LoadingDelay.SHORT);
                                    })
                                    .setNegativeButton(R.string.dialog_cancel, null)
                                    .setCancelable(false)
                                    .setTitle(R.string.dialog_title_account_book_delete)
                                    .setMessage(R.string.dialog_msg_account_book_delete)
                                    .show();
                        }
                    }, this.items);
                    this.recyclerView.setAdapter(this.adapter);
                }
            } else {
                // 오류
                Log.d(TAG, "error:" + task.getException().toString());
                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* 가계부 삭제 */
    private void deleteAccountBook(String docId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 가계부 document 참조
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.ACCOUNT_BOOK)
                .document(docId);
        // 가계부 삭제
        reference.delete()
                .addOnSuccessListener(aVoid -> {
                    // 성공
                    this.progressDialog.dismiss();

                    // 리스트에서 삭제
                    AccountBookItem item = this.adapter.remove(this.selectedPosition);

                    switch (item.accountBook.getKind()) {
                        case Constants.AccountBookKind.INCOME:
                            // 수입
                            this.totalIncome -= item.accountBook.getMoney();
                            break;
                        case Constants.AccountBookKind.EXPENDITURE:
                            // 지출
                            this.totalExpenditure -= item.accountBook.getMoney();
                            break;
                    }

                    // 총금액 표시
                    displayTotalMoney();

                    // 삭제후 가계부 내역이 없으면
                    if (items.size() == 0) {
                        this.txtNone.setVisibility(View.VISIBLE);
                    }

                    // 삭제한 내용을 달력에 적용하기 위함
                    setResult(Activity.RESULT_OK);
                })
                .addOnFailureListener(e -> {
                    // 실패
                    this.progressDialog.dismiss();
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                });
    }

    /* 총 금액 표시 */
    public void displayTotalMoney() {
        this.txtIncome.setText(Utils.formatComma(this.totalIncome) + "원");
        this.txtExpenditure.setText(Utils.formatComma(this.totalExpenditure) + "원");
        this.txtBalance.setText(Utils.formatComma(this.totalIncome - this.totalExpenditure) + "원");
    }

    /* 가계부 등록/수정 ActivityForResult */
    private final ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // 가계부 등록/수정 후

                    // 가계부 목록 (새로고침)
                    list();

                    // 등록/수정 한 내용을 달력에 적용하기 위함
                    setResult(Activity.RESULT_OK);
                }
            });
}