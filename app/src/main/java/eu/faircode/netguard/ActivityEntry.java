package eu.faircode.netguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;

/**
 * Created by Carlos on 4/8/17.
 */

public class ActivityEntry extends AppCompatActivity implements SharedPreferences
        .OnSharedPreferenceChangeListener {
    private static final int MIN_SDK = Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    private static final String TAG = "ActivityEntry";


    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        Util.setTheme(this);

        if (Build.VERSION.SDK_INT < MIN_SDK) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.android);
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry);


        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override public boolean onCreateOptionsMenu(final Menu menu) {
        if (Build.VERSION.SDK_INT < MIN_SDK) { return false; }
        return super.onCreateOptionsMenu(menu);
    }

    @Override protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);


        super.onDestroy();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.entry_firewall:
                startActivity(new Intent(this, ActivityMain.class));
                break;
            case R.id.entry_virus:
                startActivity(new Intent(this, ActivityVirus.class));
                break;
            case R.id.entry_web:
                break;
        }
    }


    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String
            key) {
        Log.i(TAG, "Preference " + key + "=" + prefs.getAll().get(key));
        if ("theme".equals(key) || "dark_theme".equals(key)) { recreate(); }
    }
}
