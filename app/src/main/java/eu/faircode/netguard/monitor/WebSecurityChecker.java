package eu.faircode.netguard.monitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import retrofit2.Response;

/**
 * Created by Carlos on 4/21/17.
 */

public class WebSecurityChecker implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "WebSecurityChecker";
    private SharedPreferences mAllowMap;
    private boolean enable = false;

    public WebSecurityChecker(Context context) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);
        mAllowMap = context.getApplicationContext().getSharedPreferences("webCheckerHis", Context
                .MODE_PRIVATE);
        enable = preferences.getBoolean("item_web_switch0", false);
    }

    public boolean isEnabled() {
        return enable;
    }

    public boolean isAddressAllowed(final String ip) {
        try {
            Log.i(TAG, String.format("check ip allowed, ip %s", ip));
            if (mAllowMap.contains(ip)) {
                Log.i(TAG, String.format("use local result, ip %s", ip));
                return mAllowMap.getBoolean(ip, true);
            }

            Log.i(TAG, String.format("get network result, ip %s", ip));
            Response<IPReputationQueryResult> resp = RetrofitFactory.getMetaDefenderAPI()
                    .queryIp(ip).execute();
            if (resp.isSuccessful()) {
                IPReputationQueryResult result = resp.body();
                Log.i(TAG, String.format("IPReputation success, resp %s, result %s", resp, result));
                if (result != null && result.data != null) {
                    if (result.data.scanResults != null) {
                        IPReputationQueryResult.ScanResults[] scanResults = result.data.scanResults;
                        for (IPReputationQueryResult.ScanResults scanResult : scanResults) {
                            if (scanResult.results != null) {
                                for (IPReputationQueryResult.Result re : scanResult.results) {
                                    String va = re.result;
                                    if (va == null) { continue; }
                                    if (va.equals("BLACKLISTED")) {
                                        mAllowMap.edit().putBoolean(ip, false).apply();
                                        return false;
                                    } else {
                                        mAllowMap.edit().putBoolean(ip, true).apply();
                                        return true;
                                    }
                                }
                            }
                        }
                        // empty
                        mAllowMap.edit().putBoolean(ip, true).apply();
                        return true;
                    }
                }
            } else {
                Log.e(TAG, String.format("IPReputation fail, resp %s", resp));
            }
        } catch (Throwable e) {
            Log.e(TAG, "isAddressAllowed throw error", e);
            e.printStackTrace();
        }
        return true;
    }

    public boolean isDomainBlocked(String name) {
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String
            key) {
        if (key.equals("item_web_switch0")) {
            enable = sharedPreferences.getBoolean("item_web_switch0", false);
            Log.i(TAG, String.format("WebSecurityChecker enabled %s", enable));
            return;
        }
    }
}
