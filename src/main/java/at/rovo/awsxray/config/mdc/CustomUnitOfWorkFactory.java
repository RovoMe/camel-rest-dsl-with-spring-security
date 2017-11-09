package at.rovo.awsxray.config.mdc;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultUnitOfWork;
import org.apache.camel.spi.UnitOfWork;
import org.apache.camel.spi.UnitOfWorkFactory;

public class CustomUnitOfWorkFactory implements UnitOfWorkFactory {

    protected final String[] headerNames;

    public CustomUnitOfWorkFactory() {
        this.headerNames = null;
    }

    public CustomUnitOfWorkFactory(String[] headerNames) {
        this.headerNames = headerNames;
    }

    @Override
    public UnitOfWork createUnitOfWork(Exchange exchange) {
        UnitOfWork answer;

        if (exchange.getContext().isUseMDCLogging()) {

            // allow customization of the logged headers
            if (headerNames != null) {
                answer = new CustomMDCUnitOfWork(exchange, headerNames);
            } else {
                // fallback to default headers
                answer = new CustomMDCUnitOfWork(exchange);
            }
        } else {
            answer = new DefaultUnitOfWork(exchange);
        }

        return answer;
    }
}
