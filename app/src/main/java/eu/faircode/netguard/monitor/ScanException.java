package eu.faircode.netguard.monitor;

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

    public ScanException(final String message, final Throwable cause, final boolean
            enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

class APIExceededException extends ScanException {
    public APIExceededException() {
        super("API usage exceeded.");
    }
}

class HTTPException extends ScanException {
    public HTTPException(Response<ScanQueryResult> response) {
        super(String.format("HTTP error, resp: %s", response.toString()));
    }
}

