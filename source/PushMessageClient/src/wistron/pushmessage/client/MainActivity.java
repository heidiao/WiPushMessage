package wistron.pushmessage.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

public class MainActivity extends Activity {
    /**
     * If the connection interrupted because of click Alert disconnect button ,
     * stop bluetooth thread and reset layout.
     */
    private class AlertCancel implements IDisconnect {
        /**
         * Component property
         */
        IComponent comp;

        public void disconnect() {
            // Stop bluetooth thread
            if (null != mBtService)
                mBtService.stop();

            // Reset layout
            comp.action();
        }
    }

    /**
     * Display connected layout
     */
    private class Connected implements IComponent {
        public void action() {
            mAccountBtn.setEnabled(true);
            mConnectBtn.setEnabled(true);
            mEnableCB.setEnabled(true);
            mConnectBtn.setText(R.string.button_disconnect);
            mTxt.setText(R.string.label_connected_to);
            // send message to imcoreservice
            MainActivity.this.mApp.postMessageToIM(
                    IMCoreService.MSG_BT_CONNECTED, null);
        }
    }

    /**
     * If the connection is interrupted or can not connect, check the
     * BT_AUTO_CONN preference to retry connect.
     */
    private class ConnectFail implements IDisconnect {
        /**
         * IComponent property
         */
        IComponent comp;

        /**
         * IToast property
         */
        IToast toast;

        public void disconnect() {
            // Stop bluetooth thread
            if (null != mBtService){
                mBtService.stop();

                // Reconnect
                if (mStore.getInt(BT_STATUS, 0) == BT_CONNECTED
                        && mStore.getBoolean(BT_AUTO_CONN, false) == true) {
                    mBtService.retry();
                }
            }
            // Reset layout
            comp.action();

            // Show disconnect message
            toast.show();
        }
    }

    /**
     * Display connecting layout
     */
    private class Connecting implements IComponent {
        public void action() {
            mAccountBtn.setEnabled(false);
            mEnableCB.setEnabled(false);
            mConnectBtn.setEnabled(true);
            mTxt.setText(R.string.label_connecting);
        }
    }

    /**
     * Display default layout
     */
    private class Default implements IComponent {
        public void action() {
            mAccountBtn.setEnabled(true);
            if (MainActivity.this.mApp.mbEnable) {
                mConnectBtn.setEnabled(true);
            } else {
                mConnectBtn.setEnabled(false);
            }
            mEnableCB.setEnabled(true);
            mConnectBtn.setText(R.string.button_connect);
            mTxt.setText("");
        }
    }

    /**
     * Interface component
     */
    private interface IComponent {
        void action();
    }

    /**
     * Interface disconnect
     */
    private interface IDisconnect {
        void disconnect();
    }

    /**
     * Interface toast message
     */
    private interface IToast {
        void show();
    }

    /**
     * Show lost connection
     */
    private class ToastLose implements IToast {
        public void show() {
            Toast.makeText(getApplicationContext(),
                    R.string.toast_lost_connect, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show unable connection
     */
    private class ToastUnable implements IToast {
        public void show() {
            Toast.makeText(getApplicationContext(),
                    R.string.toast_unable_connect, Toast.LENGTH_SHORT).show();
        }
    }

    private static final String BT_ADDR = "BT_ADDR";

    private static final String BT_AUTO_CONN = "BT_AUTO_CONN";
    private static final String BT_ENABLE = "BT_ENABLE";

    private static final int BT_CONNECTED = 1;

    private static final int BT_NONE = 0;
    private static final String BT_STATUS = "BT_STATUS";
    // Constants that indicate BT connection state (from SharedPreferences)
    private static final String BT_STORAGE = "BT_STORAGE";
    private static final boolean D = true;
    // Key names received from the BTConnectThread Handler
    // public static final String DEVICE_NAME = "device_name";
    // public static final int MESSAGE_DEVICE_NAME = 4;
    // public static final int MESSAGE_FAIL = 5;
    // public static final int MESSAGE_READ = 2;
    // // Message types sent from the BTConnectThread Handler
    // public static final int MESSAGE_STATE_CHANGE = 1;
    // public static final int MESSAGE_WRITE = 3;
    // public static final int RECONNECT = 6;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Debugging
    private static final String TAG = "BluetoothClient";
    // public static final String TOAST = "toast";
    private static final String UUIDs = "00001101-0000-1000-8000-00805F9B34fb";

    private String mCurrentAddr = null;

    private Button mAccountBtn;
    private Button mConnectBtn;
    private CheckBox mAutoConnectCB;
    private CheckBox mEnableCB;
    private TextView mTxt;

    private PushMessageClientApp mApp = null;

    // BLUETOOTH
    private BluetoothAdapter mBluetoothAdapter = null;

    private BTConnectThread mBtService = null;

    private BTClientCoreService mBTClientCoreService = null;

    private Button.OnClickListener mAccountbtnListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            // show Account UI
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, AccountActivity.class);
            startActivityForResult(intent, 0);
            // MainActivity.this.finish();
        }
    };

    /**
     * Click Button - Connect device Launch the DeviceListActivity to see
     * devices and do scan
     */
    private Button.OnClickListener mConnectBtnListener = new Button.OnClickListener() {
        public void onClick(View v) {
            if (null == mBtService)
                return;
            // Connect to device , launch the device list
            if (mBtService.getState() == BTConnectThread.STATE_NONE) {
                Intent serverIntent = new Intent(getApplicationContext(),
                        DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);

                // Disconnect device, launch confirm dialg
            } else if (mBtService.getState() == BTConnectThread.STATE_CONNECTED) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.button_disconnect)
                        .setMessage(R.string.msg_confirm_disconnect)
                        .setPositiveButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        if (D)
                                            Log.d(TAG, "click disconnect");
                                        AlertCancel fail = new AlertCancel();
                                        fail.comp = new Default();
                                        fail.disconnect();
                                        dialog.dismiss();
                                        mStore.edit()
                                                .putInt(BT_STATUS, BT_NONE)
                                                .commit();
                                    }
                                })
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        if (D)
                                            Log.d(TAG, "click cancel");
                                        dialog.dismiss();
                                    }
                                }).show();
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener mAutoConnectCBListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            // TODO Auto-generated method stub
            if (null != mStore) {
                SharedPreferences.Editor editor = mStore.edit();
                editor.putBoolean(BT_AUTO_CONN, isChecked);
                boolean bResult = editor.commit();
                if (bResult) {
                    mApp.mbAutoConnect = isChecked;
                }
            }
        }

    };

    CompoundButton.OnCheckedChangeListener mEnableCBListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            // TODO Auto-generated method stub
            if (null != mStore) {
                SharedPreferences.Editor editor = mStore.edit();
                editor.putBoolean(BT_ENABLE, isChecked);
                boolean bResult = editor.commit();
                if (bResult) {
                    mApp.mbEnable = isChecked;
                }
            }

            if (isChecked) {
                MainActivity.this.mConnectBtn.setEnabled(true);
                startBTClientCoreService();
            } else {
                stopBTClientCoreService();
                MainActivity.this.mConnectBtn.setEnabled(false);
            }
        }

    };

    /**
     * The Handler that gets information back from the BTConnectThread
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            // Status change...
            case BTClientCoreService.MESSAGE_STATE_CHANGE:
                IComponent mComponent = null;
                switch (msg.arg1) {

                // Connected
                case BTConnectThread.STATE_CONNECTED:
                    if (D)
                        Log.d(TAG, "connect success!");
                    mComponent = new Connected();

                    // Save address and status into preference
                    mStore.edit().putString(BT_ADDR, mCurrentAddr)
                            .putInt(BT_STATUS, BT_CONNECTED).commit();
                    break;

                // Connecting
                case BTConnectThread.STATE_CONNECTING:
                    if (D)
                        Log.d(TAG, "connecting...");
                    mComponent = new Connecting();
                    break;

                // None
                case BTConnectThread.STATE_NONE:
                    if (D)
                        Log.d(TAG, "reset");
                    mComponent = new Default();
                    break;
                }
                mComponent.action();
                break;

            // Get device name
            case BTClientCoreService.MESSAGE_DEVICE_NAME:
                String deviceName = msg.getData().getString(
                        BTClientCoreService.DEVICE_NAME);
                mTxt.append(deviceName);
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.label_connected_to) + " " + deviceName,
                        Toast.LENGTH_SHORT).show();
                if (D)
                    Log.d(TAG, "after success, get device name:" + deviceName);
                break;

            // Failure
            case BTClientCoreService.MESSAGE_FAIL:
                if (D)
                    Log.d(TAG, "connect fail!");

                // Reset layout and check retry statement
                ConnectFail fail = new ConnectFail();
                fail.comp = new Default();
                fail.toast = new ToastLose();
                fail.disconnect();
                break;

            // Reconnection
            case BTClientCoreService.RECONNECT:
                if (D)
                    Log.d(TAG, "reconnect");
                setBluetoothConnectRemote(mStore.getString(
                        MainActivity.BT_ADDR, null));
                break;
            }
        }
    };

    private IMCoreService mIMCoreService = null;

    private ServiceConnection mIMCoreServiceSC = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("mIMCoreServiceSC", "onServiceConnected");
            mIMCoreService = ((IMCoreService.MyBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            Log.i("mIMCoreServiceSC", "onServiceDisconnected");

        }
    };

    private ServiceConnection mBTClientCoreServiceSC = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("mBTClientCoreServiceSC", "onServiceConnected");

            mBTClientCoreService = ((BTClientCoreService.MyBinder) service)
                    .getService();
            mBTClientCoreService.setExternHandle(MainActivity.this.mHandler);
            mBtService = mBTClientCoreService.getmBtService();

            // recover ui status
            if (null != mBtService) {
                switch (mBtService.getState()) {
                case BTConnectThread.STATE_CONNECTED:
                    // set up ui
                    mAccountBtn.setEnabled(true);
                    mConnectBtn.setEnabled(true);
                    mEnableCB.setEnabled(true);
                    mConnectBtn.setText(R.string.button_disconnect);
                    mTxt.setText(R.string.label_connected_to);
                    mTxt.append(mBtService.getConnectedDeviceName());
//                    MainActivity.this.mHandler.obtainMessage(
//                            BTClientCoreService.MESSAGE_STATE_CHANGE,
//                            BTConnectThread.STATE_CONNECTED,-1).sendToTarget();
//                    Message msg = MainActivity.this.mHandler.obtainMessage(BTClientCoreService.MESSAGE_DEVICE_NAME);
//                    Bundle bundle = new Bundle();
//                    bundle.putString(BTClientCoreService.DEVICE_NAME, mBtService
//                            .getConnectedDeviceName());
//                    msg.setData(bundle);
//                    msg.sendToTarget();
                    break;
                }
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            Log.i("mBTClientCoreServiceSC", "onServiceDisconnected");

        }
    };

    private SharedPreferences mStore = null;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Activity.RESULT_CANCELED == resultCode) {
            return;
        } else if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, R.string.toast_bt_not_enabled,
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        switch (requestCode) {
        // When DeviceListActivity returns with a device to connect
        case REQUEST_CONNECT_DEVICE:
            String address = data.getExtras().getString(
                    DeviceListActivity.EXTRA_DEVICE_ADDRESS);

            // Connect to remote device(BOX)
            setBluetoothConnectRemote(address);
            break;

        // When the request to enable Bluetooth returns
        case REQUEST_ENABLE_BT:
            setInitComponent();
            break;
        }
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        // Intent intent = getIntent();
        // MainActivity.this.setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("Main", "onCreate");
        setContentView(R.layout.main);
        mApp = (PushMessageClientApp) this.getApplication();

        loadSharePreferences();

        loadComponents();

        initializeService();

        mStore.edit().putInt(BT_STATUS, BT_NONE).commit();

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.toast_bt_not_available,
                    Toast.LENGTH_LONG).show();
            // finish();
            return;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        menu.addSubMenu(0, 0, 0, R.string.app_about);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        boolean bResult = super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
        case 0:
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_about)
                    .setPositiveButton(R.string.str_ok,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    // TODO Auto-generated method stub

                                }
                            }).show();

            break;
        }
        return bResult;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("Main", "onResume");

        // if (null != mStore) {
        // mApp.mbAutoConnect = mStore.getBoolean(BT_AUTO_CONN, true);
        // }

        // this.mAutoConnectCB.setChecked(mApp.mbAutoConnect);
        // this.mEnableCB.setChecked(mApp.mbEnable);

    }

    private void startIMCoreService() {
        // if no service , then start service
        Intent intentForBind = new Intent();
        intentForBind.setClass(MainActivity.this, IMCoreService.class);
        boolean bBind = bindService(intentForBind, mIMCoreServiceSC, 0);
        if (true == bBind) {
            Log.i("Main", "Bind Service Completed!");
        } else {
            Log.i("Main", "Bind Service Failed!");
        }

        if (null == mIMCoreService) {
            Log.i("Main", "No IMCoreService");
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, IMCoreService.class);
            startService(intent);
        } else {
            Log.i("Main", "Yes IMCoreService");
            unbindService(mIMCoreServiceSC);
            mIMCoreService = null;
        }
    }

    private void startBTClientCoreService() {
        // TODO Auto-generated method stub
        Intent intentForBind = new Intent();
        intentForBind.setClass(MainActivity.this, BTClientCoreService.class);
        boolean bBind = bindService(intentForBind, mBTClientCoreServiceSC, 0);
        if (true == bBind) {
            Log.i("Main", "Bind BTClientCoreService Completed!");
        } else {
            Log.i("Main", "Bind BTClientCoreService Failed!");
        }

        if (null == mBTClientCoreService) {
            Log.i("Main", "No BTClientCoreService");
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, BTClientCoreService.class);
            startService(intent);
        } else {
            Log.i("Main", "Yes BTClientCoreService");
            unbindService(mIMCoreServiceSC);
            // mBTClientCoreService = null;
        }

    }

    private void stopBTClientCoreService() {
        // TODO Auto-generated method stub
        Intent intent = new Intent(MainActivity.this, BTClientCoreService.class);
        stopService(intent);
        this.mBTClientCoreService = null;
        this.mBtService = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i("Main", "onStart");

        // If BT is not on, request that it be enabled.
        // setInitComponent() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

            // Otherwise, setup the chat session
        } else {
            if (mBtService == null)
                setInitComponent();
        }
        
        if (false == mApp.HaveNetwork())
        {
            AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
            ad.setTitle(R.string.network);
            ad.setMessage(R.string.Start_Wifi);
            ad.setCancelable(false);
            ad.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            });
            ad.setNegativeButton(R.string.cancel, null);

            ad.show();
        }
    }

    /**
     * Connect to remote device(BOX)
     * 
     * @param address
     *            Bluetooth's address
     */
    public void setBluetoothConnectRemote(String address) {
        if (D)
            Log.d(TAG, "connect to " + address);

        // save temp address
        mCurrentAddr = address;

        try {
            // connect to remote device (call BTConnectThread class)
            mBtService.connect(mBluetoothAdapter.getRemoteDevice(address),
                    UUID.fromString(UUIDs));

        } catch (RuntimeException e) { // NullPointerException | SocketException
            if (D)
                Log.d(TAG, "connect fail(RuntimeException)!");

            // When connection is failure then reset layout and show some
            // message.
            ConnectFail conn = new ConnectFail();
            conn.comp = new Default();
            conn.toast = new ToastUnable();
            conn.disconnect();
        }
    }

    /**
     * Setup UI VIEW and initialize BLUETOOTH service
     */
    private void setInitComponent() {
        // mConnectBtn = (Button) findViewById(R.id.connect_device);
        // mTxt = (TextView) findViewById(R.id.information);

        // mConnectBtn.setOnClickListener(mConnectBtnListener);

        // Initialize the BTConnectThread to perform bluetooth connections
        // mBtService = new BTConnectThread(this, mHandler);

        // When start up application, check BT_STATUS and auto connect bluetooth
        // device(BOX)
        if (mStore.getInt(BT_STATUS, 0) == BT_CONNECTED) {
            setBluetoothConnectRemote(mStore.getString(BT_ADDR, null));
        }
    }

    private void loadSharePreferences() {
        mStore = getSharedPreferences(BT_STORAGE, MODE_PRIVATE);
        this.mApp.mbAutoConnect = mStore.getBoolean(BT_AUTO_CONN, true);
        this.mApp.mbEnable = mStore.getBoolean(BT_ENABLE, true);
    }

    private void loadComponents() {
        // find out components
        mAccountBtn = (Button) findViewById(R.id.buttonAccount);
        mAutoConnectCB = (CheckBox) findViewById(R.id.checkBoxAutoConnect);
        mEnableCB = (CheckBox) findViewById(R.id.checkBoxEnable);
        mConnectBtn = (Button) findViewById(R.id.connect_device);
        mTxt = (TextView) findViewById(R.id.information);

        // initialize value
        mEnableCB.setChecked(mApp.mbEnable);
        mAutoConnectCB.setChecked(mApp.mbAutoConnect);
        mConnectBtn.setEnabled(mApp.mbEnable);
        // set listener
        mAccountBtn.setOnClickListener(mAccountbtnListener);
        mConnectBtn.setOnClickListener(mConnectBtnListener);

        mAutoConnectCB.setOnCheckedChangeListener(mAutoConnectCBListener);
        mEnableCB.setOnCheckedChangeListener(mEnableCBListener);

    }

    private void initializeService() {
        // start IMCore
        startIMCoreService();

        // start BTClientCore
        if (mApp.mbEnable)
            startBTClientCoreService();
    }

}