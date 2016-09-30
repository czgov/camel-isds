package com.github.czgov.isds;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.abclinuxu.datoveschranky.impl.Authentication;
import cz.abclinuxu.datoveschranky.impl.BasicAuthentication;
import cz.abclinuxu.datoveschranky.impl.DataBoxManager;

/**
 * Represents a ISDS endpoint.
 */
@UriEndpoint(scheme = "isds", title = "ISDS", syntax = "isds:?opt=value", consumerClass = ISDSConsumer.class, label = "ISDS")
public class ISDSEndpoint extends DefaultEndpoint {
    private static final Logger log = LoggerFactory.getLogger(ISDSEndpoint.class);

    @UriPath(description = "This endpoint doesn't use path parameter.")
    @Metadata(required = "false")
    private String uriPath;

    @UriParam(enums = "production,test", defaultValue = "production")
    @Metadata(required = "false")
    private ISDSEnvironment environment = ISDSEnvironment.PRODUCTION;

    @UriParam(description = "Username for ISDS system.")
    @Metadata(required = "true")
    private String username;

    @UriParam(description = "Password for ISDS system")
    @Metadata(required = "true")
    private String password;

    private Authentication dataBoxAuth;
    private DataBoxManager dataBoxManager;

    public ISDSEndpoint(String uri, ISDSComponent component) {
        super(uri, component);
    }

    /**
     * Populate instance of {@link Authentication} and {@link DataBoxManager}.
     * This method must be called once {@code @UriParam} fields are resolved.
     * In constructor it's too early.
     */
    private void initDataBox() {
        if (dataBoxAuth == null) {
            log.debug("Initializing DataBoxManager with env {} and login {}.", environment, username);
            dataBoxAuth = new BasicAuthentication(environment.getConfig(), username, password);
            dataBoxManager = new DataBoxManager(environment.getConfig(), dataBoxAuth);
        }
    }

    public Producer createProducer() throws Exception {
        initDataBox();
        return new ISDSProducer(this);
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        initDataBox();
        Consumer consumer = new ISDSConsumer(this, processor, dataBoxManager);
        configureConsumer(consumer);
        return consumer;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Authentication getDataBoxAuth() {
        return dataBoxAuth;
    }

    public DataBoxManager getDataBoxManager() {
        return dataBoxManager;
    }
}
