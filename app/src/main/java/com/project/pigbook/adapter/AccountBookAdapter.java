package com.project.pigbook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.project.pigbook.R;
import com.project.pigbook.entity.AccountBookItem;
import com.project.pigbook.listener.OnItemClickListener;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;

public class AccountBookAdapter extends RecyclerView.Adapter<AccountBookAdapter.ViewHolder> {

    private OnItemClickListener listener;
    private ArrayList<AccountBookItem> items;

    public AccountBookAdapter(OnItemClickListener listener, ArrayList<AccountBookItem> items) {
        this.listener = listener;
        this.items = items;
    }

    /* 추가 (하단에 추가) */
    public void add(AccountBookItem data) {
        add(data, -1);
    }

    /* 추가 */
    public void add(AccountBookItem data, int position) {
        position = position == -1 ? getItemCount()  : position;
        // 가계부 추가
        this.items.add(position, data);
        // 추가된 가계부를 리스트에 적용하기 위함
        notifyItemInserted(position);
    }

    /* 삭제 */
    public AccountBookItem remove(int position){
        AccountBookItem data = null;

        if (position < getItemCount()) {
            data = this.items.get(position);
            // 가계부 삭제
            this.items.remove(position);
            // 삭제된 가계부를 리스트에 적용하기 위함
            notifyItemRemoved(position);
        }

        return data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account_book, null);

        // Item 사이즈 조절
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 날자
        String dateTime = this.items.get(position).accountBook.getInputDate() + " " + this.items.get(position).accountBook.getInputTime();
        holder.txtDateTime.setText(dateTime);

        // 분류
        String category = "[" + this.items.get(position).accountBook.getCategory() + "]";
        holder.txtCategory.setText(category);

        holder.txtMemo.setText(this.items.get(position).accountBook.getMemo()); // 내용

        // 지출/수입
        if (this.items.get(position).accountBook.getKind() == Constants.AccountBookKind.EXPENDITURE) {
            holder.txtKind.setText("지출");
            holder.txtKind.setTextColor(ContextCompat.getColor(holder.txtKind.getContext(), R.color.red_text_color));
            holder.txtMoney.setTextColor(ContextCompat.getColor(holder.txtMoney.getContext(), R.color.red_text_color));
        } else {
            holder.txtKind.setText("수입");
            holder.txtKind.setTextColor(ContextCompat.getColor(holder.txtKind.getContext(), R.color.blue_text_color));
            holder.txtMoney.setTextColor(ContextCompat.getColor(holder.txtMoney.getContext(), R.color.blue_text_color));
        }

        // 현금/카드
        if (this.items.get(position).accountBook.getAssetsKind() == Constants.AccountBookAssetsKind.CASH) {
            holder.txtAssetsKind.setText("현금");
        } else {
            holder.txtAssetsKind.setText("카드");
        }

        // 금액
        String money = Utils.formatComma(this.items.get(position).accountBook.getMoney()) + "원";
        holder.txtMoney.setText(money);
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }



    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView txtDateTime, txtMemo, txtCategory, txtKind, txtAssetsKind, txtMoney;
        ImageButton btnEdit;

        private ViewHolder(View view) {
            super(view);

            this.txtDateTime = view.findViewById(R.id.txtDateTime);
            this.txtMemo = view.findViewById(R.id.txtMemo);
            this.txtCategory = view.findViewById(R.id.txtCategory);
            this.txtKind = view.findViewById(R.id.txtKind);
            this.txtAssetsKind = view.findViewById(R.id.txtAssetsKind);
            this.txtMoney = view.findViewById(R.id.txtMoney);
            this.btnEdit = view.findViewById(R.id.btnEdit);

            this.btnEdit.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // 편집 선택
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(view, position);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            // 롱클릭시 삭제 처리 하기
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemLongClick(view, position);
            }

            // 다른데서는 처리할 필요없음 true
            return true;
        }
    }
}
