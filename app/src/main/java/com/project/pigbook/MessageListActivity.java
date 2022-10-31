package com.project.pigbook;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.project.pigbook.adapter.MessageAdapter;
import com.project.pigbook.data.MessageDetectionData;
import com.project.pigbook.entity.AccountBook;
import com.project.pigbook.entity.MessageItem;
import com.project.pigbook.listener.OnItemClickListener;
import com.project.pigbook.popupwindow.AccountBookAddPopup;
import com.project.pigbook.popupwindow.CategoryPopup;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MessageListActivity extends AppCompatActivity {
    //private static final String TAG = SMSListActivity.class.getSimpleName();
    private static final String TAG = "PigBook";

    private ProgressDialog progressDialog;      // 로딩 dialog

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private ArrayList<MessageItem> items;

    private TextView txtNone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);

        // 제목
        setTitle(R.string.title_sms_list);

        // 홈버튼(<-) 표시
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // 로딩 dialog
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setMessage("처리중...");
        this.progressDialog.setCancelable(false);

        // 리사이클러뷰
        this.recyclerView = findViewById(R.id.recyclerView);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        this.txtNone = findViewById(R.id.txtNone);

        // 메시지 목록
        list();
    }

    @Override
    public void onBackPressed() {
        // 처리중이면 닫기 취소
        if (this.progressDialog.isShowing()) {
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* 메시지 목록 */
    private void list() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, (-Constants.MESSAGE_SEARCH_MONTH));

        // 받은 sms 목록
        Cursor cursor = getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI,
                new String[]{ "date", "address", "body" },
                "date >= ?",
                new String[]{ String.valueOf(c.getTimeInMillis()) },
                "date DESC");

        if (cursor != null) {
            this.items = new ArrayList<>();

            while (cursor.moveToNext()) {
                // 일시 표현
                String date = Utils.getDate("yyyy-MM-dd HH:mm:ss", cursor.getLong(cursor.getColumnIndex("date")));

                String number = cursor.getString(cursor.getColumnIndex("address"));
                String message = cursor.getString(cursor.getColumnIndex("body"));

                // [Web발신] 으로 시작하는 문자만 추출
                //if (message.indexOf("[Web발신]") == 0 || message.indexOf("[WEB발신]") == 0) {
                    this.items.add(new MessageItem(date, number, message));
                //}

                //Log.d(TAG, "date : " + date);
                //Log.d(TAG, "number : " + number);
                //Log.d(TAG, "message : " + message);
            }

            cursor.close();

            Log.d(TAG, "count:" + items.size());

            if (items.size() == 0) {
                // 목록이 없으면
                this.txtNone.setVisibility(View.VISIBLE);
            } else {
                this.txtNone.setVisibility(View.GONE);
            }

            // 리스트에 어뎁터 설정
            this.adapter = new MessageAdapter(new OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    // 클릭
                    checkMessage(items.get(position));
                }

                @Override
                public void onItemLongClick(View view, final int position) {
                }
            }, this.items);
            this.recyclerView.setAdapter(this.adapter);
        } else {
            Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
        }
    }

    /* 문자 메시지 체크 */
    private void checkMessage(MessageItem item) {
        boolean detected = false;

        // 감지할 수 있는 문자메시지인지 체크
        for (String str : MessageDetectionData.getInstance().getItems()) {
            String[] texts = str.split("\n");
            String[] values = item.message.split("\n");

            if (texts.length == values.length) {
                long money = 0;
                String date = null, time = null, memo = null;

                for (int i=0; i<texts.length; i++) {
                    if (texts[i].charAt(0) == '#') {
                        // 문자 체크
                        if (texts[i].substring(1).equals(values[i])) {
                            Log.d(TAG, values[i]);
                        } else {
                            break;
                        }
                    } else {
                        switch (texts[i].substring(1)) {
                            case Constants.MessageDetectionType.MONEY:
                                // 금액
                                String temp = values[i].substring(0, values[i].indexOf("원"));
                                money = Long.parseLong(temp.replace(",", ""));
                                Log.d(TAG, money + "원");
                                break;
                            case Constants.MessageDetectionType.DATE:
                                // 날자
                                String[] d = values[i].split(" ");
                                date = item.dateTime.substring(0, 4) + "-" + d[0].replace("/", "-");
                                Log.d(TAG, date);
                                time = d[1];
                                Log.d(TAG, d[1]);
                                break;
                            case Constants.MessageDetectionType.MEMO:
                                // 내용
                                memo = values[i];
                                Log.d(TAG, memo);
                                break;
                        }
                    }
                }

                // 문자 감지 성공
                if (!TextUtils.isEmpty(date) && !TextUtils.isEmpty(time) && !TextUtils.isEmpty(memo)) {
                    // 등록할 가계부 객체 생성
                    AccountBook accountBook = new AccountBook(Constants.AccountBookKind.EXPENDITURE,
                            Constants.AccountBookAssetsKind.CARD, "", memo, money, date, time);

                    // 가계부 등록 팝업창 호출 (분류를 선택하기 위함)
                    onPopupAccountBookAdd(accountBook);

                    detected = true;
                    break;
                }
            }
        }

        if (!detected) {
            Toast.makeText(this, R.string.msg_can_not_detect, Toast.LENGTH_SHORT).show();
        }
    }

    /* 가계부 등록 */
    private void inputAccountBook(final AccountBook accountBook) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 가계부 등록
        db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.ACCOUNT_BOOK)
                .add(accountBook)
                .addOnSuccessListener(documentReference -> {
                    // 성공
                    this.progressDialog.dismiss();

                    // 등록한 내용을 달력에 적용하기 위함
                    setResult(Activity.RESULT_OK);
                })
                .addOnFailureListener(e -> {
                    // 등록 실패
                    this.progressDialog.dismiss();
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                });
    }

    /* 가계부 등록 팝업창 호출 */
    private void onPopupAccountBookAdd(final AccountBook accountBook) {
        View popupView = View.inflate(this, R.layout.popup_account_book_add, null);
        AccountBookAddPopup popup = new AccountBookAddPopup(popupView, (view, bundle) -> {
            // 확인버튼 클릭시
            if (view.getId() == R.id.btnOk) {
                // 분류 설정
                accountBook.setCategory(bundle.getString("category_name"));

                this.progressDialog.show();

                // 로딩 dialog 를 표시하기 위해 딜레이를 줌
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // 가게부 등록
                    inputAccountBook(accountBook);
                }, Constants.LoadingDelay.SHORT);
            }
        }, accountBook);
        // Back 키 눌렸을때 닫기 위함
        popup.setFocusable(true);
        popup.showAtLocation(popupView, Gravity.CENTER, 0, 0);
    }
}
