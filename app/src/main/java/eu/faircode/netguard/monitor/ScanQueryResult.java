package eu.faircode.netguard.monitor;

import com.google.gson.annotations.Expose;

import java.io.File;

/**
 * Created by Carlos on 4/4/17.
 */

public class ScanQueryResult {
    @Expose public ScanResults scanResults;
    @Expose public FileInfo fileInfo;
    @Expose public String dataId;

    // for upload scan
    @Expose public int inQueue = -1;
    @Expose public String restIp;


    public ScanQueryResult() {
    }

    public static class ScanResults {
        @Expose public int scanAllResultI;
        @Expose public int progressPercentage = -1;
        @Expose public int totalAvs;
        @Expose public int totalDetectedAvs;

    }

    public static class FileInfo {
        @Expose public String displayName;
        @Expose public String fileTypeExtension;
        public File file;
    }
}
