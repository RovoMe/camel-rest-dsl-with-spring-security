package at.rovo.awsxray.routes.beans;

import at.rovo.awsxray.exceptions.APIException;
import at.rovo.awsxray.exceptions.MissingAuthHeaderException;
import java.lang.invoke.MethodHandles;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.Message;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class PrepareErrorResponse {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Handler
    public void doWork(Exchange exchange) throws Exception {

        Throwable cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);

        LOG.debug("Handling exception {}", cause.getClass().getSimpleName());
        if (cause instanceof APIException) {
            APIException apiEx = (APIException)cause;

            Message msg = exchange.getOut();
            msg.setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            msg.setHeader(Exchange.CHARSET_NAME, "UTF-8");
            msg.setFault(false);

            if (apiEx instanceof MissingAuthHeaderException) {
                msg.setHeader(Exchange.HTTP_RESPONSE_CODE, 401);
                msg.setHeader("WWW-Authenticate", "Basic realm=\"hub-services\"");
                msg.setBody("");
            }  else {
                msg.setHeader(Exchange.HTTP_RESPONSE_CODE, apiEx.getError().getErrorCode());
                JSONObject response = convertException(apiEx);
                msg.setBody(response.toString());
                LOG.info("Set error message for caught API exception ('{}'). Message returned is: '{}'",
                         apiEx.getLocalizedMessage(), response);
            }
        } else {
            sendInternalServerError(exchange, cause);
        }
    }

    private JSONObject convertException(APIException apiEx) throws Exception {
        JSONObject failure = new JSONObject();
        failure.put("reason", apiEx.getError().getDescription());
        failure.put("details", apiEx.getMessage());

        JSONObject response = new JSONObject();
        response.put("failure", failure);
        return response;
    }

    private void sendInternalServerError(Exchange exchange, Throwable cause) throws Exception {
        JSONObject failure = new JSONObject();
        failure.put("reason", "INTERNAL_SERVER_ERROR");
        failure.put("details", cause.getLocalizedMessage());

        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put("failure", failure);

        Message msg = exchange.getOut();
        msg.setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        msg.setHeader(Exchange.CHARSET_NAME, "UTF-8");
        msg.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        msg.setBody(jsonMessage.toString());
        msg.setFault(true);

        LOG.error("Set error message for none messaging exception '"
                  + cause.getLocalizedMessage() + "': Message returned is: '" + jsonMessage + "'",
                  cause);
    }
}
