package com.project.pigbook.popupwindow;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.project.pigbook.R;
import com.project.pigbook.listener.OnPopupClickListener;
import com.project.pigbook.util.Constants;

public class CategoryPopup extends PopupWindow {

    private OnPopupClickListener listener;
    private EditText editCategory;

    private int mode;                       // 등록(0), 수정(1)

    public CategoryPopup(View view, OnPopupClickListener listener, int kind, String name) {
        super(view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        this.listener = listener;

        String title = "분류";
        if (kind == Constants.AccountBookKind.EXPENDITURE) {
            title += "(지출)";
        } else {
            title += "(수입)";
        }

        if (TextUtils.isEmpty(name)) {
            title += " 등록";
            this.mode = Constants.EditMode.N;
        } else {
            title += " 수정";
            this.mode = Constants.EditMode.U;
        }
        ((TextView) view.findViewById(R.id.txtTitle)).setText(title);

        this.editCategory = view.findViewById(R.id.editCategory);
        this.editCategory.setHint("분류명");
        this.editCategory.setText(name);

        view.findViewById(R.id.btnOk).setOnClickListener(view1 -> {
            // 확인
            String text = this.editCategory.getText().toString();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(getContentView().getContext(), R.string.msg_category_check_empty, Toast.LENGTH_SHORT).show();
                this.editCategory.requestFocus();
                return;
            }

            // 모드와 분류명을 넘겨줌
            Bundle bundle = new Bundle();
            bundle.putString("category_name", text);
            bundle.putInt("mode", this.mode);

            this.listener.onClick(view1, bundle);

            dismiss();
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(view1 -> {
            // 취소
            dismiss();
        });
    }

}
