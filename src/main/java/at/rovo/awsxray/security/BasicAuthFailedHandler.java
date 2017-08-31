package at.rovo.awsxray.security;

import at.rovo.awsxray.exceptions.MissingAuthHeaderException;
import at.rovo.awsxray.utils.ErrorCodes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.Headers;
import org.apache.camel.Message;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Responds with the correct basic authentication headers for basic-auth authentication
 * failures. This will make sure the browser shows the login window if the request is
 * coming from a browser.
 */
public class BasicAuthFailedHandler implements AuthenticationFailedHandler {

  private static final Logger LOG = LoggerFactory.getLogger(BasicAuthFailedHandler.class);

  private final static ObjectMapper mapper = new ObjectMapper();

  @Override
  @Handler
  public void processException(@Headers Map<String, Object> headers, Exchange exchange) {

    Throwable cause =  exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);

    if (cause != null) {
      LOG.info("Handling authentication failure based on exception {} for reason: {}",
               cause.getClass(), cause.getLocalizedMessage());
//      LOG.debug(cause.getLocalizedMessage(), cause);
    }

    Message msg = exchange.getIn();

    msg.setFault(false);

    if (cause instanceof MissingAuthHeaderException) {
      msg.setHeader(Exchange.HTTP_RESPONSE_CODE, 401);
      msg.setHeader("WWW-Authenticate", "Basic realm=\"hub-services\"");
      msg.setBody("");
    } else if (cause instanceof BadCredentialsException){

      JSONObject failure = new JSONObject();
      try {
        failure.put("errorCode", ErrorCodes.USER_NOT_PERMITTED.getErrorCode());
        failure.put("reason", ErrorCodes.USER_NOT_PERMITTED.getDescription());
        failure.put("details", cause.getLocalizedMessage());

        msg.setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        msg.setHeader(Exchange.HTTP_RESPONSE_CODE, 403);
        setFailureInResponse(msg, failure);
      } catch (JSONException jsonEx) {
        msg.setHeader(Exchange.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
        msg.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        msg.setFault(true);
        msg.setBody("Caught unexpected exception while processing an authentication failure. Reason: " + jsonEx.getLocalizedMessage());
      }
    } else {

      JSONObject failure = new JSONObject();
      try {
        failure.put("errorCode", ErrorCodes.USER_NOT_PERMITTED.getErrorCode());
        failure.put("reason", ErrorCodes.USER_NOT_PERMITTED.getDescription());
        failure.put("details", "Request not understood");

        msg.setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        msg.setHeader(Exchange.HTTP_RESPONSE_CODE, 403);
        setFailureInResponse(msg, failure);
      } catch (JSONException jsonEx) {
        msg.setHeader(Exchange.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
        msg.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        msg.setFault(true);
        msg.setBody("Caught unexpected exception while processing an authentication failure. Reason: " + jsonEx.getLocalizedMessage());
      }
    }
  }

  private void setFailureInResponse(Message msg, JSONObject failure) {
    try {
      JSONObject jsonMessage = new JSONObject();
      jsonMessage.put("failure", failure);
      msg.setBody(jsonMessage.toString());
    } catch (JSONException jsonEx) {
      String json;
      try {
        json = mapper.writeValueAsString(failure);
      } catch (JsonProcessingException jpEx) {
        json = failure.toString();
      }
      msg.setBody(json);
    }
  }
}
