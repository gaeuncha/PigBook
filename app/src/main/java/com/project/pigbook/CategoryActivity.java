package com.project.pigbook;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.pigbook.adapter.CategoryAdapter;
import com.project.pigbook.entity.Category;
import com.project.pigbook.entity.CategoryItem;
import com.project.pigbook.listener.OnItemClickListener;
import com.project.pigbook.popupwindow.CategoryPopup;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;

import java.util.ArrayList;
import java.util.Objects;

public class CategoryActivity extends AppCompatActivity {
    //private static final String TAG = CategoryActivity.class.getSimpleName();
    private static final String TAG = "PigBook";

    private ProgressDialog progressDialog;      // 로딩 dialog

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private ArrayList<CategoryItem> items;

    private TextView txtCount, txtNone;

    private int kind;                           // 가계부 종류 (지출/수입)

    private int selectedPosition = -1;          // 분류 리스트 위치

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // 가계부 종류
        Intent intent = getIntent();
        this.kind = intent.getIntExtra("kind", Constants.AccountBookKind.EXPENDITURE);

        String title;
        if (this.kind == Constants.AccountBookKind.EXPENDITURE) {
            title = getString(R.string.title_category_1);
        } else {
            title = getString(R.string.title_category_2);
        }

        // 제목
        setTitle(title);

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
        this.txtCount = findViewById(R.id.txtCount);
        this.txtCount.setText("");

        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            // 추가
            // 분류 팝업창 호출
            onPopupCategory(this.kind, "");
        });

        // 분류 목록
        list(this.kind);
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

    /* 분류 목록 */
    private void list(int kind) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 분류 목록 (분류명으로 정렬)
        Query query = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.CATEGORY)
                .whereEqualTo("kind", kind)
                .orderBy("name");

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    this.items = new ArrayList<>();

                    // 분류 목록
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        // 분류 정보
                        Category category = document.toObject(Category.class);
                        this.items.add(new CategoryItem(document.getId(), category));
                    }

                    if (items.size() == 0) {
                        // 목록이 없으면
                        this.txtNone.setVisibility(View.VISIBLE);
                    } else {
                        this.txtNone.setVisibility(View.GONE);
                    }

                    this.txtCount.setText("분류 " + items.size());

                    // 리스트에 어뎁터 설정
                    this.adapter = new CategoryAdapter(new OnItemClickListener() {
                        @Override
                        public void onItemClick(View view, int position) {
                            // 클릭 (편집)
                            selectedPosition = position;

                            // 분류 수정 팝업창 호출
                            onPopupCategory(kind, items.get(position).category.getName());
                        }

                        @Override
                        public void onItemLongClick(View view, final int position) {
                            // 롤클릭 (삭제)
                            selectedPosition = position;

                            new AlertDialog.Builder(CategoryActivity.this)
                                    .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                                        // 삭제
                                        progressDialog.show();

                                        // 로딩 dialog 를 표시하기 위해 딜레이를 줌
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                            // 분류 삭제
                                            deleteCategory(items.get(position).id);
                                        }, Constants.LoadingDelay.SHORT);
                                    })
                                    .setNegativeButton(R.string.dialog_cancel, null)
                                    .setCancelable(false)
                                    .setTitle(R.string.dialog_title_category_delete)
                                    .setMessage(R.string.dialog_msg_category_delete)
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

    /* 분류 중복 체크 */
    private void overlapCategory(final int kind, final String name, final int mode) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 사용자 분류 Collection 참조
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.CATEGORY);

        // 분류 중복 체크
        Query query = reference.whereEqualTo("kind", kind).whereEqualTo("name", name);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    if (task.getResult().size() == 0) {
                        // 중복 아님
                        if (mode == 0) {
                            // 등록
                            inputCategory(kind, name);
                        } else {
                            // 수정
                            String categoryId = this.items.get(this.selectedPosition).id;
                            modifyCategory(categoryId, name);
                        }
                    } else {
                        // 분류 중복
                        this.progressDialog.dismiss();
                        Toast.makeText(this, R.string.msg_category_check_overlap, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    this.progressDialog.dismiss();
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                }
            } else {
                // 오류
                this.progressDialog.dismiss();
                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* 분류 등록 */
    private void inputCategory(int kind, String name) {
        final Category category = new Category(kind, name);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 분류 등록
        db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.CATEGORY)
                .add(category)
                .addOnSuccessListener(documentReference -> {
                    // 성공
                    this.progressDialog.dismiss();

                    CategoryItem item = new CategoryItem(documentReference.getId(), category);

                    // 리스트에 추가
                    this.adapter.add(item);
                    this.recyclerView.scrollToPosition(this.adapter.getItemCount() - 1);

                    this.txtNone.setVisibility(View.GONE);
                    this.txtCount.setText("분류 " + items.size());
                })
                .addOnFailureListener(e -> {
                    // 등록 실패
                    this.progressDialog.dismiss();
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                });
    }

    /* 분류 수정 (분류명만 수정) */
    private void modifyCategory(String categoryId, final String name) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 분류 document 참조
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.CATEGORY)
                .document(categoryId);
        // 분류 수정
        reference.update("name", name)
                .addOnSuccessListener(aVoid -> {
                    // 성공
                    this.progressDialog.dismiss();

                    // 변경된 분류명 적용
                    this.items.get(this.selectedPosition).category.setName(name);
                    this.adapter.notifyItemChanged(this.selectedPosition);
                })
                .addOnFailureListener(e -> {
                    // 실패
                    this.progressDialog.dismiss();
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                });
    }

    /* 분류 삭제 */
    private void deleteCategory(String categoryId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 분류 document 참조
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.CATEGORY)
                .document(categoryId);
        // 분류 삭제
        reference.delete()
                .addOnSuccessListener(aVoid -> {
                    // 성공
                    this.progressDialog.dismiss();

                    // 리스트에서 삭제
                    this.adapter.remove(this.selectedPosition);

                    // 삭제후 분류가 없으면
                    if (items.size() == 0) {
                        this.txtNone.setVisibility(View.VISIBLE);
                    }
                    this.txtCount.setText("분류 " + items.size());
                })
                .addOnFailureListener(e -> {
                    // 실패
                    this.progressDialog.dismiss();
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                });
    }

    /* 분류 등록/수정 팝업창 호출 */
    private void onPopupCategory(final int kind, String name) {
        View popupView = View.inflate(this, R.layout.popup_category, null);
        CategoryPopup popup = new CategoryPopup(popupView, (view, bundle) -> {
            // 확인버튼 클릭시
            if (view.getId() == R.id.btnOk) {
                final int mode = bundle.getInt("mode");
                final String categoryName = bundle.getString("category_name");

                if (mode == 1) {
                    // 수정이면 분류명이 변경되었는지 체크
                    if (categoryName.equals(this.items.get(this.selectedPosition).category.getName())) {
                        // 변경되지 않음 (수정할 필요 없음)
                        return;
                    }
                }

                this.progressDialog.show();

                // 로딩 dialog 를 표시하기 위해 딜레이를 줌
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // 분류 중복 체크 후 등록 및 수정
                    overlapCategory(kind, categoryName, mode);
                }, Constants.LoadingDelay.SHORT);
            }
        }, kind, name);
        // Back 키 눌렸을때 닫기 위함
        popup.setFocusable(true);
        popup.showAtLocation(popupView, Gravity.CENTER, 0, 0);
    }
}