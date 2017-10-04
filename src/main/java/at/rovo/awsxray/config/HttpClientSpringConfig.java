package at.rovo.awsxray.config;

import at.rovo.awsxray.config.settings.HttpClientSettings;
import at.rovo.awsxray.utils.CustomTruststoreParameters;
import com.amazonaws.xray.proxies.apache.http.HttpClientBuilder;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.util.IntrospectionSupport;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HttpClientSettings.class)
public class HttpClientSpringConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private HttpClientSettings httpClientSettings;
    @Resource
    private CamelContext camelContext;

    @Bean
    public HttpClientBuilder httpClientBuilder() {

        SocketConfig socketConfig = httpClientSocketConfig();
        RequestConfig requestConfig = httpClientRequestConfig();
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultRequestConfig(requestConfig);
        clientBuilder.setDefaultSocketConfig(socketConfig);

        return clientBuilder;
    }

    @Bean
    public SocketConfig httpClientSocketConfig() {
        /*
          socket timeout:
          Monitors the time passed between two consecutive incoming messages over the connection and
         raises a SocketTimeoutException if no message was received within the given timeout interval
         */
        LOG.info("Creating a SocketConfig with a socket timeout of {} seconds", httpClientSettings.getSoTimeout());
        return SocketConfig.custom()
                .setSoTimeout(httpClientSettings.getSoTimeout() * 1000)
                .build();
    }

    @Bean
    public RequestConfig httpClientRequestConfig() {
        /*
          connection timeout:
          The time span the application will wait for a connection to get established. If the connection
          is not established within the given amount of time a ConnectionTimeoutException will be raised.
         */
        LOG.info("Creating a RequestConfig with a socket timeout of {} seconds and a connection timeout of {} seconds",
                 httpClientSettings.getSoTimeout(), httpClientSettings.getConTimeout());
        return RequestConfig.custom()
                .setConnectTimeout(httpClientSettings.getConTimeout() * 1000)
                .setSocketTimeout(httpClientSettings.getSoTimeout() * 1000)
                .build();
    }

    @Bean(name = "httpClientConfigurer")
    public HttpClientConfigurer httpConfiguration() {
        return builder -> builder.setDefaultSocketConfig(httpClientSocketConfig())
                .setDefaultRequestConfig(httpClientRequestConfig());
    }

    @Bean(name = "customHttpClient")
    public HttpComponent registerCustomHttp4Component() {

        SSLContextParameters scp = new SSLContextParameters();
        scp.setTrustManagers(new CustomTruststoreParameters());

        HttpComponent httpComponent = new AWSXRayEnrichedHttpComponent();
        httpComponent.setSslContextParameters(scp);
        httpComponent.setX509HostnameVerifier(new AllowAllHostnameVerifier());
        httpComponent.setHttpClientConfigurer(httpConfiguration());
        return httpComponent;
    }

    @PostConstruct
    public void init() {
        LOG.debug("Initializing HTTP clients");
        camelContext.addComponent("https", registerCustomHttp4Component());
        camelContext.addComponent("http4", new AWSXRayEnrichedHttpComponent());
        HttpComponent httpComponent = camelContext.getComponent("http4", AWSXRayEnrichedHttpComponent.class);
        httpComponent.setHttpClientConfigurer(httpConfiguration());
    }

    /**
     * Customized {@link HttpComponent} which uses an AWS XRay enriched {@link HttpClientBuilder} instead of the default
     * http client builder of Apache HTTP.
     */
    private class AWSXRayEnrichedHttpComponent extends HttpComponent {

        @Override
        protected HttpClientBuilder createHttpClientBuilder(final String uri, final Map<String, Object> parameters,
                                                            final Map<String, Object> httpClientOptions) throws Exception {
            // http client can be configured from URI options
            HttpClientBuilder clientBuilder = HttpClientBuilder.create();
            // allow the builder pattern
            httpClientOptions.putAll(IntrospectionSupport.extractProperties(parameters, "httpClient."));
            IntrospectionSupport.setProperties(clientBuilder, httpClientOptions);
            // set the Request configure this way and allow the builder pattern
            RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
            IntrospectionSupport.setProperties(requestConfigBuilder, httpClientOptions);
            clientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

            // validate that we could resolve all httpClient. parameters as this component is lenient
            validateParameters(uri, httpClientOptions, null);

            return clientBuilder;
        }
    }
}
