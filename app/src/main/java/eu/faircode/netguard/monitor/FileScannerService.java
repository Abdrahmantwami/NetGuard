package eu.faircode.netguard.monitor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;

import java.io.File;
import java.io.IOException;

import eu.faircode.netguard.ActivityMain;
import eu.faircode.netguard.BuildConfig;
import eu.faircode.netguard.R;
import eu.faircode.netguard.Receiver;
import eu.faircode.netguard.ServiceSinkhole;
import eu.faircode.netguard.Util;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;

/**
 * Created by Carlos on 4/4/17.
 */

public class FileScannerService extends Service {
    private static final String TAG = "FileScannerService";
    private HandlerThread scanThread;
    private ScanHandler mScanHandler;
    private UIHandler mUIHandler;
    public static final String EXTRA_COMMAND = "Command";

    public enum Command {RUN, PAUSE}

    @Override public int onStartCommand(final Intent oriIntent, final int flags, final int
            startId) {
        Intent intent = oriIntent;


        // Handle service restart
        if (intent == null) {
            Log.i(TAG, "Restart");

            // Recreate intent
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean enabled = prefs.getBoolean("enabled_virus", false);

            intent = new Intent(this, ServiceSinkhole.class);
            intent.putExtra(EXTRA_COMMAND, enabled ? Command.RUN :
                    Command.PAUSE);
        }

        Command c = (Command) intent.getSerializableExtra(EXTRA_COMMAND);

        switch (c) {
            case RUN:
                run();
                break;
            case PAUSE:
                pause();
                break;
        }

        return START_STICKY;
    }

    @Override public void onCreate() {
        Util.setTheme(this);

        super.onCreate();


        LocalBroadcastManager.getInstance(FileScannerService.this).registerReceiver(mReceiver, new
                IntentFilter
                (DownloadFileObserver.ACTION_SCAN));
        mUIHandler = new UIHandler(FileScannerService.this.getMainLooper());

    }

    private void enqueueFile(@NonNull File file) {
        Message m = mScanHandler.obtainMessage(ScanHandler.MSG_WHAT_QUEUE);
        m.obj = file;
        mScanHandler.sendMessage(m);
    }

    public void run() {
        scanThread = new HandlerThread(FileScannerService.this.getString(R.string.app_name) +
                " scan", Process.THREAD_PRIORITY_FOREGROUND);
        scanThread.start();
        mScanHandler = new ScanHandler(scanThread.getLooper());


        Log.i(TAG, " scan engine run");
        mUIHandler.openNotification();

        if (mFileObserver == null) {
            mFileObserver = new DownloadFileObserver(getApplicationContext());
        }
        mFileObserver.startWatching();
    }

    private DownloadFileObserver mFileObserver;


    //TODO pause monitor
    public void pause() {
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
        scanThread.quit();
        mUIHandler.closeNotification();
        Log.i(TAG, "scan engine shutdown");
    }

    @Override public void onDestroy() {
        LocalBroadcastManager.getInstance(FileScannerService.this).unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    private BroadcastReceiver mReceiver = new Receiver() {
        @Override public void onReceive(final Context context, final Intent intent) {
            Log.i(TAG, String.format("FileScannerService receive intent: %s", intent));
            if (intent != null && intent.getAction().equals(DownloadFileObserver.ACTION_SCAN)) {
                File file = (File) intent.getSerializableExtra("file");
                if (file == null) {
                    Log.e(TAG, "onReceive null file extra");
                } else {
                    enqueueFile(file);
                }
            }
        }
    };

    @Nullable @Override public IBinder onBind(final Intent intent) {
        return new Callback();
    }

    private class Callback extends Binder {
        @Override
        protected boolean onTransact(final int code, final Parcel data, final Parcel reply, final
        int flags) throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    }


    private class UIHandler extends Handler {
        public static final int MSG_OPEN_NOTIFICATION = 0;
        public static final int MSG_CLOSE_NOTIFICATION = 1;
        public static final int MSG_SCAN_FAIL = 10;
        public static final int MSG_SCAN_SAFE = 11;
        public static final int MSG_SCAN_QUEUE = 12;
        public static final int MSG_SCAN_DANDER = 13;
        public static final int MSG_SCAN_QUERY_FAIL = 14;
        public static final int MSG_SCAN_QUERY_SAFE = 15;
        public static final int MSG_SCAN_QUERY_DANDER = 16;
        public static final int MSG_SCAN_SOLVE = 17;

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
        public static final int NOTIFY_RUN = 11;
        public static final int NOTIFY_PAUSE = 12;

        private void updateVirusNotification() {
            if (!notificationEnable) { return; }
            FileScannerService.this.startForeground(NOTIFY_RUN, getVirusNotification(sum, danger,
                    solved, queue));
        }

        private Notification getVirusNotification(int sum, int danger, int solved, int queue) {
            Intent virus = new Intent(FileScannerService.this, ActivityMain.class);//TODO create new
            // act
            PendingIntent pi = PendingIntent.getActivity(FileScannerService.this, 0, virus,
                    PendingIntent
                            .FLAG_UPDATE_CURRENT);

            TypedValue tv = new TypedValue();
            FileScannerService.this.getTheme().resolveAttribute(R.attr.colorPrimary, tv, true);
            NotificationCompat.Builder builder = new NotificationCompat.Builder
                    (FileScannerService.this)
                    .setSmallIcon(R.drawable.ic_data_protection)
                    .setContentIntent(pi)
                    .setColor(tv.data)
                    .setOngoing(true)//TODO allow dismiss
                    .setAutoCancel(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setCategory(Notification.CATEGORY_SERVICE)
                        .setVisibility(Notification.VISIBILITY_PUBLIC);
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                builder.setContentTitle(FileScannerService.this.getString(R.string.entry_virus));
            }


            String s1;
            final String s2 = FileScannerService.this.getString(R.string.msg_virus_protect_stats,
                    sum, queue,
                    solved);

            if (danger > 0) {
                s1 = FileScannerService.this.getString(R.string.msg_virus_protect_danger, danger);
                builder.setPriority(Notification.PRIORITY_MAX);
                //TODO change icon to red, or !,  add sound

            } else if (queue > 0) {
                s1 = FileScannerService.this.getString(queue == 1 ? R.string
                        .msg_virus_protect_queue_one : R.string
                        .msg_virus_protect_queue_some);
                builder.setPriority(Notification.PRIORITY_DEFAULT);
            } else {
                s1 = FileScannerService.this.getString(R.string.msg_virus_protect_safe);
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
                    FileScannerService.this.stopForeground(true);
                    notificationEnable = false;
                    break;
                case MSG_SCAN_QUERY_SAFE:
                    queue--;
                case MSG_SCAN_SAFE:
                    sum++;
                    break;
                case MSG_SCAN_QUERY_DANDER:
                    queue--;
                case MSG_SCAN_DANDER:
                    sum++;
                    danger++;
                    //TODO ask user to handle
                    break;
                case MSG_SCAN_QUERY_FAIL:
                    queue--;
                case MSG_SCAN_FAIL:
                    sum++;
                    break;
                case MSG_SCAN_SOLVE:
                    danger--;
                    solved++;
                    break;
                case MSG_SCAN_QUEUE:
                    // sum++; in queue not count in sum
                    queue++;
                    break;
            }
            updateVirusNotification();
        }
    }

    private class ScanHandler extends Handler {
        private static final String TAG = "ScanHandler";

        public static final int MSG_WHAT_QUEUE = 0;
        public static final int MSG_WHAT_QUERY = 1;
        public static final int QUERY_DELAY_MILLIS = 1000 * 5;
        private MetaDefenderAPI api = RetrofitFactory.getMetaDefenderAPI();

        ScanHandler(final Looper looper) {
            super(looper);
        }

        private void handleQuery(Scan oldScan) {
            Log.i(TAG, String.format(" handleQuery scan: %s", oldScan));

            if (oldScan == null) {
                Log.e(TAG, " msg with null Scan obj");
                return;
            }


            Message m = mUIHandler.obtainMessage();
            try {
                Scan scan = queryScan(oldScan);

                if (BuildConfig.DEBUG) {
                    //noinspection ConstantConditions
                    assert scan != null;
                }

                m.obj = scan;
                // TODO write to database

                Log.i(TAG, String.format("scan status type %s, file %s", scan.which(), scan.file));
                switch (scan.which()) {
                    case SkipLarge:
                    case SkipSafe:
                    case Safe:
                        m.what = UIHandler.MSG_SCAN_QUERY_SAFE;
                        mUIHandler.sendMessage(m);
                        break;
                    case Danger:
                        m.what = UIHandler.MSG_SCAN_QUERY_DANDER;
                        mUIHandler.sendMessage(m);
                        break;
                    case Queue:
                        // not send queue msg
                        final Message msg = mScanHandler.obtainMessage(MSG_WHAT_QUERY);
                        msg.obj = scan;
                        mScanHandler.postDelayed(new Runnable() {
                            @Override public void run() {
                                mScanHandler.sendMessage(msg);
                            }
                        }, QUERY_DELAY_MILLIS);
                        break;
                }
            } catch (IOException | ScanException e) {
                m.what = UIHandler.MSG_SCAN_QUERY_FAIL;
                m.obj = oldScan;
                e.printStackTrace();
                Log.e(TAG, "error when query", e);
                mUIHandler.sendMessage(m);
            }
        }

        private Scan queryScan(final Scan oldScan) throws
                ScanException, IOException {
            File file = oldScan.file;
            String restIp = oldScan.restIp;
            String dataId = oldScan.dataId;

            String url = String.format("https://%s/file/%s", restIp, dataId);

            Response<Scan> resp = api.queryScan(url).execute();
            if (resp.isSuccessful()) {
                Scan newScanResult = resp.body();
                if (newScanResult == null) {
                    throw new ScanException("null scan result returned by queryScan");
                } else {
                    // if this scan complete, the result will be non-inqueue
                    // copy because query result does not contain this
                    newScanResult.restIp = restIp;
                    newScanResult.dataId = dataId;
                    return newScanResult.file(file).auto();
                }
            } else if (resp.code() == 403) {
                throw new ScanAPIExceededException();
            } else { throw new ScanHTTPException(resp); }
        }

        private void handleQueue(@Nullable final File file) {
            Log.i(TAG, String.format(" handleQueue file: %s", file));

            if (file == null) {
                Log.e(TAG, " msg with null file obj");
                return;
            }

            Message m = mUIHandler.obtainMessage();

            try {
                Scan scan = localScan(file);
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

                Log.i(TAG, String.format("scan status type %s, file %s", scan.which(), scan.file));
                switch (scan.which()) {
                    case SkipLarge:
                    case SkipSafe:
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
                        }, QUERY_DELAY_MILLIS);
                        break;
                }
            } catch (IOException | ScanException e) {
                m.what = UIHandler.MSG_SCAN_FAIL;
                m.obj = Scan.errorStubQueryResult(e, file);
                Log.e(TAG, "error when scan", e);
            }
            mUIHandler.sendMessage(m);
        }

        @Nullable
        private Scan hashScan(File file) throws ScanException, IOException {
            String sha1 = Util.sha1(file);

            // official test case
            if (BuildConfig.DEBUG) {
                if (file.getName().contains("hash_yyz_test")) {
                    sha1 = "E71A6D8760B37E45FA09D3E1E67E2CD3";
                } else if (file.getName().contains("api_yyz_test")) {
                    return null;
                }
            }

            Response<Scan> resp = api.hashLookUp(sha1).execute();
            Log.i(TAG, String.format("hashLookUp file: %s sha1: %s resp: %s", file, sha1,
                    resp));

            if (resp.isSuccessful()) {
                Scan body = resp.body();
                if (body != null) {
                    try {
                        body.file(file).auto();
                    } catch (ScanException e) {
                        // if no hash found, gson return an empty result instead null
                        // and cause unable t get type automatically
                        return null;
                    }
                }
                return body;
            } else if (resp.code() == 403) {
                throw new ScanAPIExceededException();
            } else {
                throw new ScanHTTPException(resp);
            }
        }

        @NonNull
        private Scan uploadScan(File file) throws ScanException, IOException {
            if (file.length() > 140 * 1000 * 1000) {
                return Scan.skipLargeFile(file);
            }

            RequestBody body = RequestBody.create(MediaType.parse("file"), file);

            Response<Scan> resp = api.uploadScan(file.getName(), body).execute();
            Log.i(TAG, String.format("uploadLookup file: %s", file));

            if (resp.isSuccessful()) {
                Scan scanResult = resp.body();
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
        private Scan localScan(File file) {
            if (BuildConfig.DEBUG && file.getName().contains("yyz_test")) {
                return null;
            }

            if (file.length() > 0) {
                return null;
            } else {
                return Scan.skipSafeFile(file);
            }
        }

        @Override public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_WHAT_QUERY:
                    Scan scan = (Scan) msg.obj;
                    handleQuery(scan);
                    break;
                case MSG_WHAT_QUEUE:
                    final File file = (File) msg.obj;
                    handleQueue(file);
                    break;
            }


        }


    }

}
