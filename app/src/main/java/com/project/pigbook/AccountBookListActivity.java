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
import android.text.TextUtils;
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
    private long searchMoney1, searchMoney2;    // 시작금액, 마지막금액
    private String searchCategory;              // 검색카테고리

    private int kind;                           // 지출 / 수입

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
                this.kind = intent.getIntExtra("kind", Constants.AccountBookKind.EXPENDITURE);
                this.searchCategory = intent.getStringExtra("search_category");
                this.searchMoney1 = intent.getLongExtra("search_money1", 0);
                this.searchMoney2 = intent.getLongExtra("search_money2", 0);
                this.searchDate1 = intent.getStringExtra("search_date1");
                this.searchDate2 = intent.getStringExtra("search_date2");
                setTitle("상세검색");

                String text;
                if (this.kind == Constants.AccountBookKind.EXPENDITURE) {
                    text = "지출";
                } else {
                    text = "수입";
                }
                if (!TextUtils.isEmpty(this.searchCategory)) {
                    text += ", " + this.searchCategory;
                }
                if (this.searchMoney1 > 0 && this.searchMoney2 > 0) {
                    // 시작금액이 마지막금액보다 크면
                    if (this.searchMoney1 > this.searchMoney2) {
                        long money = this.searchMoney1;
                        this.searchMoney1 = this.searchMoney2;
                        this.searchMoney2 = money;
                    }
                    text += ", 금액 " + this.searchMoney1 + " ~ " + this.searchMoney2;
                } else if (this.searchMoney1 > 0) {
                    text += ", 금액 " + this.searchMoney1 + " ~ ";
                } else if (this.searchMoney2 > 0) {
                    text += ", 금액 ~ " + this.searchMoney2;
                }
                if (Utils.isDate("yyyy-MM-dd", this.searchDate1) && Utils.isDate("yyyy-MM-dd", this.searchDate2)) {
                    // 시작일이 마지막일보다 이후이면
                    if (this.searchDate2.compareTo(this.searchDate1) < 0) {
                        String date = this.searchDate1;
                        this.searchDate1 = this.searchDate2;
                        this.searchDate2 = date;
                    }
                    text += ", 기간 " + this.searchDate1 + " ~ " + this.searchDate2;
                } else if (Utils.isDate("yyyy-MM-dd", this.searchDate1)) {
                    text += ", 기간 " + this.searchDate1 + " ~ ";
                } else if (Utils.isDate("yyyy-MM-dd", this.searchDate2)) {
                    text += ", 기간 ~ " + this.searchDate2;
                }

                txtSearchRequirement.setText(text);

                // 추가버튼 숨김
                findViewById(R.id.fabAdd).setVisibility(View.GONE);
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
                query = query.whereEqualTo("kind", this.kind);

                // 분류
                if (!TextUtils.isEmpty(this.searchCategory)) {
                    query = query.whereEqualTo("category", this.searchCategory);
                }

                /* 금액 orderBy inputDate 하고 같이 사용할 수 없음
                if (this.searchMoney1 > 0) {
                    query = query.whereGreaterThanOrEqualTo("money", this.searchMoney1);
                }
                if (this.searchMoney2 > 0) {
                    query = query.whereLessThanOrEqualTo("money", this.searchMoney2);
                }
                */

                // 기간
                if (Utils.isDate("yyyy-MM-dd", this.searchDate1)) {
                    query = query.whereGreaterThanOrEqualTo("inputDate", this.searchDate1);
                }
                if (Utils.isDate("yyyy-MM-dd", this.searchDate2)) {
                    query = query.whereLessThanOrEqualTo("inputDate", this.searchDate2);
                }

                // 정렬
                query = query.orderBy("inputDate", Query.Direction.DESCENDING)
                        .orderBy("inputTime", Query.Direction.DESCENDING);
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

                        // 상세검색이면
                        if (this.searchKind == Constants.AccountBookSearchKind.DETAIL) {
                            // 금액 검색 (시작)
                            if (this.searchMoney1 > 0) {
                                if (accountBook.getMoney() < this.searchMoney1) {
                                    accountBook = null;
                                }
                            }

                            // 금액 검색 (끝)
                            if (accountBook != null && this.searchMoney2 > 0) {
                                if (accountBook.getMoney() > this.searchMoney2) {
                                    accountBook = null;
                                }
                            }
                        }

                        if (accountBook != null) {
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