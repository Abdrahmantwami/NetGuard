package eu.faircode.netguard.monitor;

import android.annotation.TargetApi;
import android.os.Build;

import retrofit2.Response;

/**
 * Created by Carlos on 4/6/17.
 */

public class ScanException extends Exception {
    public ScanException() {
    }

    public ScanException(final String message) {
        super(message);
    }

    public ScanException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ScanException(final Throwable cause) {
        super(cause);
    }

    @TargetApi(Build.VERSION_CODES.N)
    public ScanException(final String message, final Throwable cause, final boolean
            enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

class ScanAPIExceededException extends ScanException {
    public ScanAPIExceededException() {
        super("API usage exceeded.");
    }
}

class ScanHTTPException extends ScanException {
    public ScanHTTPException(Response<Scan> response) {
        super(String.format("HTTP error, resp: %s", response.toString()));
    }
}

