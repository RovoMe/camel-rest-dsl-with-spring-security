package at.rovo.awsxray.config;

import at.rovo.awsxray.utils.SuppressJettyInfoErrorHandler;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jetty.JettyHttpComponent;
import org.apache.camel.component.jetty.JettyHttpEndpoint;
import org.apache.camel.component.jetty9.JettyHttpComponent9;
import org.apache.camel.util.IntrospectionSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(SSLContextConfig.class)
public class JettySpringConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private CamelContext camelContext;
    @Resource(name = "sslContextParameters")
    private SSLContextParameters sslContextParameters;

    private SuppressJettyInfoErrorHandler jettyErrorHandler() {
        return new SuppressJettyInfoErrorHandler();
    }

    /**
     * Defines a new Camel Jetty component and registers a SSL context for it. Also, to suppress the
     * Jetty typical <em>Powered by Jetty</em> output, a custom error handler is configured.
     *
     * @return The configured Camel Jetty component
     */
    private JettyHttpComponent jettyHttpComponent() throws URISyntaxException {
        LOG.debug("Initializing Jetty 9 HTTPS component");
        JettyHttpComponent jetty = new HackedJettyHttpComponent();
        jetty.setSslContextParameters(sslContextParameters);
        jetty.setErrorHandler(jettyErrorHandler());

        return jetty;
    }

    @PostConstruct
    public void init() throws Exception {
        LOG.debug("Registering customized Jetty 9 HTTPS component");
        camelContext.addComponent("jetty", jettyHttpComponent());
    }

    /**
     * A custom jetty http component which explicitly sets the excludedCipherSuites during creation of
     * the jetty connector.
     * <p/>
     * Why? It seems camel does not push included/excluded cipherSuites from {@link
     * SSLContextParameters} to the {@link SslContextFactory} nor does it push explicitly listed cipher
     * suites (i.e. like <em>TLS_RSA_WITH_AES_256_CBC_SHA</em>) to the Jetty SSL context factory.
     * <p/>
     * Once the fix of Camel issue 11482 is ready, this customized Jetty component can be removed again
     * as the fix will copy the properties to the Jetty context.
     *
     * @link https://issues.apache.org/jira/browse/CAMEL-11482
     * @link https://github.com/RovoMe/camel/tree/fix/CAMEL-11482_SSLContextParameters_settings_are_not_properly_copied_to_SslContextFactory
     */
    public static class HackedJettyHttpComponent extends JettyHttpComponent9 {

        @Override
        protected AbstractConnector createConnectorJettyInternal(Server server,
                                                                 JettyHttpEndpoint endpoint,
                                                                 SslContextFactory sslcf) {
            try {
                sslcf.setExcludeCipherSuites("^.*_(MD5|SHA1)$", "^.*_DHE_.*$", "^.*_3DES_.*$");
                String host = endpoint.getHttpUri().getHost();
                int porto = endpoint.getPort();
                org.eclipse.jetty.server.HttpConfiguration httpConfig = new org.eclipse.jetty.server.HttpConfiguration();
                httpConfig.setSendServerVersion(endpoint.isSendServerVersion());
                httpConfig.setSendDateHeader(endpoint.isSendDateHeader());
                httpConfig.setSendDateHeader(endpoint.isSendDateHeader());

                if (requestBufferSize != null) {
                    // Does not work
                    //httpConfig.setRequestBufferSize(requestBufferSize);
                }
                if (requestHeaderSize != null) {
                    httpConfig.setRequestHeaderSize(requestHeaderSize);
                }
                if (responseBufferSize != null) {
                    httpConfig.setOutputBufferSize(responseBufferSize);
                }
                if (responseHeaderSize != null) {
                    httpConfig.setResponseHeaderSize(responseHeaderSize);
                }
                if (useXForwardedForHeader) {
                    httpConfig.addCustomizer(new ForwardedRequestCustomizer());
                }
                HttpConnectionFactory
                        httpFactory = new org.eclipse.jetty.server.HttpConnectionFactory(httpConfig, HttpCompliance.RFC2616);

                ArrayList<ConnectionFactory> connectionFactories = new ArrayList<>();
                ServerConnector result = new org.eclipse.jetty.server.ServerConnector(server);
                if (sslcf != null) {
                    httpConfig.addCustomizer(new org.eclipse.jetty.server.SecureRequestCustomizer());
                    SslConnectionFactory
                            scf = new org.eclipse.jetty.server.SslConnectionFactory(sslcf, "HTTP/1.1");
                    connectionFactories.add(scf);
                    // The protocol name can be "SSL" or "SSL-HTTP/1.1" depending on the version of Jetty
                    result.setDefaultProtocol(scf.getProtocol());
                }
                connectionFactories.add(httpFactory);
                result.setConnectionFactories(connectionFactories);
                result.setPort(porto);
                if (host != null) {
                    result.setHost(host);
                }
                if (getSslSocketConnectorProperties() != null && "https".equals(endpoint.getProtocol())) {
                    // must copy the map otherwise it will be deleted
                    Map<String, Object>
                            properties = new HashMap<>(getSslSocketConnectorProperties());
                    IntrospectionSupport.setProperties(sslcf, properties);
                    if (properties.size() > 0) {
                        throw new IllegalArgumentException("There are " + properties.size()
                                                           + " parameters that couldn't be set on the SocketConnector."
                                                           + " Check the uri if the parameters are spelt correctly and that they are properties of the SelectChannelConnector."
                                                           + " Unknown parameters=[" + properties + "]");
                    }
                }
                return result;
            } catch (Exception e) {
                throw ObjectHelper.wrapRuntimeCamelException(e);
            }
        }
    }
}
