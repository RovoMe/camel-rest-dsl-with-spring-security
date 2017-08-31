package at.rovo.awsxray.security;

import at.rovo.awsxray.exceptions.APIException;
import at.rovo.awsxray.exceptions.MissingAuthHeaderException;
import at.rovo.awsxray.utils.HttpUtil;
import javax.security.auth.Subject;
import org.apache.camel.Exchange;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A camel processor for decoding the HTTP basic authentication header named
 * 'Authorization' and setting up a {@link UsernamePasswordAuthenticationToken}
 * for spring-security authentication support.
 */
public abstract class AbstractSpringSecurityContextLoader {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractSpringSecurityContextLoader.class);

  protected void handleRequest(String authHeader, Exchange exchange, AuthenticationProvider authProvider)
      throws APIException {

    if (authProvider == null) {
      LOG.warn("No authentication provider available");
        LOG.error("Could not reload authentication provider! Something strange is going on ...");
        throw new IllegalArgumentException(
                "An error occurred while trying to authenticate the requesting user with the service.");
    }

    if (authHeader == null) {
      LOG.debug("No authorization header set");
      throw new MissingAuthHeaderException("Failed to decode basic authentication token");
    }

    String[] usernameAndPassword = HttpUtil.decodeBasicAuthHeader(authHeader);
    if (usernameAndPassword == null || usernameAndPassword.length != 2) {
      LOG.warn("No username and password found in authorization header. This might indicate a missing authorization header");
      throw new BadCredentialsException("Invalid user credentials found");
    } else {
      if (StringUtils.isBlank(usernameAndPassword[0])) {
        LOG.warn("Invalid username found");
        throw new BadCredentialsException("Invalid user credentials found");
      }
      if (StringUtils.isBlank(usernameAndPassword[1])) {
        LOG.warn("Invalid password found for user {}", usernameAndPassword[0]);
        throw new BadCredentialsException("Invalid user credentials found");
      }
    }
    LOG.info("Received authentication request for user: {}", usernameAndPassword[0]);
    UsernamePasswordAuthenticationToken authToken;
    try {
      authToken = handleAuthentication(usernameAndPassword, exchange, authProvider);
    } catch (Exception ex) {
      LOG.info("Authentication of user {} failed", usernameAndPassword[0]);
      throw ex;
    }

    LOG.info("Authentication of user {} succeeded", usernameAndPassword[0]);

    // wrap it in a Subject
    Subject subject = new Subject();
    subject.getPrincipals().add(authToken);

//    exchange.getIn().setHeader(Exchange.AUTHENTICATION, subject);

    // setup the security context with the authenticated token (needed by our ISecurityLayer service)
    SecurityContextHolder.getContext().setAuthentication(authToken);
  }

  /**
   * This method will perform the actual authentication against a backing logic.
   *
   * @param usernameAndPassword A string array containing the username and password issued to the
   *                            service
   * @param exchange            Camel's exchange
   * @return A {@link UsernamePasswordAuthenticationToken} containing the user credentials as well
   * as its assigned authority roles
   */
  protected UsernamePasswordAuthenticationToken handleAuthentication(String[] usernameAndPassword,
                                                                     Exchange exchange,
                                                                     AuthenticationProvider authProvider) {
    UsernamePasswordAuthenticationToken token =
        new UsernamePasswordAuthenticationToken(usernameAndPassword[0], usernameAndPassword[1]);
    return (UsernamePasswordAuthenticationToken) authProvider.authenticate(token);
  }
}
