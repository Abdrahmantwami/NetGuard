package eu.faircode.netguard.monitor;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.FileObserver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;

/**
 * Created by Carlos on 4/4/17.
 */

public class DownloadFileObserver extends FileObserver {
    public static final String ACTION_SCAN = DownloadFileObserver.class.getPackage().toString() +
            ".onScanFile";
    private static final String TAG = "DownloadFileObserver";
    private final Context mContext;


    public DownloadFileObserver(Context context) {
        super(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getPath(), ATTRIB | CLOSE_WRITE | CREATE);
        Log.i(TAG, String.format("dir : %s", Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS)));
        mContext = context;
    }

    @Override public void startWatching() {
        super.startWatching();
        Log.i(TAG, "startWatching");
    }

    @Override public void onEvent(final int event, final String path) {
        Log.i(TAG, String.format("onEvent %x,path %s", event, path));
        if (path == null) {
            return;
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), path);
        if (!isInWhiteList(file)) {
            onScanFile(file);
        }
    }

    private boolean isInWhiteList(File file) {
        // TODO
        return false;
    }

    private void onScanFile(final File file) {
        Intent intent = new Intent(ACTION_SCAN);
        intent.putExtra("file", file);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        Log.i(TAG, String.format("send scan broadcast: %s", intent));
    }
}
