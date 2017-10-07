package at.rovo.awsxray;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.StaticService;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.management.event.ExchangeCompletedEvent;
import org.apache.camel.management.event.ExchangeCreatedEvent;
import org.apache.camel.management.event.ExchangeSendingEvent;
import org.apache.camel.management.event.ExchangeSentEvent;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.processor.interceptor.Tracer;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.camel.support.EventNotifierSupport;
import org.apache.camel.support.RoutePolicySupport;
import org.apache.camel.support.ServiceSupport;
import org.apache.camel.support.SynchronizationAdapter;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.apache.camel.test.spring.CamelTestContextBootstrapper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(CamelSpringRunner.class)
@BootstrapWith(CamelTestContextBootstrapper.class)
@ContextConfiguration(classes = {CamelExchangeHandlingTest.ContextConfig.class},
    loader = AnnotationConfigContextLoader.class)
public class CamelExchangeHandlingTest {

  public static class TestTracer extends ServiceSupport
      implements RoutePolicyFactory, StaticService, CamelContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EventNotifier eventNotifier = new EventNotifier();
    private CamelContext camelContext;

    @Override
    public void setCamelContext(CamelContext camelContext) {
      this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
      return this.camelContext;
    }

    @Override
    public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, RouteDefinition routeDefinition) {
      // ensure this XRay tracer gets initialized when Camel starts
      init(camelContext);
      return new RoutePolicy(routeId);
    }

    @Override
    protected void doStart() throws Exception {
      ObjectHelper.notNull(camelContext, "CamelContext", this);

      camelContext.getManagementStrategy().addEventNotifier(eventNotifier);
      if (!camelContext.getRoutePolicyFactories().contains(this)) {
        camelContext.addRoutePolicyFactory(this);
      }

      LOG.debug("Starting tracer");
      ServiceHelper.startServices(eventNotifier);
    }

    @Override
    protected void doStop() throws Exception {
      // stop event notifier
      camelContext.getManagementStrategy().removeEventNotifier(eventNotifier);
      ServiceHelper.stopAndShutdownService(eventNotifier);

      // remove route policy
      camelContext.getRoutePolicyFactories().remove(this);
      LOG.debug("Tracer stopped");
    }

    public void init(CamelContext camelContext) {
      if (!camelContext.hasService(this)) {
        try {
          LOG.debug("Initializing tracer");
          // start this service eager so we init before Camel is starting up
          camelContext.addService(this, true, true);
        } catch (Exception e) {
          throw ObjectHelper.wrapRuntimeCamelException(e);
        }
      }
    }

    private final class EventNotifier extends EventNotifierSupport {

      @Override
      public void notify(EventObject event) throws Exception {
        if (event instanceof ExchangeCreatedEvent) {
          ExchangeCreatedEvent ece = (ExchangeCreatedEvent) event;
          LOG.debug("EventNotifier::CreatedEvent - source: {}, exchange: {}",
              ece.getSource(), ece.getExchange());
        } else if (event instanceof ExchangeCompletedEvent) {
          ExchangeCompletedEvent ece = (ExchangeCompletedEvent) event;
          LOG.debug("EventNotifier::CompletedEvent - source: {}, exchange: {}",
              ece.getSource(), ece.getExchange());
        } else if (event instanceof ExchangeSentEvent) {
          ExchangeSentEvent ese = (ExchangeSentEvent) event;
          LOG.debug("EventNotifier::SentEvent - source: {}, exchange: {}, endpoint: {}",
              ese.getSource(), ese.getExchange(), ese.getEndpoint());
        } else if (event instanceof ExchangeSendingEvent) {
          ExchangeSendingEvent ese = (ExchangeSendingEvent) event;
          LOG.debug("EventNotifier::SendingEvent - source: {}, exchange: {}, endpoint: {}",
              ese.getSource(), ese.getExchange(), ese.getEndpoint());
        } else {
          LOG.debug("Received unknown event {} from source {}", event, event.getSource());
        }
      }

      @Override
      public boolean isEnabled(EventObject event) {
        // listen for either when an exchange invoked an other endpoint
        return event instanceof ExchangeSendingEvent
            || event instanceof ExchangeSentEvent
            || event instanceof ExchangeCreatedEvent
            || event instanceof ExchangeCompletedEvent;
      }
    }

    private final class RoutePolicy extends RoutePolicySupport {

      private final String routeId;

      RoutePolicy(String routeId) {
        this.routeId = routeId;
      }

      @Override
      public void onExchangeBegin(Route route, Exchange exchange) {
        // use route policy to track event when a Camel route begins/ends the lifecycle of an Exchange

        LOG.debug("RoutePolicy::OnExchangeBegin - route: {}, exchange: {}",
            route.getId(), exchange);
        exchange.addOnCompletion(new SynchronizationAdapter() {
          @Override
          public void onAfterRoute(Route route, Exchange exchange) {
            LOG.debug("RoutePolicy::OnExchangeBegin_OnAfterRoute - route: {}, exchange: {}",
                route.getId(), exchange);
          }

          @Override
          public String toString() {
            return "RoutePolicy::OnExchangeBegin_OnCompletion[" + routeId + "]";
          }
        });
      }

      @Override
      public void onExchangeDone(Route route, Exchange exchange) {
        LOG.debug("RoutePolicy::OnExchangeDone - route: {}, exchange: {}",
            route.getId(), exchange);
      }
    }
  }

  @Configuration
  public static class ContextConfig extends CamelConfiguration {

    @Override
    protected void setupCamelContext(CamelContext camelContext) throws Exception {
      super.setupCamelContext(camelContext);

      camelContext.setTracing(true);
      final Tracer tracer = new Tracer();
      tracer.getDefaultTraceFormatter().setShowBody(false);
      tracer.setLogLevel(LoggingLevel.INFO);
      camelContext.getInterceptStrategies().add(tracer);

      TestTracer testTracer = new TestTracer();
      testTracer.init(camelContext);
    }

    @Override
    public List<RouteBuilder> routes() {
      List<RouteBuilder> routes = new ArrayList<>();
      routes.add(new RouteBuilder() {
        @Override
        public void configure() throws Exception {
          from("direct:start").routeId("start")
              .setHeader("test", constant("Some header value"))
              .to("direct:otherRoute")
              .removeHeader("test")
              .end();
        }
      });
      routes.add(new RouteBuilder() {
        @Override
        public void configure() throws Exception {
          from("direct:otherRoute").routeId("otherRoute")
              .log("otherRoute invoked");
        }
      });
      return routes;
    }
  }

  @Produce(uri = "direct:start")
  private ProducerTemplate template;

  @Test
  public void testExchangeHandling() throws Exception {
    template.sendBody("Some body");
  }
}
