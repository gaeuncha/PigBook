package com.project.pigbook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.pigbook.R;
import com.project.pigbook.entity.MessageItem;
import com.project.pigbook.listener.OnItemClickListener;

import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private OnItemClickListener listener;
    private ArrayList<MessageItem> items;

    public MessageAdapter(OnItemClickListener listener, ArrayList<MessageItem> items) {
        this.listener = listener;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, null);

        // Item 사이즈 조절
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);

        // ViewHolder 생성
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtNumber.setText(this.items.get(position).number);          // 번호
        holder.txtDateTime.setText(this.items.get(position).dateTime);      // 받은 일시
        holder.txtMessage.setText(this.items.get(position).message);        // 메시지 내용
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView txtNumber, txtDateTime, txtMessage;

        private ViewHolder(View view) {
            super(view);

            this.txtNumber = view.findViewById(R.id.txtNumber);
            this.txtDateTime = view.findViewById(R.id.txtDateTime);
            this.txtMessage = view.findViewById(R.id.txtMessage);

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