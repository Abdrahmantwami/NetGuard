package eu.faircode.netguard.monitor;

import java.util.Arrays;

/**
 * Created by Carlos on 4/21/17.
 */

public class IPReputationQueryResult {
    public Data data;

    @Override public String toString() {
        return "IPReputationQueryResult{" + data +
                '}';
    }

    public static class Data {
        public String address;

        @Override public String toString() {
            return "address='" + address + '\'' +
                    ", scanResults=" + Arrays.toString(scanResults);
        }

        public ScanResults[] scanResults;
    }

    public static class ScanResults {
        public String source;
        public Result[] results;

        @Override public String toString() {
            return "ScanResults{" +
                    "source='" + source + '\'' +
                    ", results=" + Arrays.toString(results) +
                    '}';
        }
    }

    public static class Result {
        public String confident;
        public String result;// blacklisted, whitelisted, unknown.

        @Override public String toString() {
            return "Result{" +
                    "confident='" + confident + '\'' +
                    ", result='" + result + '\'' +
                    '}';
        }
    }
}
