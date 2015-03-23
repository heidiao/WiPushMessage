package wistron.pushmessage.client;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class IMCoreService extends Service implements IPostMessage {
    public class MyBinder extends Binder {
        IMCoreService getService() {
            return IMCoreService.this;
        }
    }
    public static final int MSG_BT_CONNECTED = 0x00000004;
    public static final int MSG_BT_DISCONNECTED = 0x00000005;
    public static final int MSG_XMPP_CONNECTED = 0x00000001;
    public static final int MSG_XMPP_DISCONNECTED = 0x00000002;
    public static final int MSG_XMPP_GOT_MESSAGE = 0x00000003;
    private MyBinder mBinder = new MyBinder();

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Toast toast = null;
            switch (msg.what) {
            case MSG_XMPP_CONNECTED:
//                toast = Toast.makeText(IMCoreService.this.getApplication(),
//                        "Connected", Toast.LENGTH_SHORT);
                String user = (String)msg.obj;
                // ¶Ç°eLogin¸ê°T
                synchronized(getMyApp().getBTIM()){
                    IBTPostMessage bt = getMyApp().getBTIM();
                    if(null != bt)
                        bt.sendLogin(getMyApp().getUser());
                }
                break;
            case MSG_XMPP_DISCONNECTED:
                String Status = (String) msg.obj;
                toast = Toast.makeText(IMCoreService.this.getApplication(),
                        "Disconnected:" + Status, Toast.LENGTH_SHORT);
                String User = msg.getData().getString("user");
                synchronized(getMyApp().getBTIM()){
                    IBTPostMessage bt = getMyApp().getBTIM();
                    if(null != bt)
                        bt.sendLogout(User);
                }
                break;
            case MSG_XMPP_GOT_MESSAGE:
                String Name = (String) msg.obj;
                toast = Toast.makeText(IMCoreService.this.getApplication(),
                        "Got Message: " + Name, Toast.LENGTH_SHORT);
                synchronized(getMyApp().getBTIM()){
                    IBTPostMessage bt = getMyApp().getBTIM();
                    if(null != bt)
                        bt.sendMessage(getMyApp().getUser() , Name);
                }
                
                break;
            case MSG_BT_CONNECTED:
                synchronized(getMyApp().getBTIM()){
                    IBTPostMessage bt = getMyApp().getBTIM();
                    if(null != bt)
                    {
                        MsnXMPPClient xmpp = getMyApp().getMsnXMPPClient();
                        if(null != xmpp){
                            if (xmpp.isAuthenticated()){
                                bt.sendLogin(getMyApp().getUser());
                            }
                        }
                    }
                }
                break;

            case MSG_BT_DISCONNECTED:
                break;
            default:
                toast = Toast.makeText(IMCoreService.this.getApplication(),
                        "Undefine", Toast.LENGTH_SHORT);
                break;
            }
            if (null != toast)
                toast.show();
            super.handleMessage(msg);
        }

    };

    private Runnable mRun = new Runnable() {
        @Override
        public void run() {
            // Log.d("IMCoreService","run");
            IMCoreService.this.mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i("IMCoreService", "onBind");
        return mBinder;
    }

    protected PushMessageClientApp getMyApp() {
        // TODO Auto-generated method stub
        return (PushMessageClientApp) getApplication();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("IMCoreService", "onCreate");
        PushMessageClientApp MyApp = getMyApp();
        MyApp.setHandler(mHandler);
        MyApp.setIMPM(this);
    }

    @Override
    public void onDestroy() {
        Log.i("IMCoreService", "onDestroy");
        mHandler.removeCallbacks(mRun);
        super.onDestroy();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i("IMCoreService", "onRebind");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("IMCoreService", "OnStartCommand");

        mHandler.removeCallbacks(mRun);
        mHandler.postDelayed(mRun, 1000);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("IMCoreService", "onUnbind");
        return super.onUnbind(intent);
    }

    public boolean postMessage(int doWhat , Object arg1){
        Message msg = this.mHandler.obtainMessage(doWhat, arg1);
        return this.mHandler.sendMessage(msg);
    }
}
