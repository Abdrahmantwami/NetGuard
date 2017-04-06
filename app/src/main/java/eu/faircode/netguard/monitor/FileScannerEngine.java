package eu.faircode.netguard.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import eu.faircode.netguard.BuildConfig;
import eu.faircode.netguard.R;
import retrofit2.Response;

/**
 * Created by Carlos on 4/4/17.
 */

public class FileScannerEngine extends BroadcastReceiver {
    private static final String TAG = "FileScannerEngine";
    private final Context mContext;
    private HandlerThread scanThread;
    private ScanHandler mScanHandler;
    private Handler mUIHandler;


    public FileScannerEngine(Context context) {
        mContext = context;

    }

    private static String sha1(final File file) throws NoSuchAlgorithmException, IOException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            final byte[] buffer = new byte[1024];
            for (int read = 0; (read = is.read(buffer)) != -1; ) {
                messageDigest.update(buffer, 0, read);
            }
        } catch (IOException e) {
            if (is != null) {
                is.close();
            }
            throw e;
        }

        // Convert the byte to hex format
        Formatter formatter = new Formatter();
        for (final byte b : messageDigest.digest()) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private void enqueueFile(@NonNull File file) {
        Message m = mScanHandler.obtainMessage();
        m.obj = file;
        mScanHandler.sendMessage(m);
    }

    public void init() {
        scanThread = new HandlerThread(mContext.getString(R.string.app_name) +
                " scan", Process.THREAD_PRIORITY_FOREGROUND);
        scanThread.start();

        mScanHandler = new ScanHandler(scanThread.getLooper());
        mUIHandler = new UIHandler(mContext.getMainLooper());

        LocalBroadcastManager.getInstance(mContext).registerReceiver(this, new IntentFilter
                (DownloadFileObserver.ACTION_SCAN));
        Log.i(TAG, " scan engine start");
    }

    public void destroy() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this);
        scanThread.quit();
        Log.i(TAG, "scan engine shutdown");
    }

    @Override public void onReceive(final Context context, final Intent intent) {
        Log.i(TAG, String.format("FileScannerEngine receive intent: %s", intent));
        if (intent != null && intent.getAction().equals(DownloadFileObserver.ACTION_SCAN)) {
            File file = (File) intent.getSerializableExtra("file");
            if (file == null) {
                Log.e(TAG, "onReceive null file extra");
            } else {
                enqueueFile(file);
            }
        }
    }

    private class UIHandler extends Handler {
        public static final int SCAN_EXCEED = -2;
        public static final int SCAN_FAIL = -1;
        public static final int SCAN_SAFE = 0;
        public static final int SCAN_DANDER = 1;

        public UIHandler(final Looper looper) {
            super(looper);
        }

        @Override public void handleMessage(final Message msg) {
            Log.i(TAG, String.format("UIHandler what %d, obj %s", msg.what, msg.obj));
            switch (msg.what) {
                case SCAN_SAFE:
                    //TODO
                    break;
                case SCAN_DANDER:
                    break;
                case SCAN_FAIL:
                    break;

                case SCAN_EXCEED:

                    break;
            }
        }
        //TODO add some method to send notification

    }

    private class ScanHandler extends Handler {
        private static final String TAG = "ScanHandler";

        ScanHandler(final Looper looper) {
            super(looper);
        }

        private ScanQueryResult hashScan(File file) throws IOException, NoSuchAlgorithmException,
                APIExceededException, HTTPException {
            String sha1 = sha1(file);

            // official test case
            if (BuildConfig.DEBUG) {
                if (file.getName().contains("hash_yyz_test")) {
                    sha1 = "E71A6D8760B37E45FA09D3E1E67E2CD3";
                } else if (file.getName().contains("api_yyz_test")) {
                    return null;
                }
            }

            MetaDefenderAPI api = RetrofitFactory.getMetaDefenderAPI();
            Response<ScanQueryResult> resp = api.hashLookUp(sha1).execute();
            Log.i(TAG, String.format("hashLookUp sha1: %s resp: %s", sha1, resp));

            if (resp.isSuccessful()) {
                return resp.body();
            } else if (resp.code() == 403) {
                throw new APIExceededException();
            } else {
                throw new HTTPException(resp);
            }
        }

        @NonNull
        private ScanQueryResult uploadScan(File file) {
            if (BuildConfig.DEBUG && file.getName().contains("upload_yyz_test")) {
                //TODO upload always
            }

            //TODO add upload scan
            return null;
        }

        private ScanQueryResult localScan(File file) {
            if (BuildConfig.DEBUG && file.getName().contains("yyz_test")) {
                return null;
            }

            if (file.canExecute()) {
                return null;
            } else {
                return ScanQueryResult.safeSkipScan(file);
            }
        }

        @Override public void handleMessage(final Message msg) {
            final File file = (File) msg.obj;
            Log.i(TAG, String.format(" handleMessage file: %s", file));

            if (file == null) {
                Log.e(TAG, " msg with null file obj");
                return;
            }

            Message m = mUIHandler.obtainMessage();

            try {
                ScanQueryResult scan = localScan(file);
                if (scan == null) {
                    scan = hashScan(file);
                }
                if (scan == null) {
                    scan = uploadScan(file);
                    if (BuildConfig.DEBUG) {
                        //noinspection ConstantConditions
                        assert scan != null;
                    }
                }

                m.obj = scan;
                if (scan.isSafe()) {
                    m.what = UIHandler.SCAN_SAFE;
                } else {
                    m.what = UIHandler.SCAN_DANDER;
                }
                // TODO write to database

            } catch (NoSuchAlgorithmException | IOException | HTTPException e) {
                m.what = UIHandler.SCAN_FAIL;
                Log.e(TAG, "error when scan", e);
            } catch (APIExceededException e) {
                m.what = UIHandler.SCAN_EXCEED;
                Log.e(TAG, "scan limit exceeded", e);
            }
            mUIHandler.sendMessage(m);
        }


    }

}
