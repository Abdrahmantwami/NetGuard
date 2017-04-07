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
import android.support.annotation.Nullable;
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
import okhttp3.MediaType;
import okhttp3.RequestBody;
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

    private static String sha1(final File file) throws ScanException {
        try {
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
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new ScanHashException(e);
        }
    }

    private void enqueueFile(@NonNull File file) {
        Message m = mScanHandler.obtainMessage(ScanHandler.MSG_WHAT_QUEUE);
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
            }
        }
        //TODO add some method to send notification

    }

    private class ScanHandler extends Handler {
        private static final String TAG = "ScanHandler";

        public static final int MSG_WHAT_QUEUE = 0;
        public static final int MSG_WHAT_QUERY = 1;
        private MetaDefenderAPI api = RetrofitFactory.getMetaDefenderAPI();

        ScanHandler(final Looper looper) {
            super(looper);
        }

        private void handleQuery(ScanQueryResult scanQueryResult) {
            Log.i(TAG, String.format(" handleQuery scan: %s", scanQueryResult));

            if (scanQueryResult == null) {
                Log.e(TAG, " msg with null ScanQueryResult obj");
                return;
            }

            Message m = mUIHandler.obtainMessage();

            try {
                ScanQueryResult scan = queryScan(scanQueryResult);

                if (BuildConfig.DEBUG) {
                    //noinspection ConstantConditions
                    assert scan != null;
                }

                m.obj = scan;
                // TODO write to database

                Log.i(TAG, String.format("scan status type %s, file %s", scan.which(), scan
                        .fileInfo.file));
                switch (scan.which()) {
                    case SkipLarge:
                        break;
                    case SkipSafe:
                        break;
                    case Safe:
                        break;
                    case Danger:
                        break;
                    case Queue:
                        final Message msg = mScanHandler.obtainMessage(MSG_WHAT_QUERY);
                        msg.obj = scan;
                        mScanHandler.postDelayed(new Runnable() {
                            @Override public void run() {
                                mScanHandler.sendMessage(msg);
                            }
                        }, 1000 * 60 * 2);
                        break;
                }
            } catch (IOException | ScanException e) {
                m.what = UIHandler.SCAN_FAIL;
                m.obj = scanQueryResult;
                Log.e(TAG, "error when query", e);
            }
            mUIHandler.sendMessage(m);
        }

        private ScanQueryResult queryScan(ScanQueryResult scanQueryResult) throws
                ScanException, IOException {
            File file = scanQueryResult.fileInfo.file;
            String restIp = scanQueryResult.restIp;
            String dataId = scanQueryResult.dataId;

            String url = String.format("https://%s/file/%s", restIp, dataId);

            Response<ScanQueryResult> resp = api.queryScan(url).execute();
            if (resp.isSuccessful()) {
                ScanQueryResult scanResult = resp.body();
                if (scanResult == null) {
                    throw new ScanException("null scan result returned by queryScan");
                } else {
                    //TODO what if this scan complete?
                    //TODO if not complete, this will still be "inqueue"?
                    return scanQueryResult.file(file).auto();
                }
            } else if (resp.code() == 403) {
                throw new ScanAPIExceededException();
            } else { throw new ScanHTTPException(resp); }
        }

        private void handleQueue(@Nullable
                                 final File file) {
            Log.i(TAG, String.format(" handleQueue file: %s", file));

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
                // TODO write to database

                Log.i(TAG, String.format("scan status type %s, file %s", scan.which(), scan
                        .fileInfo.file));
                switch (scan.which()) {
                    case SkipLarge:
                        break;
                    case SkipSafe:
                        break;
                    case Safe:
                        break;
                    case Danger:
                        break;
                    case Queue:
                        final Message msg = mScanHandler.obtainMessage(MSG_WHAT_QUERY);
                        msg.obj = scan;
                        mScanHandler.postDelayed(new Runnable() {
                            @Override public void run() {
                                mScanHandler.sendMessage(msg);
                            }
                        }, 1000 * 60 * 2);
                        break;
                }
            } catch (IOException | ScanException e) {
                m.what = UIHandler.SCAN_FAIL;
                m.obj = ScanQueryResult.errorStubQueryResult(e, file);
                Log.e(TAG, "error when scan", e);
            }
            mUIHandler.sendMessage(m);
        }

        @Nullable
        private ScanQueryResult hashScan(File file) throws ScanException, IOException {
            String sha1 = sha1(file);

            // official test case
            if (BuildConfig.DEBUG) {
                if (file.getName().contains("hash_yyz_test")) {
                    sha1 = "E71A6D8760B37E45FA09D3E1E67E2CD3";
                } else if (file.getName().contains("api_yyz_test")) {
                    return null;
                }
            }

            Response<ScanQueryResult> resp = api.hashLookUp(sha1).execute();
            Log.i(TAG, String.format("hashLookUp file: %s sha1: %s resp: %s", file, sha1,
                    resp));

            if (resp.isSuccessful()) {
                ScanQueryResult body = resp.body();
                if (body != null) {
                    body.file(file).auto();
                }
                return body;
            } else if (resp.code() == 403) {
                throw new ScanAPIExceededException();
            } else {
                throw new ScanHTTPException(resp);
            }
        }

        @NonNull
        private ScanQueryResult uploadScan(File file) throws ScanException, IOException {
            if (file.length() > 140 * 1000 * 1000) {
                return ScanQueryResult.skipLargeFile(file);
            }

            RequestBody body = RequestBody.create(MediaType.parse("file"), file);

            Response<ScanQueryResult> resp = api.uploadScan(file.getName(), body).execute();
            Log.i(TAG, String.format("uploadLookup file: %s", file));

            if (resp.isSuccessful()) {
                ScanQueryResult scanResult = resp.body();
                if (scanResult == null) {
                    throw new ScanException("null scan result returned by uploadScan");
                } else {
                    return scanResult.file(file).auto();
                }
            } else if (resp.code() == 403) {
                throw new ScanAPIExceededException();
            } else {
                throw new ScanHTTPException(resp);
            }
        }

        @Nullable
        private ScanQueryResult localScan(File file) {
            if (BuildConfig.DEBUG && file.getName().contains("yyz_test")) {
                return null;
            }

            if (file.length() > 0) {
                return null;
            } else {
                return ScanQueryResult.skipSafeFile(file);
            }
        }

        @Override public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_WHAT_QUERY:
                    ScanQueryResult scanQueryResult = (ScanQueryResult) msg.obj;
                    handleQuery(scanQueryResult);
                    break;
                case MSG_WHAT_QUEUE:
                    final File file = (File) msg.obj;
                    handleQueue(file);
                    break;
            }


        }


    }

}
