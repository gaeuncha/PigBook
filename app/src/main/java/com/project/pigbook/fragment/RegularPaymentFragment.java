package com.project.pigbook.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.project.pigbook.R;
import com.project.pigbook.RegularPaymentAddActivity;
import com.project.pigbook.adapter.RegularPaymentAdapter;
import com.project.pigbook.entity.RegularPayment;
import com.project.pigbook.entity.RegularPaymentItem;
import com.project.pigbook.fragment.abstracts.IFragment;
import com.project.pigbook.listener.OnItemClickListener;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;

import java.util.ArrayList;

public class RegularPaymentFragment extends Fragment implements IFragment {
    //private static final String TAG = RegularPaymentFragment.class.getSimpleName();
    private static final String TAG = "PigBook";

    private Context context;

    private ListenerRegistration registration;  // Firestore 수신대기 리스너

    private ProgressDialog progressDialog;      // 로딩 dialog

    private RecyclerView recyclerView;
    private RegularPaymentAdapter adapter;
    private ArrayList<RegularPaymentItem> items;

    private TextView txtCount, txtNone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_regular_payment, container, false);

        // 로딩 dialog
        this.progressDialog = new ProgressDialog(this.context);
        this.progressDialog.setMessage("처리중...");
        this.progressDialog.setCancelable(false);

        // 리사이클러뷰
        this.recyclerView = view.findViewById(R.id.recyclerView);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL));

        this.txtNone = view.findViewById(R.id.txtNone);
        this.txtCount = view.findViewById(R.id.txtCount);
        this.txtCount.setText("0건 이용중");

        view.findViewById(R.id.fabAdd).setOnClickListener(v -> {
            // 정기결제 추가
            Intent intent = new Intent(this.context, RegularPaymentAddActivity.class);
            startActivity(intent);
        });

        // 리스트 초기화
        this.items = new ArrayList<>();
        // 리스트에 어뎁터 설정
        this.adapter = new RegularPaymentAdapter(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
            }

            @Override
            public void onItemLongClick(View view, final int position) {
                // 롤클릭 (삭제)
                new AlertDialog.Builder(context)
                        .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                            // 삭제
                            progressDialog.show();

                            // 로딩 dialog 를 표시하기 위해 딜레이를 줌
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                // 정기결제 삭제
                                deleteRegularPayment(items.get(position).id);
                            }, Constants.LoadingDelay.SHORT);
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .setCancelable(false)
                        .setTitle(R.string.dialog_title_regular_payment_delete)
                        .setMessage(R.string.dialog_msg_regular_payment_delete)
                        .show();
            }
        }, this.items);
        this.recyclerView.setAdapter(this.adapter);

        this.txtNone.setVisibility(View.VISIBLE);

        // 정기결제 목록
        list();

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
    public void onDestroy() {
        super.onDestroy();

        // Firestore 수신대기 리스너 제거
        if (this.registration != null) {
            this.registration.remove();
        }
    }

    @Override
    public boolean isExecuted() {
        return this.progressDialog.isShowing();
    }

    /* 정기결제 목록 (push 방식) */
    private void list() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 정기결제 목록 (결제시작일 정렬)
        Query query = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.REGULAR_PAYMENT)
                .orderBy("startDate");

        this.registration = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                // 오류
                Log.d(TAG, e.toString());
                Toast.makeText(this.context, R.string.msg_listener_error, Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            Log.d(TAG, "ADDED");

                            this.txtNone.setVisibility(View.GONE);

                            // 정기결제 item 구성
                            RegularPayment regularPayment = dc.getDocument().toObject(RegularPayment.class);

                            // 리스트에 추가
                            this.adapter.add(new RegularPaymentItem(dc.getDocument().getId(), regularPayment));
                            this.recyclerView.scrollToPosition(this.adapter.getItemCount() - 1);

                            this.txtCount.setText(this.items.size() + "건 이용중");
                            break;
                        case MODIFIED:
                            Log.d(TAG, "MODIFIED");

                            // 정기결제의 해당 위치 찾음
                            for (int i=0; i<this.items.size(); i++) {
                                if (this.items.get(i).id.equals(dc.getDocument().getId())) {
                                    RegularPaymentItem regularPaymentItem = items.get(i);

                                    // 정기결제 정보 적용
                                    regularPaymentItem.regularPayment = dc.getDocument().toObject(RegularPayment.class);

                                    this.adapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                            break;
                        case REMOVED:
                            Log.d(TAG, "REMOVED");

                            // 정기결제의 해당 위치 찾음
                            for (int i=0; i<this.items.size(); i++) {
                                if (this.items.get(i).id.equals(dc.getDocument().getId())) {
                                    // 정기결제 삭제
                                    this.adapter.remove(i);
                                    break;
                                }
                            }

                            // 내역이 없으면
                            if (this.items.size() == 0) {
                                this.txtNone.setVisibility(View.VISIBLE);
                            }

                            this.txtCount.setText(this.items.size() + "건 이용중");
                            break;
                    }
                }
            } else {
                Toast.makeText(this.context, R.string.msg_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* 정기결제 삭제 */
    private void deleteRegularPayment(String docId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 정기결제 document 참조
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.REGULAR_PAYMENT)
                .document(docId);
        // 정기결제 삭제
        reference.delete()
                .addOnSuccessListener(aVoid -> {
                    // 성공
                    this.progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    // 실패
                    this.progressDialog.dismiss();
                    Toast.makeText(this.context, R.string.msg_error, Toast.LENGTH_SHORT).show();
                });
    }
}
