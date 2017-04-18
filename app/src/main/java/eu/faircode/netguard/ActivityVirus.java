package eu.faircode.netguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

import eu.faircode.netguard.monitor.FileScannerService;
import eu.faircode.netguard.monitor.Scan;
import eu.faircode.netguard.view.FloatActionSwitch;

/**
 * Created by Carlos on 4/9/17.
 */

public class ActivityVirus extends AppCompatActivity implements SharedPreferences
        .OnSharedPreferenceChangeListener {


    private static final String TAG = "ActivityVirus";

    private ORMDatabaseHelper mDatabaseHelper;
    private ListView listView;
    private AdapterVirusList mAdapter;

    @Override protected void onCreate(@Nullable final Bundle savedInstanceState) {
        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.virus);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        // RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        // recyclerView.setHasFixedSize(true);
        // AdapterVirus adapterVirus = new AdapterVirus(this);
        // recyclerView.setAdapter(adapterVirus);

        listView = (ListView) findViewById(R.id.listView);

        ImageView loading = new ImageView(this);
        loading.setImageResource(R.drawable.sample_footer_loading_progress);
        loading.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams
                .MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        listView.setEmptyView(loading);

        mAdapter = new AdapterVirusList(this);
        listView.setAdapter(mAdapter);


        FloatActionSwitch fab = (FloatActionSwitch) findViewById(R.id.fab);
        fab.setOnCheckedChangeListener(isChecked -> {
            prefs.edit().putBoolean("enabled_virus", isChecked).apply();
            long first = prefs.getLong("first_enable_virus", 0L);
            if (first == 0) {
                prefs.edit().putLong("first_enable_virus", System.currentTimeMillis()).apply();
            }

            if (isChecked) { ActivityVirus.this.startVirusService();} else {
                ActivityVirus.this.pauseVirusService();
            }
        });
        boolean enable = prefs.getBoolean("enabled_virus", false);
        fab.setChecked(enable);

        mDatabaseHelper = new ORMDatabaseHelper(this);
        loadingData();
    }

    private void loadingData() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            try {
                Dao<Scan, Long> dao = mDatabaseHelper.getDao(Scan.class);
                List<Scan> scans = dao.queryForAll();
                runOnUiThread(() -> onResult(scans));
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> onResult(null));
            }
        });
    }

    private void onResult(@Nullable List<Scan> scans) {
        if (isDestroyed()) {
            return;
        }
        ImageView emptyView = new ImageView(this);
        emptyView.setImageResource(R.drawable.ic_empty);
        emptyView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams
                .MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        listView.setEmptyView(emptyView);

        if (scans != null && scans.size() > 0) {
            mAdapter.add(scans);
        }
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
        mAdapter.onDestory();

        mDatabaseHelper.close();
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
