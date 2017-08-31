package at.rovo.awsxray.routes;

import at.rovo.awsxray.exceptions.ExternalMessagingException;
import at.rovo.awsxray.routes.beans.CopyBodyToHeaders;
import java.lang.invoke.MethodHandles;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static at.rovo.awsxray.utils.MaskingConverter.CONFIDENTIAL;

public class HttpInvokerRoute extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private int redeliveryDelay = 30000;

    public HttpInvokerRoute() {
        redeliveryDelay = 30000;
    }

    public HttpInvokerRoute(int redeliveryDelay) {
        this.redeliveryDelay = redeliveryDelay;
    }

    @Override
    public void configure() throws Exception {

        /*
         * The HTTP client throws HTTP Operation Failed exceptions if the service returned some errors.
         * The error itself indicates an irrecoverable error so there is no reason to try again
         */
        onException(ExternalMessagingException.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "error reported: ${exception.details} - can not recover from this");

        /*
         * In case the host is not reachable try to forward the message 3 times with a delay of 30
         * seconds per retry
         */
        onException(UnknownHostException.class)
                .handled(true)
                .logRetryAttempted(true)
                .redeliveryDelay(redeliveryDelay)
                .maximumRedeliveries(3)
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .log(LoggingLevel.ERROR, "Could not reach host ${exception.message}");

        from("direct:http-invoke")
                .routeId("HttpInvoker")

                .log(LoggingLevel.INFO, LOG, CONFIDENTIAL, "Invoking external URL: ${header[EXTERNAL_URL]}")
                // forward the message to the remote host
                .recipientList(header("EXTERNAL_URL"))

                .log(LoggingLevel.DEBUG, "HTTP response code: ${header[" + Exchange.HTTP_RESPONSE_CODE + "]}")

                .bean(CopyBodyToHeaders.class)
                // In order to get the error cause contained in the response, we must not automatically
                // throw an exception on failure but instead handle the response our-self and then throw an
                // appropriate exception.
                .choice()
                    .when(header(Exchange.HTTP_RESPONSE_CODE).isGreaterThanOrEqualTo(300))
                        // log the exact error as reported by the service and rethrow the error. This is
                        // necessary as the exception will later on not have any access to the message body
                        // returned by the service if an exception is thrown on errors.
                        .process((Exchange exchange) ->
                         {
                             String url = exchange.getIn().getHeader("EXTERNAL_URL").toString();
                             int statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
                             String statusText = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_TEXT, String.class);
                             String responseBody = exchange.getIn().getBody(String.class);
                             LOG.debug("Received invocation response code {} {} - body was {}",
                                       statusCode, statusText, responseBody);
                             Map<String, String> responseHeaders = new HashMap<>();

                             HttpOperationFailedException error;
                             try {
                                 JSONObject response = new JSONObject(responseBody);
                                 error = new HttpOperationFailedException(url, statusCode, statusText,
                                                                          null, responseHeaders,
                                                                          response.toString());
                             } catch (JSONException jsonEx) {
                                 error = new HttpOperationFailedException(url, statusCode, statusText,
                                                                          null, responseHeaders,
                                                                          responseBody);
                             }
                             throw new ExternalMessagingException(error);
                         })
                    .otherwise()
                        .log("Remote service responded with ${in.header.CamelHttpResponseCode} ${in.header.CamelHttpResponseText}")
                        .process((Exchange exchange) -> exchange.getIn().setBody(null));
    }
}
