package at.rovo.awsxray.xray;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.aws.xray.XRayTrace;
import org.apache.camel.component.bean.BeanProcessor;
import org.apache.camel.model.BeanDefinition;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.processor.DelegateAsyncProcessor;
import org.apache.camel.processor.DelegateSyncProcessor;
import org.apache.camel.spi.InterceptStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;

public class TraceEIPTracingStrategy implements InterceptStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Processor wrapProcessorInInterceptors(CamelContext camelContext,
                                                 ProcessorDefinition<?> processorDefinition,
                                                 Processor target, Processor nextTarget)
            throws Exception {

        Class<?> processorClass = processorDefinition.getClass();
        String shortName = processorDefinition.getShortName();
        String simpleName = null;

        if (processorDefinition instanceof BeanDefinition) {
            BeanProcessor beanProcessor = (BeanProcessor) nextTarget;
            processorClass = beanProcessor.getBean().getClass();
            simpleName = processorClass.getSimpleName();
        } else if (processorDefinition instanceof ProcessDefinition) {
            DelegateSyncProcessor syncProcessor = (DelegateSyncProcessor) nextTarget;
            processorClass = syncProcessor.getProcessor().getClass();
            simpleName = processorClass.getSimpleName();
        }

        if (!processorClass.isAnnotationPresent(XRayTrace.class)) {
            LOG.trace("{} does not contain an @Trace annotation. Skipping interception",
                    processorClass.getSimpleName());

            if (simpleName != null) {
                shortName = shortName + ":" + simpleName;
            }
            final String defName = sanitizeName(shortName);
            return new DelegateAsyncProcessor((Exchange exchange) -> {
                LOG.trace("Creating new subsegment for {} - EIP {}", defName, target);
                Subsegment subsegment = AWSXRay.beginSubsegment(defName);
                try {
                    LOG.trace("Processing EIP {}", target);
                    target.process(exchange);
                } catch (Exception ex) {
                    LOG.trace("Handling exception thrown by invoked EIP {}", target);
                    subsegment.addException(ex);
                    throw ex;
                } finally {
                    LOG.trace("Closing down subsegment for {}", defName);
                    subsegment.close();
                }
            });
        }

        LOG.trace("Wrapping process definition {} of target {} in order for recording its trace",
                processorDefinition, processorClass);

        Annotation annotation = processorClass.getAnnotation(XRayTrace.class);
        XRayTrace trace = (XRayTrace)annotation;

        String metricName = trace.metricName();

        if ("".equals(metricName)) {
            metricName = processorClass.getSimpleName();
        }

        final Class<?> type = processorClass;
        final String name = sanitizeName(shortName + ":" + metricName);

        return new DelegateAsyncProcessor((Exchange exchange) -> {
            LOG.trace("Creating new subsegment for {} of type {} - EIP {}", name, type, target);
            Subsegment subsegment = AWSXRay.beginSubsegment(name);
            try {
                LOG.trace("Processing EIP {}", target);
                target.process(exchange);
            } catch (Exception ex) {
                LOG.trace("Handling exception thrown by invoked EIP {}", target);
                subsegment.addException(ex);
                throw ex;
            } finally {
                LOG.trace("Closing down subsegment for {}", name);
                subsegment.close();
            }
        });
    }

    private String sanitizeName(String name) {
        // Allowed characters: a-z, A-Z, 0-9, _, ., :, /, %, &, #, =, +, \, -, @
        // \w = a-zA-Z0-9_
        return name.replaceAll("[^\\w.:/%&#=+\\-@]", "_");
    }
}
