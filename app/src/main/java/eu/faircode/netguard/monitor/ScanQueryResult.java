package eu.faircode.netguard.monitor;

import java.io.File;

/**
 * Created by Carlos on 4/4/17.
 */

public class ScanQueryResult {
    public static final int TYPE_LOCAL_SAFE = 0;
    public static final int TYPE_HASH = 1;
    public static final int TYPE_UPLOAD = 2;
    public ScanResults scanResults;
    public FileInfo fileInfo;
    public String FileId;
    public String dataId;
    public int type;


    public ScanQueryResult() {
    }

    public static ScanQueryResult safeSkipScan(File file) {
        ScanQueryResult result = new ScanQueryResult();
        result.type = TYPE_LOCAL_SAFE;
        result.fileInfo = new FileInfo();
        result.fileInfo.file = file;
        return result;
    }

    public boolean isSafe() {
        return this.type == TYPE_LOCAL_SAFE || this.scanResults.scanAllResultI > 0;
    }

    public static class ScanResults {
        public int scanAllResultI;
        public int totalAvs;
        public int totalDetectedAvs;

    }

    public static class FileInfo {
        public String displayName;
        public String fileTypeExtension;
        public File file;
    }
}
