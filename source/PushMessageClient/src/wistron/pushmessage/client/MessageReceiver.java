package wistron.pushmessage.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MessageReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        // TODO Auto-generated method stub
        if (arg1.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent BootIntent = new Intent(arg0, IMCoreService.class);
            BootIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            arg0.startService(BootIntent);
        }
    }

}
