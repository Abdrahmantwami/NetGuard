package eu.faircode.netguard;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import eu.faircode.netguard.monitor.Scan;

/**
 * Created by Carlos on 4/18/17.
 */

public class ORMDatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String TAG = "ORMDatabaseHelper";

    public ORMDatabaseHelper(final Context context) {
        super(context, "scan", null, 1);
    }

    @Override
    public void onCreate(final SQLiteDatabase sqLiteDatabase, final ConnectionSource
            connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Scan.class);
        } catch (SQLException e) {
            Log.e(TAG, "sql ex when create table", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final ConnectionSource
            connectionSource, final int i, final int i1) {
    }
}
