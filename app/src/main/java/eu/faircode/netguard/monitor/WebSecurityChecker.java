package eu.faircode.netguard.monitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashMap;

import retrofit2.Response;

/**
 * Created by Carlos on 4/21/17.
 */

public class WebSecurityChecker implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "WebSecurityChecker";
    private static HashMap<String, Boolean> mAllowMap = new HashMap<>();
    private boolean enable = false;

    public WebSecurityChecker(Context context) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    public boolean isEnabled() {
        return enable;
    }

    public boolean isAddressAllowed(final String ip) {
        try {
            Log.i(TAG, String.format("check ip allowed, ip %s", ip));
            Boolean b = mAllowMap.get(ip);
            if (b != null) {
                return b;
            }

            Response<IPReputationQueryResult> resp = RetrofitFactory.getMetaDefenderAPI()
                    .queryIp(ip).execute();
            if (resp.isSuccessful()) {
                IPReputationQueryResult result = resp.body();
                if (result == null || result.data == null) {
                    Log.e(TAG, String.format("return null when query ip, resp %s, result %s",
                            resp, result));
                } else if (result.data.scanResults != null) {
                    IPReputationQueryResult.ScanResults[] scanResults = result.data.scanResults;
                    for (IPReputationQueryResult.ScanResults scanResult : scanResults) {
                        if (scanResult.results != null) {
                            for (IPReputationQueryResult.Result re : scanResult.results) {
                                String va = re.result;
                                if (va == null) { continue; }
                                if (va.equals("BLACKLISTED")) {
                                    mAllowMap.put(ip, false);
                                    return false;
                                } else {
                                    mAllowMap.put(ip, true);
                                    return true;
                                }
                            }
                        }
                    }
                    // empty
                    mAllowMap.put(ip, true);
                    return true;

                }
            } else {
                Log.e(TAG, String.format("fail when query ip, resp %s", resp));
            }
        } catch (Throwable e) {
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
        if (key.equals("item_web_switch1")) {
            enable = sharedPreferences.getBoolean("item_web_switch1", false);
            Log.i(TAG, "WebSecurityChecker is enabled");
            return;
        }
    }
}
