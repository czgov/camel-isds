package com.github.czgov.isds;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

/**
 * The ISDS producer.
 */
public abstract class ISDSProducer extends DefaultProducer {
    protected ISDSEndpoint endpoint;

    public ISDSProducer(ISDSEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public abstract void process(Exchange exchange) throws Exception;
}
