package at.rovo.awsxray.utils;

public enum ErrorCodes {

    EXTERNAL_REPORTED_FAILURE(400, "Some problems were noticed while processing an external resource"),
    AUTHENTICATION_FAILURE(401, "Authentication credentials are missing"),
    USER_NOT_PERMITTED(403, "Insufficient permissions to perform requested action"),
    FILE_NOT_FOUND(404, "Requested file does not exist");

    private int errorCode;
    private String description;

    ErrorCodes(int errorCode, String description) {
        this.errorCode = errorCode;
        this.description = description;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getDescription() {
        return this.description;
    }
}
