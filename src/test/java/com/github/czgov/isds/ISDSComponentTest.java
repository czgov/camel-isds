package com.github.czgov.isds;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Test;

public class ISDSComponentTest extends CamelTestSupport {

    @Test
    public void testISDS() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("isds:?environment=test")
                        .to("isds://bar")
                        .to("mock:result");
            }
        };
    }
}
