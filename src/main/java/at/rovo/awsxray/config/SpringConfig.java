package at.rovo.awsxray.config;

import at.rovo.awsxray.routes.S3FileUploadRoute;
import at.rovo.awsxray.routes.api.HealthCheckResponderRoute;
import at.rovo.awsxray.routes.HttpInvokerRoute;
import at.rovo.awsxray.routes.api.SampleFileRoute;
import at.rovo.awsxray.routes.beans.LogUserCompany;
import at.rovo.awsxray.utils.DatabasePopulator;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import dk.nykredit.jackson.dataformat.hal.HALMapper;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultDataFormatResolver;
import org.apache.camel.management.DefaultManagementAgent;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.ManagementAgent;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;

@Configuration
@Import({
        MongoSpringConfig.class,
        JettySpringConfig.class,
        SpringSecurityConfig.class,
        AwsS3SpringConfig.class,
        HttpClientSpringConfig.class
})
@PropertySource("classpath:app-${spring.profiles.active}.properties")
public class SpringConfig extends CamelConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private Environment env;
    @Resource
    private S3FileUploadRoute s3FileUploadRoute;

    /**
     * This bean is required to access the property files using @Value("${...}")
     *
     * @return A PropertySourcePlaceholderConfigurer object
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Override
    protected void setupCamelContext(CamelContext camelContext) throws Exception {
        LOG.debug("Initializing camel context");
        super.setupCamelContext(camelContext);

        final ManagementAgent agent = new DefaultManagementAgent(camelContext);
        agent.setCreateConnector(true);
        agent.setUsePlatformMBeanServer(true);
        camelContext.getManagementStrategy().setManagementAgent(agent);

        // and a simple naming pattern for all the other Camel JMX managed parts
        camelContext.getManagementNameStrategy().setNamePattern("#name#");
        camelContext.setUseBreadcrumb(true);
        camelContext.setTracing(false);

        final PropertiesComponent pc = new PropertiesComponent("classpath:" + env.getProperty("propertyfile"));
        camelContext.addComponent("properties", pc);

        camelContext.setDataFormatResolver(new HalDataFormatResolver());
    }

    @Bean(name = "producerTemplate")
    public ProducerTemplate producerTemplate() throws Exception {
        return camelContext().createProducerTemplate();
    }

    @Override
    public List<RouteBuilder> routes() {
        LOG.debug("Initializing Camel routes");
        List<RouteBuilder> routes = new ArrayList<>(4);
        routes.add(healthCheckResponderRoute());
        routes.add(httpInvokerRoute());
        routes.add(sampleFileRoute());
        routes.add(s3FileUploadRoute);

        return routes;
    }

    @Bean(name = "healthCheckResponder")
    public HealthCheckResponderRoute healthCheckResponderRoute() {
        LOG.debug("Initializing HealCheckResponder route");
        return new HealthCheckResponderRoute();
    }

    @Bean
    public HttpInvokerRoute httpInvokerRoute() {
        return new HttpInvokerRoute();
    }

    @Bean
    public SampleFileRoute sampleFileRoute() {
        return new SampleFileRoute();
    }

    @Bean
    public DatabasePopulator databasePopulator() {
        return new DatabasePopulator();
    }

    @Bean
    public JacksonJsonProvider jacksonJsonProvider() {
        return new JacksonJsonProvider(new HALMapper());
    }

    @Bean
    public LogUserCompany logUserCompany() {
        return new LogUserCompany();
    }

    private class HalDataFormatResolver extends DefaultDataFormatResolver {

        private final DataFormat dataFormat;

        public HalDataFormatResolver() {
            JacksonDataFormat jdf = new JacksonDataFormat();
            jdf.setPrettyPrint(true);
            jdf.setObjectMapper(new HALMapper());
            dataFormat = jdf;
        }

        @Override
        public DataFormat resolveDataFormat(String name, CamelContext context) {
            if ("hal+json".equals(name)) {
                return dataFormat;
            } else {
                return super.resolveDataFormat(name, context);
            }
        }

        @Override
        public DataFormat createDataFormat(String name, CamelContext context) {
            if ("hal+json".equals(name)) {
                return dataFormat;
            } else {
                return super.createDataFormat(name, context);
            }
        }
    }
}
