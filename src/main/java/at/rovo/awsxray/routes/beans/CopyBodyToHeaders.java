package at.rovo.awsxray.routes.beans;

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;

/**
 * Copies the received forward response to the headers in order to prevent the actual message content
 * from being overwritten by the received response
 */
public class CopyBodyToHeaders
{

  @Handler
  public byte[] copyBodyToHeaders(Exchange exchange) {
    Map<String, Object> headers = exchange.getIn().getHeaders();
    byte[] body = exchange.getIn().getBody(byte[].class);
    if (body != null) {
      headers.put("HTTP_RESPONSE_CONTENT", new String(body));
    }
    return body;
  }
}
