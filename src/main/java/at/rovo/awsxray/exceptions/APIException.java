package at.rovo.awsxray.exceptions;

import at.rovo.awsxray.utils.ErrorCodes;

public class APIException extends Exception {
    protected ErrorCodes error;

    public APIException(String msg) {
        super(msg);
    }

    public APIException(ErrorCodes error, String msg) {
        super(msg);
        this.error = error;
    }

    public APIException(String msg, Throwable t) {
        super(msg, t);
    }

    public APIException(ErrorCodes error, String msg, Throwable t) {
        super(msg, t);
        this.error = error;
    }

    public ErrorCodes getError() {
        return this.error;
    }
}
