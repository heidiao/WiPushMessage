package wistron.pushmessage.client;

import java.util.Arrays;

import org.json.JSONObject;

import com.microsoft.live.LiveAuthClient;
import com.microsoft.live.LiveAuthException;
import com.microsoft.live.LiveAuthListener;
import com.microsoft.live.LiveConnectClient;
import com.microsoft.live.LiveConnectSession;
import com.microsoft.live.LiveOperation;
import com.microsoft.live.LiveOperationException;
import com.microsoft.live.LiveStatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AccountActivity extends Activity {
    public static final String mOAuthAppClientId = "00000000440CABFE";
    public static final String mOAuthAppRedirectUri = "https://oauth.live.com/desktop";
    public static final String[] SCOPES = { "wl.signin", "wl.messenger" };
    private static final int MSG_LOGIN_BEGIN = 1;
    private static final int MSG_GET_ME_INFO = 2;
    private static final int MSG_LOGIN_END = 3;

    // private WebView m_webview;
    PushMessageClientApp mApp;
    private boolean mbNotLogin = true;
    Button mChangeUserBtn;
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
            case MSG_LOGIN_BEGIN:
                new Thread(new Runnable() {
                    public void run() {
                        mApp.getMsnXMPPClient().Login();
                        mHandler.sendEmptyMessage(MSG_GET_ME_INFO);
                    }
                }).start();
                break;
            case MSG_GET_ME_INFO:
                boolean bGotIt = getMeFromLive();
                if (bGotIt) {
                    mHandler.sendEmptyMessage(MSG_LOGIN_END);
                } else {
                    Log.i("Account","Get me retry");
                    mHandler.sendEmptyMessageDelayed(MSG_GET_ME_INFO, 1000);
                }
                break;
            case MSG_LOGIN_END:
                mHandler.post(mRun);
                break;

            }
            super.handleMessage(msg);
        }

    };
    ProgressDialog mInitializeDialog;
    Button mLoginBtn;
    Button mLogoutBtn;
    private Runnable mRun = new Runnable() {
        @Override
        public void run() {
            mUserTextV.setText(mApp.getUser());
            AccountActivity.this.ShowOtherComponents();
            mInitializeDialog.dismiss();
            AccountActivity.this.mApp.postMessageToIM(
                    IMCoreService.MSG_XMPP_CONNECTED, mApp.getUser());
        }
    };
    TextView mUserTextV;

    public void HideOtherComponents() {
        mLoginBtn.setVisibility(View.GONE);
        mLogoutBtn.setVisibility(View.GONE);
        mChangeUserBtn.setVisibility(View.GONE);
        mUserTextV.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(this.toString(), "onBackPressed");
        Intent intent = getIntent();
        AccountActivity.this.setResult(RESULT_OK, intent);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(this.toString(), "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account);
        mApp = (PushMessageClientApp) this.getApplication();

        mInitializeDialog = ProgressDialog.show(this, "",
                "Initializing. Please wait...", true);

        mLoginBtn = (Button) findViewById(R.id.buttonLogin);
        mLoginBtn.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (mApp.HaveNetwork()) {
                    AccountActivity.this.mLoginBtn.setVisibility(View.GONE);
                    ShowLoginUI();
                } else {
                    AlertDialog.Builder ad = new AlertDialog.Builder(
                            AccountActivity.this);
                    ad.setTitle(R.string.network);
                    ad.setMessage(R.string.internet_not_found);
                    ad.setCancelable(false);
                    ad.setPositiveButton(R.string.ok, null);
                    ad.show();
                }
            }
        });
        mLogoutBtn = (Button) findViewById(R.id.buttonLogout);
        mLogoutBtn.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                MsnXMPPClient xmppclient = AccountActivity.this.mApp
                        .getMsnXMPPClient();
                xmppclient.Logout();
                mApp.setMsnXMPPClient(null);
                HideOtherComponents();
                onBackPressed();
            }
        });
        mChangeUserBtn = (Button) findViewById(R.id.buttonChangeUser);
        mChangeUserBtn.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                MsnXMPPClient xmppclient = AccountActivity.this.mApp
                        .getMsnXMPPClient();
                xmppclient.Logout();
                mApp.getAuthClient().logout(null);
                // m_App.setAuthClient(null);
                mApp.setSession(null);
                mApp.setMsnXMPPClient(null);
                mApp.setConnectClient(null);
                HideOtherComponents();
                ShowLoginUI();
            }

        });

        mUserTextV = (TextView) findViewById(R.id.textViewUser);

        LiveAuthClient AuthClient = mApp.getAuthClient();
        if (null == AuthClient) {
            AuthClient = new LiveAuthClient(mApp, mOAuthAppClientId);
            mApp.setAuthClient(AuthClient);

            AuthClient.initialize(Arrays.asList(SCOPES),
                    new LiveAuthListener() {
                        @Override
                        public void onAuthComplete(LiveStatus status,
                                LiveConnectSession session, Object userState) {

                            mInitializeDialog.dismiss();
                            if (status == LiveStatus.CONNECTED) {
                                // launchMainActivity(session);
                            } else {
                            }
                        }

                        @Override
                        public void onAuthError(LiveAuthException exception,
                                Object userState) {
                            mInitializeDialog.dismiss();
                        }

                    });
        } else {
            this.mInitializeDialog.dismiss();
        }

        // 若尚未登入
        /*
         * if (mApp.mstrAccessToken.isEmpty()) { HideOtherComponents(); } else {
         * // 顯示登入的帳號，並且顯示登出按鈕 ShowOtherComponents();
         * mInitializeDialog.dismiss(); }
         */}

    @Override
    protected void onResume() {
        super.onResume();

        MsnXMPPClient client = mApp.getMsnXMPPClient();
        if (null != client)
            mbNotLogin = !client.isAuthenticated();
        else
            mbNotLogin = true;

        this.ShowOtherComponents();
    }

    private boolean getMeFromLive() {
        boolean isGotMe = false;
        try {
            LiveOperation operation;
            operation = mApp.getConnectClient().get("me");
            JSONObject result = operation.getResult();
            String strUser = result.optString("name");
            mbNotLogin = !mApp.getMsnXMPPClient().isAuthenticated();
            mApp.setUser(strUser);
            // mHandler.post(mRun);
            isGotMe = true;
        } catch (LiveOperationException e) {
            e.printStackTrace();
            isGotMe = false;
        }

        return isGotMe;
    }

    public void ShowLoginUI() {
        LiveAuthClient AuthClient = mApp.getAuthClient();
        AuthClient.login(AccountActivity.this, Arrays.asList(SCOPES),
                new LiveAuthListener() {
                    @Override
                    public void onAuthComplete(LiveStatus arg0,
                            LiveConnectSession arg1, Object arg2) {
                        // 授權成功
                        mApp.setSession(arg1);
                        mApp.setConnectClient(new LiveConnectClient(arg1));
                        // 可以連結Messenger
                        mApp.mstrAccessToken = arg1.getAccessToken();
                        mApp.setMsnXMPPClient(new MsnXMPPClient(
                                mApp.mstrAccessToken));
                        mInitializeDialog.show();
                        // new Thread(new Runnable() {
                        // public void run() {
                        // mApp.getMsnXMPPClient().Login();
                        // }
                        // }).start();
                        mHandler.sendEmptyMessage(MSG_LOGIN_BEGIN);
                        ShowOtherComponents();
                    }

                    @Override
                    public void onAuthError(LiveAuthException arg0, Object arg1) {
                        // 授權失敗
                        onBackPressed();
                    }
                });

    }

    public void ShowOtherComponents() {
        if (mbNotLogin) {
            mLoginBtn.setVisibility(View.VISIBLE);
            mLogoutBtn.setVisibility(View.GONE);
            mChangeUserBtn.setVisibility(View.GONE);
            mUserTextV.setVisibility(View.INVISIBLE);
        } else {
            mLoginBtn.setVisibility(View.GONE);
            mLogoutBtn.setVisibility(View.VISIBLE);
            mChangeUserBtn.setVisibility(View.VISIBLE);
            mUserTextV.setVisibility(View.VISIBLE);
            mLogoutBtn.requestFocus();
        }
        mUserTextV.setText(mApp.getUser());
    }

}
