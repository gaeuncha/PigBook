package com.project.pigbook.listener;

import android.view.View;

public interface OnItemClickListener {

    /* 아이템 클릭 */
    void onItemClick(View view, int position);

    /* 아이템 롱 클릭 */
    void onItemLongClick(View view, int position);

}
