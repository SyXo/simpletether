package net.kwain.simpletether;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class NetworkStatsActivity extends AppCompatActivity {
    private static final String TAG = "NetworkStatsActivity";

    public static boolean tethering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.graph_title);
        setContentView(R.layout.activity_network_stats);
    }

    public static void setTetheringEnabled(boolean enabled) {
        tethering = enabled;
    }

    public boolean isTetheringEnabled() {
        return tethering;
    }

    private void openConnectionSharingSettings() {
        Intent launch = new Intent();
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launch.setClassName("com.android.settings", "com.android.settings.TetherSettings");
        startActivity(launch);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && !isTetheringEnabled()) {
            openConnectionSharingSettings();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
