package com.project.pigbook.popupwindow;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.pigbook.R;
import com.project.pigbook.entity.AccountBook;
import com.project.pigbook.entity.Category;
import com.project.pigbook.listener.OnPopupClickListener;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;

public class AccountBookAddPopup extends PopupWindow {

    private OnPopupClickListener listener;
    private Spinner spCategory;

    public AccountBookAddPopup(View view, OnPopupClickListener listener, AccountBook accountBook) {
        super(view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        this.listener = listener;

        // 등록 일시
        String dateTime = accountBook.getInputDate() + " " + accountBook.getInputTime();
        ((TextView) view.findViewById(R.id.txtDateTime)).setText(dateTime);

        if (accountBook.getAssetsKind() == Constants.AccountBookAssetsKind.CASH) {
            ((TextView) view.findViewById(R.id.txtAssetsKind)).setText("현금");
        } else {
            ((TextView) view.findViewById(R.id.txtAssetsKind)).setText("카드");
        }

        // 금액
        String money = Utils.formatComma(accountBook.getMoney()) + "원";
        ((TextView) view.findViewById(R.id.txtMoney)).setText(money);

        ((TextView) view.findViewById(R.id.txtMemo)).setText(accountBook.getMemo());

        this.spCategory = view.findViewById(R.id.spCategory);

        view.findViewById(R.id.btnOk).setOnClickListener(view1 -> {
            // 확인
            if (this.spCategory.getSelectedItemPosition() == 0) {
                Toast.makeText(getContentView().getContext(), R.string.msg_category_select_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            // 분류명을 넘겨줌
            Bundle bundle = new Bundle();
            bundle.putString("category_name", this.spCategory.getSelectedItem().toString());

            this.listener.onClick(view1, bundle);

            dismiss();
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(view1 -> {
            // 취소
            dismiss();
        });

        // 분류 구성하기
        createCategory(accountBook.getKind());
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
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContentView().getContext(), R.layout.spinner_item, items);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

            this.spCategory.setAdapter(adapter);
        });
    }
}
