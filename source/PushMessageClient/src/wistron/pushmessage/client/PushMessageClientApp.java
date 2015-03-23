package wistron.pushmessage.client;

import com.microsoft.live.LiveAuthClient;
import com.microsoft.live.LiveConnectClient;
import com.microsoft.live.LiveConnectSession;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;

public class PushMessageClientApp extends Application {

    private MsnXMPPClient mMsnXMPPClient;
    private LiveAuthClient mAuthClient;
    private LiveConnectClient mConnectClient;
    private LiveConnectSession mSession;
    public String mstrAccessToken;
    // public String mstrUser = new String("");
    private Handler mHandler;
    public boolean mbAutoConnect = true;
    public boolean mbEnable = true;
    private IPostMessage mIMPM = null;
    private IBTPostMessage mBTPM = null;

    public MsnXMPPClient getMsnXMPPClient() {
        return mMsnXMPPClient;
    }

    public void setMsnXMPPClient(MsnXMPPClient client) {
        if (null != mMsnXMPPClient) {
            mMsnXMPPClient.setHandler(null);
        }
        mMsnXMPPClient = client;
        if (null != mMsnXMPPClient) {
            mMsnXMPPClient.setHandler(mHandler);
        }
    }

    public LiveAuthClient getAuthClient() {
        return mAuthClient;
    }

    public void setAuthClient(LiveAuthClient authClient) {
        mAuthClient = authClient;
    }

    public LiveConnectClient getConnectClient() {
        return mConnectClient;
    }

    public void setConnectClient(LiveConnectClient connectClient) {
        mConnectClient = connectClient;
    }

    public LiveConnectSession getSession() {
        return mSession;
    }

    public void setSession(LiveConnectSession session) {
        this.mSession = session;
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        mstrAccessToken = new String();
    }

    @Override
    public void onTerminate() {
        // TODO Auto-generated method stub
        super.onTerminate();
    }

    void postMessageToIM(int doWhat, Object arg1) {
        synchronized (this) {
            if (null == mIMPM)
                return;
            this.mIMPM.postMessage(doWhat, arg1);
        }
    }

    void postMessageToBT(int doWhat, Object arg1) {
        synchronized (this) {
            if (null == mBTPM)
                return;
            this.mBTPM.postMessage(doWhat, arg1);
        }
    }

    public void setIMPM(IPostMessage PM) {
        synchronized (this) {
            this.mIMPM = PM;
        }
    }

    public void setBTPM(IBTPostMessage PM) {
        synchronized (this) {
            this.mBTPM = PM;
        }
    }

    public IBTPostMessage getBTIM() {
        // TODO Auto-generated method stub
        return this.mBTPM;
    }

    public void setUser(String strUser) {
        // TODO Auto-generated method stub
        this.mMsnXMPPClient.setUser(strUser);
    }

    public String getUser() {
        // TODO Auto-generated method stub
        if (null == this.mMsnXMPPClient) {
            return "";
        } else {
            return this.mMsnXMPPClient.getUser();
        }

    }
    
    public boolean HaveNetwork(){
        final ConnectivityManager connMgr = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null != connMgr){
            final android.net.NetworkInfo activenetworkinfo = connMgr.getActiveNetworkInfo();
            
            if (null != activenetworkinfo)
            {
                if (activenetworkinfo.isConnectedOrConnecting())
                {
                    return true;
                }
            }
        }
        return false;
    }

}
