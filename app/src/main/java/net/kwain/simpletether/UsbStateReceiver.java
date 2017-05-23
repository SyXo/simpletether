package net.kwain.simpletether;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.List;

/**
 * Created by jflesch on 23/05/17.
 */

public class UsbStateReceiver extends BroadcastReceiver {
    private final static String TAG = "UsbStateReceiver";

    // hidden Android API
    private final static String ACTION_TETHER_STATE_CHANGED = (
            "android.net.conn.TETHER_STATE_CHANGED"
    );
    private final static String EXTRA_ACTIVE_TETHER = "activeArray";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ACTION_TETHER_STATE_CHANGED)) {
            List<String> actives = intent.getStringArrayListExtra(EXTRA_ACTIVE_TETHER);
            int nbActives = actives.size();
            NetworkStatsActivity.setTetheringEnabled(nbActives > 0);
            if (nbActives <= 0) {
                return;
            }
        }
        PackageManager pm = context.getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage("net.kwain.simpletether");
        launch.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_FROM_BACKGROUND
                | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
    }
}
