package at.rovo.awsxray.security;

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.spi.HeaderFilterStrategy;

/**
 * Whitelist of jetty http response headers allowed to be returned.
 */
public class AuthFilterStrategy implements HeaderFilterStrategy
{

  public static final String ID = "authFilterStrategy";

  protected final List<String> allowedHeaders = Lists.newArrayList(
    "accept",
    "accept-encoding",
    "accept-language",
    "cache-control",
    "authorization",
    "content-length",
    "upgrade-insecure-requests",
    "www-authenticate"
  );

  @Override
  public boolean applyFilterToCamelHeaders(String headerName, Object headerValue, Exchange exchange) {
    return !isAllowed(headerName);
  }

  @Override
  public boolean applyFilterToExternalHeaders(String headerName, Object headerValue, Exchange exchange) {
    return !isAllowed(headerName);
  }

  protected boolean isAllowed(String headerName) {
    return Iterables.tryFind(allowedHeaders, Predicates.containsPattern(Strings.nullToEmpty(headerName).toLowerCase())).isPresent();
  }
}
