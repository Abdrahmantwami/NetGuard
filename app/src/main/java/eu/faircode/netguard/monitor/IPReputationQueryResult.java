package eu.faircode.netguard.monitor;

/**
 * Created by Carlos on 4/21/17.
 */

public class IPReputationQueryResult {
    public Data data;


    public static class Data {
        public String address;
        public ScanResults[] scanResults;
    }

    public static class ScanResults {
        public String source;
        public Result[] results;
    }

    public static class Result {
        public String confident;
        public String result;// blacklisted, whitelisted, unknown.
    }
}
