package at.rovo.awsxray.xray;

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.processor.DelegateAsyncProcessor;
import org.apache.camel.spi.InterceptStrategy;

public class NoopTracingStrategy implements InterceptStrategy {

    @Override
    public Processor wrapProcessorInInterceptors(CamelContext camelContext, ProcessorDefinition<?> processorDefinition,
                                                 Processor target, Processor nextTarget) throws Exception {
        return new DelegateAsyncProcessor(target);
    }
}
