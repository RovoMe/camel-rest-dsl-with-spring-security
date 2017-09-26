package at.rovo.awsxray.xray;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.model.BeanDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.processor.DelegateAsyncProcessor;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EIPTracingStrategy implements InterceptStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Processor wrapProcessorInInterceptors(CamelContext camelContext, ProcessorDefinition<?> processorDefinition,
                                                 Processor target, Processor nextTarget) throws Exception {

        Class<?> beanClass = null;
        if (processorDefinition instanceof BeanDefinition) {
            BeanDefinition beanDef = (BeanDefinition) processorDefinition;
            Field[] declaredFields = beanDef.getClass().getDeclaredFields();
            Field beanDefField = Arrays.stream(declaredFields).filter(field -> "beanClass".equals(field.getName())).findFirst().orElse(null);
            if (null != beanDefField) {
                beanDefField.setAccessible(true);
                Object value = beanDefField.get(beanDef);
                if (value instanceof Class<?>) {
                    beanClass = (Class<?>) value;
                }
            }
        }

        if (null == beanClass || !beanClass.isAnnotationPresent(Trace.class)) {
            LOG.trace("Either no bean or no bean with an @Trace annotation found. Skipping interception");
            return new DelegateAsyncProcessor(target);
        }

        LOG.trace("Wrapping process definition {} of target bean {} in order for recording the EIP trace",
                  processorDefinition, beanClass);

        Annotation annotation = beanClass.getAnnotation(Trace.class);
        Trace trace = (Trace)annotation;

        String metricName = trace.metricName();

        if (StringUtils.isBlank(metricName)) {
            metricName = beanClass.getSimpleName();
        }

        final String name = metricName;

        return new DelegateAsyncProcessor((Exchange exchange) -> {
            LOG.debug("Creating new subsegment for EIP {} as {}", target, name);
            Subsegment subsegment = AWSXRay.beginSubsegment(name);
            try {
                LOG.debug("Processing EIP {}", target);
                target.process(exchange);
            } catch (Exception ex) {
                LOG.debug("Handling exception thrown by invoked EIP {}", target);
                subsegment.addException(ex);
            } finally {
                LOG.debug("Closing down subsegment for {}", name);
                subsegment.close();
            }
        });
    }
}
