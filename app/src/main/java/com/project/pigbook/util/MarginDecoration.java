package com.project.pigbook.util;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class MarginDecoration extends RecyclerView.ItemDecoration {
    private final int margin;

    public MarginDecoration(Context context, int margin) {
        this.margin = margin;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(this.margin, this.margin, this.margin, this.margin);
        //outRect.set(-margin, 0, -margin, 0);
    }
}
