package com.project.pigbook;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.project.pigbook.adapter.CategoryAdapter;
import com.project.pigbook.entity.AccountBook;
import com.project.pigbook.entity.Category;
import com.project.pigbook.entity.CategoryItem;
import com.project.pigbook.entity.RegularPayment;
import com.project.pigbook.entity.User;
import com.project.pigbook.listener.OnItemClickListener;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class IntroActivity extends AppCompatActivity {
    //private static final String TAG = IntroActivity.class.getSimpleName();
    private static final String TAG = "PigBook";

    private GoogleSignInClient googleSignInClient;      // ?????? api ???????????????
    private FirebaseAuth firebaseAuth;                  // ?????????????????? ?????? ?????? ??????

    private LinearLayout layGoogleLogin;
    private TextView txtMessage;

    private boolean executed;
    private boolean allowed;

    private ArrayList<AccountBook> accountBooks;        // ??????????????? ????????? ????????? ??????
    private HashMap<String, String> applyDates;         // ???????????? ?????????
    private int applyCount;                             // ???????????? ?????? ??? (????????? ????????? + ????????? ?????????)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ?????? ???????????? ?????? ??????
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_intro);

        this.layGoogleLogin = findViewById(R.id.layGoogleLogin);
        this.txtMessage = findViewById(R.id.txtMessage);

        this.layGoogleLogin.setVisibility(View.INVISIBLE);
        this.txtMessage.setVisibility(View.VISIBLE);
        this.txtMessage.setText(R.string.msg_user_authentication_confirm);

        // ?????????????????? ?????? ?????? ??????
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.googleSignInClient = GoogleSignIn.getClient(this,
                Utils.getGoogleSignInOptions(getString(R.string.default_web_client_id)));

        this.executed = true;
        this.allowed = false;

        // ????????? ????????? 1????????? ???????????? ????????????
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            this.executed = false;

            // ????????????
            checkPermission();
        }, Constants.LoadingDelay.LONG);

        // ?????? ?????? button
        this.layGoogleLogin.setOnClickListener(v -> {
            if (this.executed) {
                return;
            }

            if (allowed) {
                this.executed = true;

                if (this.firebaseAuth.getCurrentUser() != null) {
                    // ???????????? ????????? ?????? ??????
                    signOut();
                }

                // ?????? ??????
                Intent intent = this.googleSignInClient.getSignInIntent();
                this.activityLauncher.launch(intent);
            } else {
                Toast.makeText(IntroActivity.this, R.string.permission_rationale_app_use, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (this.executed) {
            return;
        }

        moveTaskToBack(true);
        finish();
    }

    /* ?????? ?????? */
    private void checkPermission() {
        // ?????? ??????
        Dexter.withContext(this)
                .withPermission(Manifest.permission.READ_SMS)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        // ?????? ??????
                        allowed = true;

                        // ?????? ?????? ????????? ?????? ????????? ??????
                        checkGoogleLogin();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        // ?????? ??????
                        /*
                        if (permissionDeniedResponse.isPermanentlyDenied()) {
                            // ????????? ???????????? ??????
                            Toast.makeText(IntroActivity.this, R.string.permission_rationale_app_use, Toast.LENGTH_SHORT).show();
                        }
                        */

                        Toast.makeText(IntroActivity.this, R.string.permission_rationale_app_use, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        // ?????? ????????? ?????? ??????????????? ????????????
                        showPermissionRationale(permissionToken);
                    }
                })
                .withErrorListener(dexterError -> {
                    // ???????????? ??????
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                }).check();
    }

    /* ?????? ????????? ???????????? ??????, ??????????????? ???????????? */
    private void showPermissionRationale(PermissionToken token) {
        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.dialog_allow, (dialog, which) -> {
                    // ?????? ???????????? ??? ???????????? ?????? (onPermissionRationaleShouldBeShown) ???????????? ?????? ?????? ?????? (?????? ?????? ??????)
                    // ?????????????????? ???????????? ?????? ??????????????? ?????????
                    token.continuePermissionRequest();
                })
                .setNegativeButton(R.string.dialog_deny, (dialog, which) -> {
                    // ?????? ?????? ??????
                    token.cancelPermissionRequest();
                })
                .setCancelable(false)
                .setMessage(R.string.permission_rationale_app_use)
                .show();
    }

    /* ?????? ?????? ????????? ?????? ????????? ?????? */
    private void checkGoogleLogin() {
        // ?????? ???????????? ????????? ??????
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // ?????? ?????? ?????? ?????????
            this.layGoogleLogin.setVisibility(View.VISIBLE);
            this.txtMessage.setText("");
        } else {
            this.executed = true;

            successAuth(currentUser);
        }
    }

    /*
     ???????????? ??????????????? ???????????? ?????? GoogleSignInAccount ???????????? ID ????????? ????????????
     Firebase ????????? ?????? ????????? ???????????? Firebase ????????? ?????? ????????? ????????? Firebase ??? ???????????????.
     */
    private void authWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        this.firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // ????????? ??????
                        Log.d(TAG, "??????");

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        successAuth(user);
                    } else {
                        // ????????? ??????
                        Log.d(TAG, "??????");

                        Toast.makeText(this, R.string.msg_google_login_failure, Toast.LENGTH_SHORT).show();
                        this.executed = false;
                    }
                });
    }

    /* ?????? ?????? ?????? */
    private void successAuth(FirebaseUser user) {
        if (user != null) {
            Log.d(TAG, "Uid: " + user.getUid());
            Log.d(TAG, "Email: " + user.getEmail());
            Log.d(TAG, "DisplayName: " + user.getDisplayName());
            Log.d(TAG, "PhoneNumber: " + user.getPhoneNumber());
            Log.d(TAG, "PhotoUrl: " + user.getPhotoUrl());

            // ?????????
            login(user);
        } else {
            Log.d(TAG, "user: ??????");

            Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
            this.executed = false;
        }
    }

    /* ???????????? */
    private void signOut() {
        this.firebaseAuth.signOut();

        // Google sign out
        this.googleSignInClient.signOut().addOnCompleteListener(this,
                task -> {
                    // ???????????? ??????
                    Log.d(TAG, "???????????? ??????");
                });
    }

    /* ????????? */
    private void login(FirebaseUser fireUser) {
        this.layGoogleLogin.setVisibility(View.INVISIBLE);
        this.txtMessage.setText(R.string.msg_user_login_confirm);

        // ?????????????????? db
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // users ??????????????? uid ?????? ??????
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(fireUser.getUid());

        reference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // ??????
                DocumentSnapshot document = task.getResult();
                if (document != null) {
                    // ????????? ??????
                    User user = document.toObject(User.class);
                    if (user != null) {
                        GlobalVariable.user = user;

                        // ???????????? ????????? ????????? ??????
                        checkRegularPayment();
                    } else {
                        // ???????????? ??????
                        join(fireUser);
                    }
                } else {
                    // ??????
                    onError(R.string.msg_error);
                }
            } else {
                // ??????
                onError(R.string.msg_error);
            }
        });
    }

    /* ???????????? */
    private void join(FirebaseUser fireUser) {
        this.txtMessage.setText(R.string.msg_user_join_confirm);

        // ?????????????????? db
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // users ?????????
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.USER);

        // uid ?????? ??????
        Query query = reference.whereEqualTo("uid", fireUser.getUid());
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    if (task.getResult().size() == 0) {
                        // ??????
                        final User user = new User(fireUser.getUid(), fireUser.getEmail(), fireUser.getDisplayName(), fireUser.getPhoneNumber(), System.currentTimeMillis());

                        // ???????????? ?????? (document ID ?????? uid ??? ??????)
                        db.collection(Constants.FirestoreCollectionName.USER)
                                .document(fireUser.getUid())
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    // ??????
                                    GlobalVariable.user = user;

                                    // ???????????? ??????
                                    goMain();
                                })
                                .addOnFailureListener(e -> {
                                    // ??????
                                    onError(R.string.msg_error);
                                });
                    } else {
                        // ??????
                        onError(R.string.msg_google_uid_check_overlap);
                    }
                } else {
                    // ??????
                    onError(R.string.msg_error);
                }
            } else {
                // ??????
                onError(R.string.msg_error);
            }
        });
    }

    /* ???????????? ????????? ????????? ????????? ?????? */
    private void checkRegularPayment() {
        final String currentDate = Utils.getDate("yyyy-MM-dd", System.currentTimeMillis());  // ?????????

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // ???????????? ?????? (???????????? ??????????????? ????????? ?????? ??????)
        Query query = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.REGULAR_PAYMENT)
                .whereLessThanOrEqualTo("applyDate", currentDate);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    this.accountBooks = new ArrayList<>();
                    this.applyDates = new HashMap<>();
                    this.applyCount = 0;

                    // ???????????? ??????
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        // ???????????? ??????
                        RegularPayment regularPayment = document.toObject(RegularPayment.class);
                        // ?????????
                        String applyDate = regularPayment.getApplyDate();
                        while (true) {
                            // ???????????? ??????????????? ????????????
                            if (currentDate.compareTo(applyDate) < 0) {
                                this.applyDates.put(document.getId(), applyDate);
                                break;
                            } else {
                                // ????????? ??????
                                AccountBook accountBook = new AccountBook(Constants.AccountBookKind.EXPENDITURE,
                                        regularPayment.getAssetsKind(), regularPayment.getCategory(),
                                        regularPayment.getName(), regularPayment.getMoney(),
                                        applyDate, Constants.REGULAR_PAYMENT_TIME);

                                this.accountBooks.add(accountBook);

                                // ???????????? ?????? ??????
                                Calendar calendar = Utils.getCalendar("yyyy-MM-dd", applyDate);
                                calendar.add(Calendar.MONTH, 1);
                                applyDate = Utils.getDate("yyyy-MM-dd", calendar.getTimeInMillis());
                            }
                        }
                    }

                    if (this.accountBooks.size() > 0) {
                        // ????????? ??????
                        for (AccountBook accountBook : this.accountBooks) {
                            inputAccountBook(accountBook);
                        }

                        // ????????? ??????
                        for (String key : this.applyDates.keySet()) {
                            modifyApplyDate(key, this.applyDates.get(key));
                        }
                    } else {
                        Log.d(TAG, "RegularPayment none");
                        // ???????????? ??????
                        goMain();
                    }
                }
            } else {
                // ??????
                onError(R.string.msg_error);
            }
        });
    }

    /* ????????? ?????? */
    private void inputAccountBook(final AccountBook accountBook) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // ????????? ??????
        db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.ACCOUNT_BOOK)
                .add(accountBook)
                .addOnSuccessListener(documentReference -> {
                    // ??????
                    this.applyCount++;

                    // ?????? ??????
                    checkCompletion();
                })
                .addOnFailureListener(e -> {
                    // ?????? ??????(??????)
                    this.applyCount++;

                    // ?????? ??????
                    checkCompletion();
                });
    }

    /* ???????????? ????????? ?????? */
    private void modifyApplyDate(String docId, final String applyDate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // ???????????? document ??????
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.user.getUid())
                .collection(Constants.FirestoreCollectionName.REGULAR_PAYMENT)
                .document(docId);
        // ???????????? ????????? ??????
        reference.update("applyDate", applyDate)
                .addOnSuccessListener(aVoid -> {
                    // ??????
                    this.applyCount++;

                    // ?????? ??????
                    checkCompletion();
                })
                .addOnFailureListener(e -> {
                    // ?????? (??????)
                    this.applyCount++;

                    // ?????? ??????
                    checkCompletion();
                });
    }

    /* ???????????? ?????? ?????? ?????? */
    private void checkCompletion() {
        // ????????????
        if (this.applyCount == (this.accountBooks.size() + this.applyDates.size())) {
            // ???????????? ??????
            goMain();
        }
    }

    /* ?????? ?????? */
    private void onError(int res) {
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
        this.executed = false;

        // ?????? ?????? ?????? ?????????
        this.layGoogleLogin.setVisibility(View.VISIBLE);
        this.txtMessage.setText("");
    }

    /* ???????????? ?????? */
    private void goMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        finish();
    }

    /* ???????????? ActivityForResult */
    private final ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data == null) {
                        this.executed = false;
                        Log.d(TAG, "??????");
                        return;
                    }

                    // ??????????????? ?????? ??????
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        // ?????? ????????? ??????
                        Log.d(TAG, "?????? ??????");

                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        authWithGoogle(account);
                    } catch (ApiException e) {
                        Log.d(TAG, "?????? ??????: " + e.toString());

                        Toast.makeText(this, R.string.msg_google_login_failure, Toast.LENGTH_SHORT).show();
                        this.executed = false;
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    // ??????
                    Log.d(TAG, "??????");
                    this.executed = false;
                }
            });
}