package eu.faircode.netguard.monitor;

import com.google.gson.annotations.Expose;

import java.io.File;

/**
 * Created by Carlos on 4/4/17.
 */

public class ScanQueryResult {
    private Type type;
    public Exception e;

    public enum Type {SkipLarge, SkipSafe, Safe, Queue, Danger, Stub}

    public Type which() {
        return type;
    }


    @Expose public ScanResults scanResults;
    @Expose public FileInfo fileInfo;
    @Expose public String dataId;

    // for upload scan
    @Expose public int inQueue = -1;
    @Expose public String restIp;


    public ScanQueryResult() {
    }

    public static ScanQueryResult skipSafeFile(File file) {
        ScanQueryResult result = new ScanQueryResult();
        result.type = Type.SkipSafe;
        result.fileInfo = new FileInfo();
        result.fileInfo.file = file;
        return result;
    }

    public static ScanQueryResult skipLargeFile(File file) {
        ScanQueryResult result = new ScanQueryResult();
        result.type = Type.SkipLarge;
        result.fileInfo = new FileInfo();
        result.fileInfo.file = file;
        return result;
    }

    public static ScanQueryResult errorStubQueryResult(Exception e, File file) {
        ScanQueryResult result = new ScanQueryResult();
        result.type = Type.Stub;
        result.e = e;
        result.file(file);
        return result;
    }

    public ScanQueryResult file(File file) {
        if (this.fileInfo == null) {
            this.fileInfo = new FileInfo();
        }
        fileInfo.file = file;
        return this;
    }

    public ScanQueryResult type(Type type) {
        this.type = type;
        return this;
    }

    public ScanQueryResult auto() throws ScanException {
        if (this.type == Type.SkipSafe || type == Type.SkipLarge) {
            return this;
        }

        if (this.scanResults != null) {
            if (scanResults.progressPercentage == 100) {
                if (this.scanResults.scanAllResultI > 0) {
                    type = Type.Danger;
                } else if (this.scanResults.scanAllResultI == 0) {
                    type = Type.Safe;
                }
            } else {
                type = Type.Queue;
            }
        } else if (inQueue > 0) {
            this.type = Type.Queue;
        } else { throw new ScanException("unknown type"); }//  this will happen when hash look
        // found nothing

        return this;
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
