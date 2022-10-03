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
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.project.pigbook.entity.User;
import com.project.pigbook.util.Constants;
import com.project.pigbook.util.GlobalVariable;
import com.project.pigbook.util.Utils;

public class IntroActivity extends AppCompatActivity {
    //private static final String TAG = IntroActivity.class.getSimpleName();
    private static final String TAG = "PigBook";

    private GoogleSignInClient googleSignInClient;      // 구글 api 클라이언트
    private FirebaseAuth firebaseAuth;                  // 파이어베이스 인증 객체 생성

    private LinearLayout layGoogleLogin;
    private TextView txtMessage;

    private boolean executed;
    private boolean allowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 툴바 안보이게 하기 위함
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_intro);

        this.layGoogleLogin = findViewById(R.id.layGoogleLogin);
        this.txtMessage = findViewById(R.id.txtMessage);

        this.layGoogleLogin.setVisibility(View.INVISIBLE);
        this.txtMessage.setVisibility(View.VISIBLE);
        this.txtMessage.setText(R.string.msg_user_authentication_confirm);

        // 파이어베이스 인증 객체 선언
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.googleSignInClient = GoogleSignIn.getClient(this,
                Utils.getGoogleSignInOptions(getString(R.string.default_web_client_id)));

        this.executed = true;
        this.allowed = false;

        // 인트로 화면을 1초동안 보여주고 권한체크
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            this.executed = false;

            // 권한체크
            checkPermission();
        }, Constants.LoadingDelay.LONG);

        // 구글 연동 button
        this.layGoogleLogin.setOnClickListener(v -> {
            if (this.executed) {
                return;
            }

            if (allowed) {
                this.executed = true;

                if (this.firebaseAuth.getCurrentUser() != null) {
                    // 연동되어 있으면 연동 끊기
                    signOut();
                }

                // 구글 연동
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

    /* 권한 체크 */
    private void checkPermission() {
        // 권한 체크
        Dexter.withContext(this)
                .withPermission(Manifest.permission.READ_SMS)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        // 권한 허용
                        allowed = true;

                        // 현재 구글 연동이 되어 있는지 확인
                        checkGoogleLogin();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        // 권한 거부
                        /*
                        if (permissionDeniedResponse.isPermanentlyDenied()) {
                            // 완전히 거부했을 경우
                            Toast.makeText(IntroActivity.this, R.string.permission_rationale_app_use, Toast.LENGTH_SHORT).show();
                        }
                        */

                        Toast.makeText(IntroActivity.this, R.string.permission_rationale_app_use, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        // 권한 거부시 설정 다이얼로그 보여주기
                        showPermissionRationale(permissionToken);
                    }
                })
                .withErrorListener(dexterError -> {
                    // 권한설정 오류
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                }).check();
    }

    /* 만약 권한을 거절했을 경우, 다이얼로그 보여주기 */
    private void showPermissionRationale(PermissionToken token) {
        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.dialog_allow, (dialog, which) -> {
                    // 다시 권한요청 후 거부했을 경우 (onPermissionRationaleShouldBeShown) 메서드가 다시 실행 안됨 (권한 설정 못함)
                    // 어플리케이션 설정에서 직접 권한설정을 해야함
                    token.continuePermissionRequest();
                })
                .setNegativeButton(R.string.dialog_deny, (dialog, which) -> {
                    // 권한 요청 취소
                    token.cancelPermissionRequest();
                })
                .setCancelable(false)
                .setMessage(R.string.permission_rationale_app_use)
                .show();
    }

    /* 현재 구글 연동이 되어 있는지 확인 */
    private void checkGoogleLogin() {
        // 현재 연동되어 있는지 확인
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // 구글 연동 버튼 활성화
            this.layGoogleLogin.setVisibility(View.VISIBLE);
            this.txtMessage.setText("");
        } else {
            this.executed = true;

            successAuth(currentUser);
        }
    }

    /*
     사용자가 정상적으로 로그인한 후에 GoogleSignInAccount 개체에서 ID 토큰을 가져와서
     Firebase 사용자 인증 정보로 교환하고 Firebase 사용자 인증 정보를 사용해 Firebase 에 인증합니다.
     */
    private void authWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        this.firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 로그인 성공
                        Log.d(TAG, "성공");

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        successAuth(user);
                    } else {
                        // 로그인 실패
                        Log.d(TAG, "실패");

                        Toast.makeText(this, R.string.msg_google_login_failure, Toast.LENGTH_SHORT).show();
                        this.executed = false;
                    }
                });
    }

    /* 구글 인증 성공 */
    private void successAuth(FirebaseUser user) {
        if (user != null) {
            Log.d(TAG, "Uid: " + user.getUid());
            Log.d(TAG, "Email: " + user.getEmail());
            Log.d(TAG, "DisplayName: " + user.getDisplayName());
            Log.d(TAG, "PhoneNumber: " + user.getPhoneNumber());
            Log.d(TAG, "PhotoUrl: " + user.getPhotoUrl());

            // 로그인
            login(user);
        } else {
            Log.d(TAG, "user: 없음");

            Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
            this.executed = false;
        }
    }

    /* 연동끊기 */
    private void signOut() {
        this.firebaseAuth.signOut();

        // Google sign out
        this.googleSignInClient.signOut().addOnCompleteListener(this,
                task -> {
                    // 연동끊기 성공
                    Log.d(TAG, "연동끊기 성공");
                });
    }

    /* 로그인 */
    private void login(FirebaseUser fireUser) {
        this.layGoogleLogin.setVisibility(View.INVISIBLE);
        this.txtMessage.setText(R.string.msg_user_login_confirm);

        // 파이어스토어 db
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // users 컬렉션에서 uid 문서 확인
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(fireUser.getUid());

        reference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 성공
                DocumentSnapshot document = task.getResult();
                if (document != null) {
                    // 사용자 객체
                    User user = document.toObject(User.class);
                    if (user != null) {
                        GlobalVariable.user = user;

                        // 메인으로 이동
                        goMain();
                    } else {
                        // 회원가입 하기
                        join(fireUser);
                    }
                } else {
                    // 오류
                    onError(R.string.msg_error);
                }
            } else {
                // 오류
                onError(R.string.msg_error);
            }
        });
    }

    /* 회원가입 */
    private void join(FirebaseUser fireUser) {
        this.txtMessage.setText(R.string.msg_user_join_confirm);

        // 파이어스토어 db
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // users 컬렉션
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.USER);

        // uid 중복 체크
        Query query = reference.whereEqualTo("uid", fireUser.getUid());
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    if (task.getResult().size() == 0) {
                        // 가입
                        final User user = new User(fireUser.getUid(), fireUser.getEmail(), fireUser.getDisplayName(), fireUser.getPhoneNumber(), System.currentTimeMillis());

                        // 회원가입 하기 (document ID 값을 uid 로 설정)
                        db.collection(Constants.FirestoreCollectionName.USER)
                                .document(fireUser.getUid())
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    // 성공
                                    GlobalVariable.user = user;

                                    // 메인으로 이동
                                    goMain();
                                })
                                .addOnFailureListener(e -> {
                                    // 실패
                                    onError(R.string.msg_error);
                                });
                    } else {
                        // 중복
                        onError(R.string.msg_google_uid_check_overlap);
                    }
                } else {
                    // 오류
                    onError(R.string.msg_error);
                }
            } else {
                // 오류
                onError(R.string.msg_error);
            }
        });
    }

    /* 오류 확인 */
    private void onError(int res) {
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
        this.executed = false;

        // 구글 연동 버튼 활성화
        this.layGoogleLogin.setVisibility(View.VISIBLE);
        this.txtMessage.setText("");
    }

    /* 메인으로 이동 */
    private void goMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        finish();
    }

    /* 구글인증 ActivityForResult */
    private final ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data == null) {
                        this.executed = false;
                        Log.d(TAG, "오류");
                        return;
                    }

                    // 구글로그인 버튼 응답
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        // 구글 로그인 성공
                        Log.d(TAG, "연동 성공");

                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        authWithGoogle(account);
                    } catch (ApiException e) {
                        Log.d(TAG, "연동 실패: " + e.toString());

                        Toast.makeText(this, R.string.msg_google_login_failure, Toast.LENGTH_SHORT).show();
                        this.executed = false;
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    // 취소
                    Log.d(TAG, "취소");
                    this.executed = false;
                }
            });
}