package eu.faircode.netguard;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Carlos on 4/8/17.
 */

public class ActivityEntry extends AppCompatActivity implements SharedPreferences
        .OnSharedPreferenceChangeListener {
    private static final int MIN_SDK = Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    private static final String TAG = "ActivityEntry";
    private static final int REQUEST_INVITE = 2;
    private static final int REQUEST_LOGCAT = 3;
    private AlertDialog dialogAbout = null;

    private static Intent getIntentRate(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context
                .getPackageName()));
        if (intent.resolveActivity(context.getPackageManager()) == null) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google" +
                    ".com/store/apps/details?id=" + context.getPackageName()));
        }
        return intent;
    }

    private static Intent getIntentInvite(Context context) {
        Intent intent = new Intent("com.google.android.gms.appinvite.ACTION_APP_INVITE");
        intent.setPackage("com.google.android.gms");
        intent.putExtra("com.google.android.gms.appinvite.TITLE", context.getString(R.string
                .menu_invite));
        intent.putExtra("com.google.android.gms.appinvite.MESSAGE", context.getString(R.string
                .msg_try));
        intent.putExtra("com.google.android.gms.appinvite.BUTTON_TEXT", context.getString(R
                .string.msg_try));
        // com.google.android.gms.appinvite.DEEP_LINK_URL
        return intent;
    }

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


        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.entry, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent
            data) {
        Log.i(TAG, "onActivityResult request=" + requestCode + " result=" + requestCode + " ok="
                + (resultCode == RESULT_OK));
        Util.logExtras(data);

        if (requestCode == REQUEST_INVITE) {
            // Do nothing

        }
    }

    @Override public boolean onOptionsItemSelected(final MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_invite:
                startActivityForResult(getIntentInvite(this), REQUEST_INVITE);
                return true;
            case R.id.menu_about:
                menuAbout();
                return true;

        }


        return super.onOptionsItemSelected(item);
    }

    private Intent getIntentLogcat() {
        Intent intent;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            if (Util.isPackageInstalled("org.openintents.filemanager", this)) {
                intent = new Intent("org.openintents.action.PICK_DIRECTORY");
            } else {
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=org" +
                        ".openintents.filemanager"));
            }
        } else {
            intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, "logcat.txt");
        }
        return intent;
    }

    private void menuAbout() {
        // Create view
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.about, null, false);
        TextView tvVersionName = (TextView) view.findViewById(R.id.tvVersionName);
        TextView tvVersionCode = (TextView) view.findViewById(R.id.tvVersionCode);
        Button btnRate = (Button) view.findViewById(R.id.btnRate);
        TextView tvLicense = (TextView) view.findViewById(R.id.tvLicense);
        TextView tvAdmob = (TextView) view.findViewById(R.id.tvAdmob);

        // Show version
        tvVersionName.setText(Util.getSelfVersionName(this));
        if (!Util.hasValidFingerprint(this)) { tvVersionName.setTextColor(Color.GRAY); }
        tvVersionCode.setText(Integer.toString(Util.getSelfVersionCode(this)));

        // Handle license
        tvLicense.setMovementMethod(LinkMovementMethod.getInstance());
        tvAdmob.setMovementMethod(LinkMovementMethod.getInstance());
        tvAdmob.setVisibility(View.GONE);

        // Handle logcat
        view.setOnClickListener(new View.OnClickListener() {
            private short tap = 0;
            private Toast toast = Toast.makeText(ActivityEntry.this, "", Toast.LENGTH_SHORT);

            @Override
            public void onClick(View view) {
                tap++;
                if (tap == 7) {
                    tap = 0;
                    toast.cancel();

                    Intent intent = getIntentLogcat();
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(intent, REQUEST_LOGCAT);
                    }

                } else if (tap > 3) {
                    toast.setText(Integer.toString(7 - tap));
                    toast.show();
                }
            }
        });

        // Handle rate
        btnRate.setVisibility(View.GONE);
        btnRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(getIntentRate(ActivityEntry.this));
            }
        });

        // Show dialog
        dialogAbout = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        dialogAbout = null;
                    }
                })
                .create();
        dialogAbout.show();
    }

    @Override protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);


        if (dialogAbout != null) {
            dialogAbout.dismiss();
            dialogAbout = null;
        }


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
