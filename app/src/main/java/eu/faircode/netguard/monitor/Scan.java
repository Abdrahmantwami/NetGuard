package eu.faircode.netguard.monitor;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.File;

/**
 * Created by Carlos on 4/13/17.
 */
@DatabaseTable
public class Scan {
    @DatabaseField private Scan.Type type;
    public Exception e;
    @DatabaseField public String path;


    public Type which() {
        return type;
    }


    @DatabaseField
    @Expose
    public String dataId;

    // for upload scan
    @DatabaseField
    @Expose public int inQueue = -1;
    @DatabaseField
    @Expose public String restIp;

    @DatabaseField
    @Expose public int scanAllResultI;
    @DatabaseField
    @Expose public int progressPercentage = -1;

    @Override public String toString() {
        return "Scan{" +
                "type=" + type +
                ", e=" + e +
                ", path='" + path + '\'' +
                ", dataId='" + dataId + '\'' +
                ", inQueue=" + inQueue +
                ", restIp='" + restIp + '\'' +
                ", scanAllResultI=" + scanAllResultI +
                ", progressPercentage=" + progressPercentage +
                ", totalAvs=" + totalAvs +
                ", totalDetectedAvs=" + totalDetectedAvs +
                ", displayName='" + displayName + '\'' +
                ", fileTypeExtension='" + fileTypeExtension + '\'' +
                ", time=" + time +
                '}';
    }

    @DatabaseField
    @Expose public int totalAvs;
    @DatabaseField
    @Expose public int totalDetectedAvs;


    @DatabaseField
    @Expose public String displayName;
    @DatabaseField
    @Expose public String fileTypeExtension;
    @DatabaseField(id = true) public long time = System.currentTimeMillis();

    public Scan() {
        // keep for ORMLite
    }

    public static Scan skipSafeFile(File file) {
        Scan result = new Scan();
        result.type = Scan.Type.SkipSafe;
        result.file(file);
        return result;
    }

    public static Scan skipLargeFile(File file) {
        Scan result = new Scan();
        result.type = Scan.Type.SkipLarge;
        result.file(file);
        return result;
    }

    public static Scan errorStubQueryResult(Exception e, File file) {
        Scan result = new Scan();
        result.type = Scan.Type.Stub;
        result.e = e;
        result.file(file);
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
        this.path = file.getPath();
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
