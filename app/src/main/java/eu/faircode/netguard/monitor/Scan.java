package eu.faircode.netguard.monitor;

import com.google.gson.annotations.Expose;

import java.io.File;

/**
 * Created by Carlos on 4/13/17.
 */

public class Scan {
    private Scan.Type type;
    public Exception e;
    public File file;


    public Type which() {
        return type;
    }


    @Expose public String dataId;

    // for upload scan
    @Expose public int inQueue = -1;
    @Expose public String restIp;

    @Expose public int scanAllResultI;
    @Expose public int progressPercentage = -1;
    @Expose public int totalAvs;
    @Expose public int totalDetectedAvs;


    @Expose public String displayName;
    @Expose public String fileTypeExtension;

    public Scan() {
        // keep for ORMLite
    }

    public static Scan skipSafeFile(File file) {
        Scan result = new Scan();
        result.type = Scan.Type.SkipSafe;
        result.file = file;
        return result;
    }

    public static Scan skipLargeFile(File file) {
        Scan result = new Scan();
        result.type = Scan.Type.SkipLarge;
        result.file = file;
        return result;
    }

    public static Scan errorStubQueryResult(Exception e, File file) {
        Scan result = new Scan();
        result.type = Scan.Type.Stub;
        result.e = e;
        result.file = file;
        return result;
    }

    Scan(ScanQueryResult scanQueryResult) {
        this.dataId = scanQueryResult.dataId;
        this.inQueue = scanQueryResult.inQueue;
        this.restIp = scanQueryResult.restIp;

        this.scanAllResultI = scanQueryResult.scanResults.scanAllResultI;
        this.progressPercentage = scanQueryResult.scanResults.progressPercentage;
        this.totalAvs = scanQueryResult.scanResults.totalAvs;
        this.totalDetectedAvs = scanQueryResult.scanResults.totalDetectedAvs;

        this.displayName = scanQueryResult.fileInfo.displayName;
        this.fileTypeExtension = scanQueryResult.fileInfo.fileTypeExtension;
    }

    public Scan file(File file) {
        this.file = file;
        return this;
    }


    public Scan auto() throws ScanException {
        if (this.type == Scan.Type.SkipSafe || type == Scan.Type.SkipLarge) {
            return this;
        }

        if (progressPercentage == 100) {
            if (scanAllResultI > 0) {
                type = Scan.Type.Danger;
            } else {
                type = Scan.Type.Safe;
            }
            return this;
        } else {
            type = Scan.Type.Queue;
            return null;
        }
        //
        // if (inQueue > 0) {
        //     this.type = Scan.Type.Queue;
        // } else { throw new ScanException("unknown type"); }//  this will happen when hash look
        // // found nothing
    }

    public enum Type {SkipLarge, SkipSafe, Safe, Queue, Danger, Stub}
}
