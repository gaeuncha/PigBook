package com.project.pigbook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.pigbook.R;
import com.project.pigbook.entity.CategoryItem;
import com.project.pigbook.listener.OnItemClickListener;

import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private OnItemClickListener listener;
    private ArrayList<CategoryItem> items;

    public CategoryAdapter(OnItemClickListener listener, ArrayList<CategoryItem> items) {
        this.listener = listener;
        this.items = items;
    }

    /* 추가 (하단에 추가) */
    public void add(CategoryItem data) {
        add(data, -1);
    }

    /* 추가 */
    public void add(CategoryItem data, int position) {
        position = position == -1 ? getItemCount()  : position;
        // 분류 추가
        this.items.add(position, data);
        // 추가된 분류를 리스트에 적용하기 위함
        notifyItemInserted(position);
    }

    /* 삭제 */
    public CategoryItem remove(int position){
        CategoryItem data = null;

        if (position < getItemCount()) {
            data = this.items.get(position);
            // 분류 삭제
            this.items.remove(position);
            // 삭제된 분류를 리스트에 적용하기 위함
            notifyItemRemoved(position);
        }

        return data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, null);

        // Item 사이즈 조절
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);

        // ViewHolder 생성
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtCategory.setText(this.items.get(position).category.getName());    // 분류명
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView txtCategory;
        ImageButton btnEdit;

        private ViewHolder(View view) {
            super(view);

            this.txtCategory = view.findViewById(R.id.txtCategory);
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
