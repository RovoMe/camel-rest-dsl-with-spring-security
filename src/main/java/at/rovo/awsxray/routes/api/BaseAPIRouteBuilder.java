package at.rovo.awsxray.routes.api;

import at.rovo.awsxray.exceptions.APIException;
import at.rovo.awsxray.routes.beans.PrepareErrorResponse;
import at.rovo.awsxray.security.BasicAuthFailedHandler;
import org.apache.camel.CamelAuthorizationException;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

public abstract class BaseAPIRouteBuilder extends RouteBuilder {

    /**
     * Configures basic exception handler and a common Camel Rest DSL configuration for inheriting
     * Rest endpoints defined by sub classes.
     */
    @Override
    public void configure() throws Exception {

        onException(APIException.class)
                .handled(true)
                .bean(PrepareErrorResponse.class)
                .log("Handled error ${exception.message}");

        onException(CamelAuthorizationException.class, BadCredentialsException.class, AuthenticationException.class)
                .handled(true)
                .logExhausted(false)
                .bean(BasicAuthFailedHandler.class)
                .log("Handled basic auth failure");

        onException(Exception.class)
                .handled(true)
                .bean(PrepareErrorResponse.class)
                .log("Handled generic error ${exception.stacktrace}");

        restConfiguration()
                .component("jetty")
                .scheme("https")
                    .host("0.0.0.0")
                    .port("{{api.port}}")
                    .contextPath("/api")
                .endpointProperty("matchOnUriPrefix", "true")
                .endpointProperty("sendServerVersion", "false")
                .componentProperty("filtersRef", "#tracingFilters")
                // enable CORS header in the HTTP response
                .enableCORS(true)
                .corsHeaderProperty("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .corsHeaderProperty("Access-Control-Allow-Headers",
                                    "Origin, Accept, Content-Type, Accept-Encoding, Accept-Language, "
                                    + "X-Requested-With, Transfer-Encoding, Authorization, X-APP-KEY, "
                                    + "Access-Control-Request-Method, Access-Control-Request-Headers")
                // we do want to make use of the streaming API which sends out the response in small chunks
                // instead of all at once. This, however, will replace the Content-Length header field with
                // a Transfer-Encoding: chunked header field. A chunk length of 0 indicates the end of the
                // stream
                .endpointProperty("chunked", "true");

        this.defineRoute();
    }

    /**
     * Defines an custom API route services have to specify.
     *
     * @throws Exception All thrown exceptions while executing the route
     */
    protected abstract void defineRoute() throws Exception;
}
