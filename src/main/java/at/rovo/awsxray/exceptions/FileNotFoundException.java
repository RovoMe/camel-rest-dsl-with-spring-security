package at.rovo.awsxray.exceptions;

import at.rovo.awsxray.utils.ErrorCodes;

public class FileNotFoundException extends APIException{

    public FileNotFoundException(String error)
    {
        super(ErrorCodes.FILE_NOT_FOUND, error);
    }
}
