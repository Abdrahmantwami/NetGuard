package eu.faircode.netguard.monitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import eu.faircode.netguard.ActivityMain;
import eu.faircode.netguard.BuildConfig;
import eu.faircode.netguard.R;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Carlos on 4/4/17.
 */

public class FileScannerEngine extends BroadcastReceiver {
    private static final String TAG = "FileScannerEngine";
    private final Context mContext;
    private HandlerThread scanThread;
    private ScanHandler mScanHandler;
    private UIHandler mUIHandler;


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
        mUIHandler.openNotification();
    }

    public void destroy() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this);
        scanThread.quit();
        mUIHandler.closeNotification();
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
        public static final int MSG_OPEN_NOTIFICATION = 0;
        public static final int MSG_CLOSE_NOTIFICATION = 1;
        public static final int MSG_SCAN_FAIL = 10;
        public static final int MSG_SCAN_SAFE = 11;
        public static final int MSG_SCAN_QUEUE = 12;
        public static final int MSG_SCAN_DANDER = 13;
        public static final int MSG_SCAN_SOLVE = 14;

        private int sum;
        private int danger;
        private int solved;
        private int queue;
        private boolean notificationEnable = false;

        private void openNotification() {
            this.sendEmptyMessage(MSG_OPEN_NOTIFICATION);
        }

        private void closeNotification() {
            this.sendEmptyMessage(MSG_CLOSE_NOTIFICATION);
        }


        public static final int NOTIFICATION_VIRUS = 1024;

        private void updateVirusNotification() {
            if (!notificationEnable) { return; }
            NotificationManager nm = (NotificationManager) mContext.getSystemService
                    (NOTIFICATION_SERVICE);
            Notification notification = getVirusNotification(sum, danger, solved, queue);
            nm.notify(NOTIFICATION_VIRUS, notification);
        }

        private Notification getVirusNotification(int sum, int danger, int solved, int queue) {
            Intent virus = new Intent(mContext, ActivityMain.class);//TODO create new act
            PendingIntent pi = PendingIntent.getActivity(mContext, 0, virus, PendingIntent
                    .FLAG_UPDATE_CURRENT);

            TypedValue tv = new TypedValue();
            mContext.getTheme().resolveAttribute(R.attr.colorPrimary, tv, true);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                    .setSmallIcon(R.drawable.ic_security_white_24dp)
                    .setContentIntent(pi)
                    .setColor(tv.data)
                    .setOngoing(true)//TODO allow dismiss
                    .setAutoCancel(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setCategory(Notification.CATEGORY_SERVICE)
                        .setVisibility(Notification.VISIBILITY_PUBLIC);
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                builder.setContentTitle(mContext.getString(R.string.app_name));
            }


            String s1;
            final String s2 = mContext.getString(R.string.msg_virus_protect_stats, sum, queue,
                    solved);

            if (danger > 0) {
                s1 = mContext.getString(R.string.msg_virus_protect_danger, danger);
                builder.setPriority(Notification.PRIORITY_MAX);
                //TODO change icon to red, or !,  add sound

            } else if (queue > 0) {
                s1 = mContext.getString(queue == 1 ? R.string.msg_virus_protect_queue_one : R.string
                        .msg_virus_protect_queue_some);
                builder.setPriority(Notification.PRIORITY_DEFAULT);
            } else {
                s1 = mContext.getString(R.string.msg_virus_protect_safe);
                builder.setPriority(Notification.PRIORITY_MIN);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setContentTitle(s1);
                builder.setContentText(s2);
                return builder.build();
            } else {
                builder.setContentText(s1);
                NotificationCompat.BigTextStyle notification = new NotificationCompat
                        .BigTextStyle(builder);
                notification.bigText(s2);
                return notification.build();
            }
        }


        public UIHandler(final Looper looper) {
            super(looper);
        }

        @Override public void handleMessage(final Message msg) {
            Log.i(TAG, String.format("UIHandler what %d, obj %s", msg.what, msg.obj));
            switch (msg.what) {
                case MSG_OPEN_NOTIFICATION:
                    notificationEnable = true;
                    break;
                case MSG_CLOSE_NOTIFICATION:
                    notificationEnable = false;
                    NotificationManager nm = (NotificationManager) mContext.getSystemService
                            (NOTIFICATION_SERVICE);
                    nm.cancel(NOTIFICATION_VIRUS);
                    break;
                case MSG_SCAN_SAFE:
                    sum++;
                    break;
                case MSG_SCAN_DANDER:
                    sum++;
                    danger++;
                    //TODO ask user to handle
                    break;
                case MSG_SCAN_FAIL:
                    sum++;
                    break;
                case MSG_SCAN_SOLVE:
                    danger--;
                    solved++;
                    break;
            }
            updateVirusNotification();
        }
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
                        m.what = UIHandler.MSG_SCAN_SAFE;
                        break;
                    case SkipSafe:
                        m.what = UIHandler.MSG_SCAN_SAFE;
                        break;
                    case Safe:
                        m.what = UIHandler.MSG_SCAN_SAFE;
                        break;
                    case Danger:
                        m.what = UIHandler.MSG_SCAN_DANDER;
                        break;
                    case Queue:
                        m.what = UIHandler.MSG_SCAN_QUEUE;
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
                m.what = UIHandler.MSG_SCAN_FAIL;
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
                        m.what = UIHandler.MSG_SCAN_SAFE;
                        break;
                    case SkipSafe:
                        m.what = UIHandler.MSG_SCAN_SAFE;
                        break;
                    case Safe:
                        m.what = UIHandler.MSG_SCAN_SAFE;
                        break;
                    case Danger:
                        m.what = UIHandler.MSG_SCAN_DANDER;
                        break;
                    case Queue:
                        m.what = UIHandler.MSG_SCAN_QUEUE;
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
                m.what = UIHandler.MSG_SCAN_FAIL;
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
