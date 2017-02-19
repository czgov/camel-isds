package com.github.czgov.isds;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.czgov.isds.internal.ISDSOperation;

import java.util.Map;

/**
 * Represents the component that manages {@link ISDSEndpoint}.
 */
public class ISDSComponent extends UriEndpointComponent {

    private static final Logger log = LoggerFactory.getLogger(ISDSComponent.class);

    public ISDSComponent() {
        super(ISDSEndpoint.class);
    }

    public ISDSComponent(CamelContext context) {
        super(context, ISDSEndpoint.class);
    }

    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        ISDSEndpoint endpoint = new ISDSEndpoint(uri, this);
        endpoint.setOperation(ISDSOperation.fromValue(remaining));
        setProperties(endpoint, parameters);
        return endpoint;
    }
}
