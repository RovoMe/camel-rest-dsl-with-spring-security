package at.rovo.awsxray.exceptions;

import at.rovo.awsxray.utils.ErrorCodes;

/**
 * Thrown if no authentication header was present in the headers
 */
public class MissingAuthHeaderException extends APIException {

  public MissingAuthHeaderException(String error) {
    super(ErrorCodes.USER_NOT_PERMITTED, error);
  }
}
