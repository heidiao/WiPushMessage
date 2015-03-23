
package wistron.pushmessage.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        
        // Check bluetooth enable then auto start the service
        SharedPreferences btStore = context.getSharedPreferences(MainActivity.BT_STORAGE, 0);
        boolean state = btStore.getBoolean(MainActivity.BT_ENABLE, MainActivity.DISABLE);
        if (state == MainActivity.ENABLE) {
            Intent it = new Intent(context, BTCoreService.class);
            context.startService(it);
        }
    }

}
