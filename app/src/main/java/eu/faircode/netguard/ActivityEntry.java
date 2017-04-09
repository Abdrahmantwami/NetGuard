package eu.faircode.netguard;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;

/**
 * Created by Carlos on 4/8/17.
 */

public class ActivityEntry extends AppCompatActivity {
    private static final int MIN_SDK = Build.VERSION_CODES.ICE_CREAM_SANDWICH;


    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT < MIN_SDK) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.android);
            return;
        }

        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry);

    }

    @Override public boolean onCreateOptionsMenu(final Menu menu) {
        if (Build.VERSION.SDK_INT < MIN_SDK) { return false; }
        return super.onCreateOptionsMenu(menu);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.entry_firewall:
                startActivity(new Intent(this, ActivityMain.class));
                break;
            case R.id.entry_virus:
                break;
            case R.id.entry_web:
                break;
        }
    }
}
