package com.github.czgov.isds;

import org.apache.camel.EndpointInject;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import cz.abclinuxu.datoveschranky.common.entities.Message;

/**
 * Created by jludvice on 9/30/16.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ISDSComponentTest extends ISDSTestBase {

    @EndpointInject(uri = "mock:producer-basic")
    MockEndpoint mockEndpoint;
    private String ovmId;
    private String foId;

    @Before
    public void init() throws Exception {
        ovmId = context.resolvePropertyPlaceholders("{{isds.ovm.id}}");
        foId = context.resolvePropertyPlaceholders("{{isds.fo.id}}");
    }

    @Test
    public void sendMessageInBody() {
        String subject = "Test Message OVM -> FO " + new Date();

        Message message = createMessage(foId, subject);
        Message response = template.requestBody("isds:?environment=test&username={{isds.ovm.login}}&password={{isds.ovm.password}}", message, Message.class);

        String msgId = response.getEnvelope().getMessageID();
        assertNotNull("Sent message must have ID defined", msgId);
        assertTrue("Sent message must have numeric id. Found: " + msgId, Long.parseLong(msgId) > 0);
        System.out.println("result: " + response);
    }

    @Test
    public void recieveMessageInBody() throws InterruptedException {

        // send message from OVM to FO
        Message message = createMessage(ovmId, "FO->OVM at " + new Date());
        Message response = template.requestBody("isds:?environment=test&username={{isds.fo.login}}&password={{isds.fo.password}}", message, Message.class);

        // assert we received message with given id
        mockEndpoint.expectedBodiesReceived(response);
        mockEndpoint.assertIsSatisfied(TimeUnit.MINUTES.toMillis(1));
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        initProperties();
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("isds:?environment=test&username={{isds.ovm.login}}&password={{isds.ovm.password}}").id("from-ovm")
                        .log("new message in OVM inbox - id ${body.envelope.messageID}")
                        .to(mockEndpoint.getEndpointUri());
            }
        };
    }
}
