package eu.faircode.netguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import eu.faircode.netguard.monitor.FileScannerService;
import eu.faircode.netguard.view.FloatActionSwitch;

/**
 * Created by Carlos on 4/9/17.
 */

public class ActivityVirus extends AppCompatActivity implements SharedPreferences
        .OnSharedPreferenceChangeListener {


    private static final String TAG = "ActivityVirus";

    @Override protected void onCreate(@Nullable final Bundle savedInstanceState) {
        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.virus);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        AdapterVirus adapterVirus = new AdapterVirus(this);
        recyclerView.setAdapter(adapterVirus);

        FloatActionSwitch fab = (FloatActionSwitch) findViewById(R.id.fab);
        fab.setOnCheckedChangeListener(new FloatActionSwitch.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(final boolean isChecked) {
                prefs.edit().putBoolean("enabled_virus", isChecked).apply();
                if (isChecked) { ActivityVirus.this.startVirusService();} else {
                    ActivityVirus.this.pauseVirusService();
                }
            }
        });
    }


    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String
            key) {
        Log.i(TAG, "Preference " + key + "=" + prefs.getAll().get(key));
        if ("theme".equals(key) || "dark_theme".equals(key)) { recreate(); }
    }

    @Override protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);


        super.onDestroy();
    }

    @Override public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.virus, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuScan:

                return true;
            case R.id.menuScanSettings:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startVirusService() {
        Intent intent = new Intent(this, FileScannerService.class);
        intent.putExtra(FileScannerService.EXTRA_COMMAND, FileScannerService.Command.RUN);
        startService(intent);
    }

    private void pauseVirusService() {
        Intent intent = new Intent(this, FileScannerService.class);
        intent.putExtra(FileScannerService.EXTRA_COMMAND, FileScannerService.Command.PAUSE);
        startService(intent);
    }
}
