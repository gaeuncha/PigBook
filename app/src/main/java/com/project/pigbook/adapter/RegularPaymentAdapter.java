package com.project.pigbook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.pigbook.R;
import com.project.pigbook.entity.RegularPaymentItem;
import com.project.pigbook.listener.OnItemClickListener;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;

public class RegularPaymentAdapter extends RecyclerView.Adapter<RegularPaymentAdapter.ViewHolder> {

    private OnItemClickListener listener;
    private ArrayList<RegularPaymentItem> items;

    public RegularPaymentAdapter(OnItemClickListener listener, ArrayList<RegularPaymentItem> items) {
        this.listener = listener;
        this.items = items;
    }

    /* 추가 (하단에 추가) */
    public void add(RegularPaymentItem data) {
        add(data, -1);
    }

    /* 추가 */
    public void add(RegularPaymentItem data, int position) {
        position = position == -1 ? getItemCount()  : position;
        // 정기결제 추가
        this.items.add(position, data);
        // 추가된 정기결제를 리스트에 적용하기 위함
        notifyItemInserted(position);
    }

    /* 삭제 */
    public RegularPaymentItem remove(int position){
        RegularPaymentItem data = null;

        if (position < getItemCount()) {
            data = this.items.get(position);
            // 정기결제 삭제
            this.items.remove(position);
            // 삭제된 정기결제를 리스트에 적용하기 위함
            notifyItemRemoved(position);
        }

        return data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_regular_payment, null);

        // Item 사이즈 조절
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);

        // ViewHolder 생성
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtName.setText(this.items.get(position).regularPayment.getName());  // 정기결제 이름

        // 분류
        String category = "[" + this.items.get(position).regularPayment.getCategory() + "]";
        holder.txtCategory.setText(category);

        holder.txtStartDate.setText(this.items.get(position).regularPayment.getStartDate());    // 결제시작일

        // 현금/카드
        if (this.items.get(position).regularPayment.getAssetsKind() == Constants.AccountBookAssetsKind.CASH) {
            holder.txtAssetsKind.setText("현금");
        } else {
            holder.txtAssetsKind.setText("카드");
        }

        // 금액
        String money = Utils.formatComma(this.items.get(position).regularPayment.getMoney()) + "원";
        holder.txtMoney.setText(money);
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {
        TextView txtName, txtCategory, txtStartDate, txtAssetsKind, txtMoney;

        private ViewHolder(View view) {
            super(view);

            this.txtName = view.findViewById(R.id.txtName);
            this.txtCategory = view.findViewById(R.id.txtCategory);
            this.txtStartDate = view.findViewById(R.id.txtStartDate);
            this.txtAssetsKind = view.findViewById(R.id.txtAssetsKind);
            this.txtMoney = view.findViewById(R.id.txtMoney);

            view.setOnLongClickListener(this);
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
