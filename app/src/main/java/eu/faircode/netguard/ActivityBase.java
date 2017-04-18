package eu.faircode.netguard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by Carlos on 4/18/17.
 */

public abstract class ActivityBase extends AppCompatActivity implements SharedPreferences
        .OnSharedPreferenceChangeListener {
    public abstract String getTag();

    @Override protected void onCreate(@Nullable final Bundle savedInstanceState) {
        Util.setTheme(this);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String
            key) {
        Log.i(getTag(), "Preference " + key + "=" + prefs.getAll().get(key));
        if ("theme".equals(key) || "dark_theme".equals(key)) { recreate(); }
    }
}
