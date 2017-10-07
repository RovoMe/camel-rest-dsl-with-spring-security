package at.rovo.awsxray.xray;

import at.rovo.awsxray.HeaderConstants;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.TraceID;
import java.lang.invoke.MethodHandles;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.StaticService;
import org.apache.camel.management.event.ExchangeSendingEvent;
import org.apache.camel.management.event.ExchangeSentEvent;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.support.EventNotifierSupport;
import org.apache.camel.support.RoutePolicySupport;
import org.apache.camel.support.ServiceSupport;
import org.apache.camel.support.SynchronizationAdapter;
import org.apache.camel.util.EndpointHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XRayTracer extends ServiceSupport implements RoutePolicyFactory, StaticService, CamelContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final XRayEventNotifier eventNotifier = new XRayEventNotifier();
    private CamelContext camelContext;

    private Set<String> excludePatterns = new HashSet<>();
    private InterceptStrategy tracingStrategy;

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
        return new XRayRoutePolicy(routeId);
    }

    @Override
    protected void doStart() throws Exception {
        ObjectHelper.notNull(camelContext, "CamelContext", this);

        camelContext.getManagementStrategy().addEventNotifier(eventNotifier);
        if (!camelContext.getRoutePolicyFactories().contains(this)) {
            camelContext.addRoutePolicyFactory(this);
        }

        if (null == tracingStrategy) {
            LOG.info("No tracing strategy available. Defaulting to no-op strategy");
            tracingStrategy = new NoopTracingStrategy();
        }

        camelContext.addInterceptStrategy(tracingStrategy);

        LOG.debug("Starting XRay tracer");
        ServiceHelper.startServices(eventNotifier);
    }

    @Override
    protected void doStop() throws Exception {
        // stop event notifier
        camelContext.getManagementStrategy().removeEventNotifier(eventNotifier);
        ServiceHelper.stopAndShutdownService(eventNotifier);

        // remove route policy
        camelContext.getRoutePolicyFactories().remove(this);
        LOG.debug("XRay tracer stopped");
    }

    public void init(CamelContext camelContext) {
        if (!camelContext.hasService(this)) {
            try {
                LOG.debug("Initializing XRay tracer");
                // start this service eager so we init before Camel is starting up
                camelContext.addService(this, true, true);
            } catch (Exception e) {
                throw ObjectHelper.wrapRuntimeCamelException(e);
            }
        }
    }

    public InterceptStrategy getTracingStrategy() {
        return tracingStrategy;
    }

    public void setTracingStrategy(InterceptStrategy traceingStrategy) {
        this.tracingStrategy = traceingStrategy;
    }

    public Set<String> getExcludePatterns() {
        return this.excludePatterns;
    }

    public void setExcludePatterns(Set<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    /**
     * Adds an exclude pattern that will disable tracing for Camel messages that matches the pattern.
     *
     * @param pattern The pattern such as route id, endpoint url
     */
    public void addExcludePattern(String pattern) {
        excludePatterns.add(pattern);
    }

    private boolean isExcluded(Exchange exchange, Endpoint endpoint) {
        String url = endpoint.getEndpointUri();
        if (url != null && !excludePatterns.isEmpty()) {
            for (String pattern : excludePatterns) {
                if (EndpointHelper.matchEndpoint(exchange.getContext(), url, pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Custom camel event handler that will create a new {@link Segment XRay segment} if a new exchange was created or
     * close the segment if it is handed over to an other endpoint.
     */
    private final class XRayEventNotifier extends EventNotifierSupport {

        @Override
        public void notify(EventObject event) throws Exception {
             if (event instanceof ExchangeSentEvent) {
                ExchangeSentEvent ese = (ExchangeSentEvent) event;
                LOG.debug("Exchange handed over from source {} to endpoint {}", ese.getSource(), ese.getEndpoint());
                if (isExcluded(ese.getExchange(), ese.getEndpoint())) {
                    return;
                }

                // TODO close a segment
            } else {
                LOG.debug("Received event {} from source {}", event, event.getSource());
            }
        }

        @Override
        public boolean isEnabled(EventObject event) {
            // listen for either when an exchange invoked an other endpoint
            return event instanceof ExchangeSendingEvent || event instanceof ExchangeSentEvent;
        }
    }

    private final class XRayRoutePolicy extends RoutePolicySupport {

        private final String routeId;

        XRayRoutePolicy(String routeId) {
            this.routeId = routeId;
        }

        @Override
        public void onExchangeBegin(Route route, Exchange exchange) {
            // use route policy to track event when a Camel route begins/ends the lifecycle of an Exchange

            if (isExcluded(exchange, route.getEndpoint())) {
                return;
            }

            TraceID traceID;
            if (exchange.getIn().getHeaders().containsKey(HeaderConstants.XRAY_TRACE_ID)) {
                traceID = TraceID.fromString(exchange.getIn().getHeader(HeaderConstants.XRAY_TRACE_ID, String.class));
            } else {
                traceID = new TraceID();
                exchange.getIn().setHeader(HeaderConstants.XRAY_TRACE_ID, traceID.toString());
            }

            final Segment segment = AWSXRay.beginSegment(route.getId());
            segment.setTraceId(traceID);

            LOG.debug("Starting new exchange {} for route {}", exchange.getExchangeId(), route.getId());
            exchange.addOnCompletion(new SynchronizationAdapter() {
                @Override
                public void onAfterRoute(Route route, Exchange exchange) {
                    LOG.debug("Exchange {} completed", exchange.getExchangeId());
                    segment.close();
                }

                @Override
                public String toString() {
                    return "XRayTracerOnCompletion[" + routeId + "]";
                }
            });
        }
    }
}
