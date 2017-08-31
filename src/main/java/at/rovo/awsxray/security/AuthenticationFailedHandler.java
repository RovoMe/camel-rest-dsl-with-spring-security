package at.rovo.awsxray.security;

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;

/**
 * Interface for handling jetty authentication exceptions
 */
public interface AuthenticationFailedHandler
{

  @Handler
  void processException(Map<String, Object> headers, Exchange exchange);

}
