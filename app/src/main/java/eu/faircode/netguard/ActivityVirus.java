package eu.faircode.netguard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Carlos on 4/9/17.
 */

public class ActivityVirus extends AppCompatActivity {


    @Override protected void onCreate(@Nullable final Bundle savedInstanceState) {
        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.virus);
    }
}
