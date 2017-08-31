package at.rovo.awsxray.routes.api;

import org.apache.camel.Exchange;

public class HealthCheckResponderRoute extends BaseAPIRouteBuilder
{

  @Override
  protected void defineRoute() throws Exception {

    rest("/health")
        .get()

          .route().routeId("rest-health-check")
            .process((Exchange exchange) -> {
              exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
              exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "text/plain");
              exchange.getIn().setBody("Services is up and running");
            })
        .endRest();
  }
}
