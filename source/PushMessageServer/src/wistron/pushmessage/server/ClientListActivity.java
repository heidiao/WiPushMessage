
package wistron.pushmessage.server;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Collection;
import java.util.Map;

public class ClientListActivity extends Activity {

    private ArrayAdapter<String> mDeviceArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        mDeviceArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item);
        ListView mDevicesListView = (ListView) findViewById(R.id.listView);
        mDevicesListView.setAdapter(mDeviceArrayAdapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mDeviceArrayAdapter.clear();
        SharedPreferences btStore = getSharedPreferences(MainActivity.BT_LIST, 0);
        Map<String, ?> values = btStore.getAll();
        Collection<String> keys = values.keySet();
        for (String s : keys) {
            mDeviceArrayAdapter.add(values.get(s) + "\n" + s);
        }
    }

}
