package com.github.czgov.isds;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ISDS producer.
 */
public class ISDSProducer extends DefaultProducer {
    private static final Logger LOG = LoggerFactory.getLogger(ISDSProducer.class);
    private ISDSEndpoint endpoint;

    public ISDSProducer(ISDSEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    public void process(Exchange exchange) throws Exception {
        System.out.println(exchange.getIn().getBody());
    }
}
