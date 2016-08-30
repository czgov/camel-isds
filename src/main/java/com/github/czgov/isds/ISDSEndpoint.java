package com.github.czgov.isds;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;

import java.util.concurrent.TimeUnit;

/**
 * Represents a ISDS endpoint.
 */
@UriEndpoint(scheme = "isds", title = "ISDS", syntax = "isds:?opt=value", consumerClass = ISDSConsumer.class, label = "ISDS")
public class ISDSEndpoint extends DefaultEndpoint {

    @UriPath(description = "This endpoint doesn't use path parameter.")
    @Metadata(required = "false")
    private String uriPath;

    @UriParam(enums = "production,test", defaultValue = "production")
    @Metadata(required = "false")
    private ISDSEnvironment environment = ISDSEnvironment.PRODUCTION;

    @UriParam(defaultValue = "1m", label = "consumer", description = "Interval between polling messages from ISDS in miliseconds."
            + " You can also specify time values using units, such as 60s (60 seconds), 5m30s (5 minutes and 30 seconds), and 1h (1 hour).")
    private long period = TimeUnit.MINUTES.toMillis(1);

    public ISDSEndpoint(String uri, ISDSComponent component) {
        super(uri, component);
    }

    public Producer createProducer() throws Exception {
        return new ISDSProducer(this);
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        return new ISDSConsumer(this, processor);
    }

    public boolean isSingleton() {
        return true;
    }

    public ISDSEnvironment getEnvironment() {
        return environment;
    }

    /**
     * Some description of this option, and what it does
     */
    public void setEnvironment(ISDSEnvironment environment) {
        this.environment = environment;
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }
}
