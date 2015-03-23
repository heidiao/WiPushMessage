
package wistron.pushmessage.server;

import wistron.pushmessage.server.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

public class MainActivity extends Activity {

    private CheckBox mCheckBoxEnable, mCheckBoxNotify;
    private Button mButtonList;

    public static final String BT_STORAGE = "BT_STORAGE";
    public static final String BT_ENABLE = "BT_ENABLE";
    public static final String BT_NOTIFY = "BT_NOTIFY";
    public static final String BT_LIST = "BT_LIST";
    public static final boolean ENABLE = true;
    public static final boolean DISABLE = false;

    private SharedPreferences btStore, btStoreList;
    private BluetoothAdapter mBluetoothAdapter = null;

    // Intent request codes
    private static final int REQUEST_DISCOVERY_BT = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle(R.string.title_name);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.toast_not_available, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        // If Bluetooth has not been enabled on the device,
        // then enabling device discoverability will automatically enable Bluetooth
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            
            // A value of 0 means device is always discoverable
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivityForResult(discoverableIntent, REQUEST_DISCOVERY_BT);
        }

        // component find by resource
        mCheckBoxEnable = (CheckBox) findViewById(R.id.checkBoxEnable);
        mCheckBoxNotify = (CheckBox) findViewById(R.id.checkBoxNotify);
        mButtonList = (Button) findViewById(R.id.btnList);

        // add listener
        mCheckBoxEnable.setOnCheckedChangeListener(CheckBoxEnableOnChange);
        mCheckBoxNotify.setOnCheckedChangeListener(CheckBoxNotifyOnChange);
        mButtonList.setOnClickListener(ButtonListOnClick);

        // instant store
        btStore = getSharedPreferences(BT_STORAGE, 0);
        btStoreList = getSharedPreferences(BT_LIST, 0);

        // get store
        boolean state = btStore.getBoolean(BT_ENABLE, DISABLE);
        if (state == ENABLE) { // enable
            mCheckBoxEnable.setChecked(true);
        } else { // disable
            mCheckBoxEnable.setChecked(false);
            mCheckBoxNotify.setEnabled(false);
            mButtonList.setEnabled(false);
        }
        if (btStore.getBoolean(BT_NOTIFY, ENABLE)) {
            mCheckBoxNotify.setChecked(true);
        }
    }

    /**
     * Click List Button
     */
    private Button.OnClickListener ButtonListOnClick = new Button.OnClickListener() {
        public void onClick(View v) {
            Intent serverIntent = new Intent(getApplicationContext(), ClientListActivity.class);
            startActivity(serverIntent);
        }
    };

    /**
     * Check Enable
     */
    private OnCheckedChangeListener CheckBoxEnableOnChange = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            // checked enable CheckBox
            if (buttonView.isChecked() == true) {
                // change UI effect
                mCheckBoxNotify.setEnabled(true);
                mButtonList.setEnabled(true);

                // store enable
                btStore.edit().putBoolean(BT_ENABLE, ENABLE).commit();

                // start service
                Intent it = new Intent(MainActivity.this, BTCoreService.class);
                startService(it);
            } else {
                // change UI effect
                mCheckBoxNotify.setEnabled(false);
                mButtonList.setEnabled(false);

                // store enable
                btStore.edit().putBoolean(BT_ENABLE, DISABLE).commit();

                // stop service
                Intent it = new Intent(MainActivity.this, BTCoreService.class);
                stopService(it);
            }
        }
    };

    /**
     * Check Notification
     */
    private OnCheckedChangeListener CheckBoxNotifyOnChange = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            // checked notification CheckBox
            if (buttonView.isChecked() == true) {
                // enable notification
                btStore.edit().putBoolean(BT_NOTIFY, ENABLE).commit();
            } else {
                // disable notification
                btStore.edit().putBoolean(BT_NOTIFY, DISABLE).commit();
            }
        }
    };

    /**
     * Activity Result
     * 
     * @param int request code: REQUEST_DISCOVERY_BT = Box's bluetooth make discoverable
     * @param int result code: receive canceled and finish.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_DISCOVERY_BT:
                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, R.string.toast_not_discovery, Toast.LENGTH_SHORT)
                            .show();
                    finish();
                }
                break;
        }
    }
}
