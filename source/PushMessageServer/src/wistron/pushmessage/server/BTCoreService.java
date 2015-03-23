
package wistron.pushmessage.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class BTCoreService extends Service {

    private final String TAG = "BTCoreService";
    private BTConnectThread mServiceThread = null;
    private boolean notify;

    // Message types sent from the BTConnectThread Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_CONNECTED = 4;
    public static final int MESSAGE_DISCONNECT = 5;

    // Key names received from the BTConnectThread Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDR = "device_addr";
    public static final String DEVICE_CONN = "device_conn";
    public static final String TOAST = "toast";

    private String mConnectedDeviceName = null;
    private String mConnectedDeviceAddr = null;
    private SharedPreferences btStore = null;
    private SharedPreferences btStoreList = null;
    
    public static final String BT_STORAGE = "BT_STORAGE";
    public static final String BT_ENABLE = "BT_ENABLE";
    public static final String BT_NOTIFY = "BT_NOTIFY";
    public static final String BT_LIST = "BT_LIST";

    public class LocalBinder extends Binder {
        BTCoreService getService() {
            return BTCoreService.this;
        }
    }

    private LocalBinder mLocBin = new LocalBinder();

    @Override
    public IBinder onBind(Intent arg0) {
        return mLocBin;
    }

    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mServiceThread = new BTConnectThread(this, mHandler);

        btStore = getSharedPreferences(BT_STORAGE, 0);
        btStoreList = getSharedPreferences(BT_LIST, 0);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mServiceThread != null) {
            mServiceThread.stop();
        }

        // clean store
        btStoreList.edit().clear().commit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mServiceThread != null) {
            if (mServiceThread.getState() == BTConnectThread.STATE_NONE) {
                mServiceThread.start();
            }
        }

        
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Notification Manager
     * 
     * @param content show notice information message
     */
    public void setNotification(String content) {
        if (btStore.getBoolean(BT_NOTIFY, true)) {
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
            // int icon = R.drawable.notification_icon;
            CharSequence tickerText = content;
            long when = System.currentTimeMillis();
            Notification notification = new Notification(1, tickerText, when);
            notification.setLatestEventInfo(getApplicationContext(), "Push Message", content, null);
            mNotificationManager.notify(0, notification);
        }
    }

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            // Change
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BTConnectThread.STATE_CONNECTED:
                            Log.d(TAG, "STATE_CONNECTED");
                            break;
                        case BTConnectThread.STATE_CONNECTING:
                            Log.d(TAG, "STATE_CONNECTING");
                            break;
                        case BTConnectThread.STATE_NONE:
                        case BTConnectThread.STATE_LISTEN:
                            break;
                    }
                    break;
                // Write
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    Log.d(TAG, "writeMessage:" + writeMessage);
                    break;

                // Read
                case MESSAGE_READ:
                    notify = btStore.getBoolean(BT_NOTIFY, false);
                    if (notify) {
                        byte[] readBuf = (byte[]) msg.obj;
                        String readMessage = new String(readBuf, 0, msg.arg1);

                        // Show notification and toast message
                        if (readMessage.length() > 0) {
                            setNotification(readMessage);
                            Log.d(TAG, "readMessage:" + readMessage);
                        }
                    }
                    break;
                // Connected
                case MESSAGE_CONNECTED:
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    mConnectedDeviceAddr = msg.getData().getString(DEVICE_ADDR);
                    notify = btStore.getBoolean(BT_NOTIFY, false);
                    if (notify) {
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.toast_connected) + " " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "add=" + mConnectedDeviceAddr);
                    btStoreList.edit().putString(mConnectedDeviceAddr, mConnectedDeviceName).commit();
                    break;
                // Disconnect
                case MESSAGE_DISCONNECT:
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    mConnectedDeviceAddr = msg.getData().getString(DEVICE_ADDR);
                    notify = btStore.getBoolean(BT_NOTIFY, false);
                    if (notify) {
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.toast_disconnect)  + " " + mConnectedDeviceName , Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "remove=" + mConnectedDeviceAddr);
                    btStoreList.edit().remove(mConnectedDeviceAddr).commit();
                    break;
            }
        }
    };

}
