package com.project.pigbook.adapter;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.project.pigbook.R;
import com.project.pigbook.entity.CalendarDay;
import com.project.pigbook.listener.OnItemClickListener;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private OnItemClickListener listener;
    private ArrayList<CalendarDay> items;

    // 레이아웃 사이즈
    private int layoutWidth;
    private int layoutHeight;

    private float displayDensity;                   // db 사이즈 구할 때 사용됨

    public CalendarAdapter(OnItemClickListener listener, ArrayList<CalendarDay> items,
                           int layoutWidth, int layoutHeight, float displayDensity) {
        this.listener = listener;
        this.items = items;

        this.layoutWidth = layoutWidth;
        this.layoutHeight = layoutHeight;
        this.displayDensity = displayDensity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtDay.setText(this.items.get(position).day);

        if (this.items.get(position).today) {
            holder.imgToday.setVisibility(View.VISIBLE);
            holder.txtDay.setTextColor(Color.WHITE);
        } else {
            holder.imgToday.setVisibility(View.INVISIBLE);

            switch (this.items.get(position).week) {
                case 1:
                    // 일요일
                    holder.txtDay.setTextColor(ContextCompat.getColor(holder.txtDay.getContext(), R.color.red_text_color));
                    break;
                case 7:
                    // 토요일
                    holder.txtDay.setTextColor(ContextCompat.getColor(holder.txtDay.getContext(), R.color.blue_text_color));
                    break;
                default:
                    holder.txtDay.setTextColor(Color.BLACK);
                    break;
            }
        }

        // 수입
        displayValue(holder.txtIncome, this.items.get(position).income);

        // 지출
        displayValue(holder.txtExpenditure, this.items.get(position).expenditure);

        // 잔액 (수입, 지출 둘다 있을경우에만 잔액 표시)
        if (this.items.get(position).income != 0 && this.items.get(position).expenditure != 0) {
            displayValue(holder.txtBalance, this.items.get(position).income - this.items.get(position).expenditure);
        } else {
            holder.txtBalance.setText("");
        }

        int space;
        if (position > 34 ) {
            // 맨 마지막 가로 라인은 표시 안함
            holder.viewB.setVisibility(View.GONE);
            space = 0;
        } else {
            holder.viewB.setVisibility(View.VISIBLE);
            // 라인 dp 사이즈
            space = Math.round(1 * this.displayDensity);
        }

        if (TextUtils.isEmpty(this.items.get(position).day)) {
            holder.layDay.setBackgroundResource(0);
        } else {
            holder.layDay.setBackgroundResource(R.drawable.list_item_selector);
        }

        // 레이아웃 사이즈 조절
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(this.layoutWidth, this.layoutHeight + space);
        holder.layDay.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    /* 값 표시 */
    private void displayValue(TextView textView, long value) {
        if (value == 0) {
            textView.setText("");
        } else {
            textView.setText(Utils.formatComma(value));
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        LinearLayout layDay;
        TextView txtDay;
        TextView txtIncome, txtExpenditure, txtBalance;
        ImageView imgToday;

        View viewB;             // 가로 라인

        private ViewHolder(View view) {
            super(view);

            this.layDay = view.findViewById(R.id.layDay);
            this.txtDay = view.findViewById(R.id.txtDay);
            this.txtIncome = view.findViewById(R.id.txtIncome);
            this.txtExpenditure = view.findViewById(R.id.txtExpenditure);
            this.txtBalance = view.findViewById(R.id.txtBalance);
            this.imgToday = view.findViewById(R.id.imgToday);
            this.viewB = view.findViewById(R.id.viewB);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // 선택
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(view, position);
            }
        }
    }
}
