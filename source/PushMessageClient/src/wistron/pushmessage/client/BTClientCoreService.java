package wistron.pushmessage.client;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class BTClientCoreService extends Service implements IBTPostMessage {
    public class MyBinder extends Binder {
        BTClientCoreService getService() {
            return BTClientCoreService.this;
        }
    }

    public static final String TOAST = "toast";
    public static final String DEVICE_NAME = "device_name";
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_FAIL = 5;
    public static final int MESSAGE_READ = 2;
    // Message types sent from the BTConnectThread Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_WRITE = 3;
    public static final int RECONNECT = 6;

    private MyBinder mBinder = new MyBinder();
    private BTConnectThread mBtService = null;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            if (null != mExternHandler)
                mExternHandler.dispatchMessage(msg);
            switch (msg.what) {

            }
            super.handleMessage(msg);
        }

    };
    private Handler mExternHandler = null;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        mBtService = new BTConnectThread(null, mHandler);
        PushMessageClientApp app = (PushMessageClientApp) getApplication();
        app.setBTPM(this);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if (null != mBtService) {
            mBtService.stop();
            mBtService = null;
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        // TODO Auto-generated method stub
        super.onLowMemory();
    }

    @Override
    public void onRebind(Intent intent) {
        // TODO Auto-generated method stub
        super.onRebind(intent);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO Auto-generated method stub
        return super.onUnbind(intent);
    }

    public BTConnectThread getmBtService() {
        return mBtService;
    }

    public void setExternHandle(Handler ExternHandler) {
        // TODO Auto-generated method stub
        mExternHandler = ExternHandler;
    }

    @Override
    public boolean postMessage(int doWhat, Object arg1) {
        // TODO Auto-generated method stub
        Message msg = this.mHandler.obtainMessage(doWhat, arg1);
        return this.mHandler.sendMessage(msg);
    }

    @Override
    public void sendMessage(String to, String from) {
        // TODO Auto-generated method stub
        String message = from + " " + getResources().getString(R.string.toast_send)
                + " " + to;
        byte[] send = message.getBytes();
        mBtService.write(send);
    }

    @Override
    public void sendLogin(String user) {
        // TODO Auto-generated method stub
        String message = user + " " + getResources().getString(R.string.toast_login);
        byte[] send = message.getBytes();
        mBtService.write(send);
    }

    @Override
    public void sendLogout(String user) {
        // TODO Auto-generated method stub
        String message = user + " " + getResources().getString(R.string.toast_logout);
        byte[] send = message.getBytes();
        mBtService.write(send);
    }

}
