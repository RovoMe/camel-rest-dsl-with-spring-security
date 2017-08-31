package at.rovo.awsxray;

import at.rovo.awsxray.config.SpringConfig;
import java.lang.invoke.MethodHandles;
import org.apache.camel.Service;
import org.apache.camel.spring.javaconfig.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AwsXRayApp implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Main app;

    public static void main(String ... args) throws Exception {
        try {
            java.security.Security.setProperty("networkaddress.cache.ttl", "180");
            LOG.info("Set Networkaddress cache of the Java VM to a TTL of 3m");

        } catch (Exception ex) {
            LOG.warn(
                    "Could not set the TTL of the Java VM Networkaddress cache. Will stick to the default value");

        }
        AwsXRayApp app = new AwsXRayApp();
        app.start();
    }

    @Override
    public void start() throws Exception {
        app = new Main();
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringConfig.class);
        app.setApplicationContext(ctx);
        app.run();
    }

    @Override
    public void stop() throws Exception {
        if (app != null) {
            app.stop();
        }
    }
}
