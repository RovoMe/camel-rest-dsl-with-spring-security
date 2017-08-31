package at.rovo.awsxray.exceptions;

import at.rovo.awsxray.utils.ErrorCodes;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.lang.invoke.MethodHandles;
import lombok.Getter;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Any failure returned by an external system which we forwarded messages to. The failure either
 * occurred during the forwarding or while the callback was invoked.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalMessagingException extends Exception {

  private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String errorCode;
  private String reason;
  private String details;

  public ExternalMessagingException() {
    super();
  }

  public ExternalMessagingException(String errorCode, String reason) {
    super(errorCode + " " + reason);
    this.errorCode = errorCode;
    this.reason = reason;
  }

  public ExternalMessagingException(String errorCode, String reason, String details) {
    super(errorCode + " | Reason: " + reason + " | Details: " + details);
    this.errorCode = errorCode;
    this.reason = reason;
    this.details = details;
  }

  public ExternalMessagingException(HttpOperationFailedException cause) {
    super(cause.getStatusText(), cause);
    this.errorCode = ""+ErrorCodes.EXTERNAL_REPORTED_FAILURE.getErrorCode();
    this.reason = ErrorCodes.EXTERNAL_REPORTED_FAILURE.getDescription();
    this.details = cause.getResponseBody();
  }

  public String getErrorCode() {
    return this.errorCode;
  }

  public String getReason() {
    return this.reason;
  }

  public String getDetails() {
    return this.details;
  }

  @Override
  public String toString() {
    String s = getClass().getName();
    if (StringUtils.isNotBlank(details)) {
      return s + ": " + details;
    }
    String message = getLocalizedMessage();
    return (message != null) ? (s + ": " + message) : s;
  }
}
