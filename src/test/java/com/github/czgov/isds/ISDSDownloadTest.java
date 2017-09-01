package com.github.czgov.isds;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;

import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.abclinuxu.datoveschranky.common.ByteArrayAttachmentStorer;
import cz.abclinuxu.datoveschranky.common.entities.Message;
import cz.abclinuxu.datoveschranky.impl.MessageValidator;

/**
 * Created by jludvice on 19.7.17.
 */
public class ISDSDownloadTest extends ISDSTestBase {
    public static final Logger log = LoggerFactory.getLogger(ISDSDownloadTest.class);

    @EndpointInject(uri = "direct:download-sent")
    ProducerTemplate start;

    private Message fixtureMessage;

    @Before
    public void sentTestMessage() {
        fixtureMessage = createMessage(getOvmId(), "Msg for Download" + new Date());
        getManager().getDataBoxUploadService().sendMessage(fixtureMessage);
        log.info("Fixture message {}", fixtureMessage);
    }

    @Test
    public void downloadSentMessage() throws IOException, InterruptedException {

        Map<String, Object> headers = new HashMap<>();

        headers.put(Constants.MSG_ID, fixtureMessage.getEnvelope().getMessageID());
        headers.put(Constants.MSG_TYPE, "sent");

        byte[] messageBytes = start.requestBodyAndHeaders(
                "direct:download-sent", "message body",
                headers, byte[].class);
        MessageValidator v = new MessageValidator();
        Message m = v.createMessage(messageBytes, new ByteArrayAttachmentStorer());

        assertEquals("Subject of sent and downloaded message must match",
                fixtureMessage.getEnvelope().getAnnotation(),
                m.getEnvelope().getAnnotation());
    }

    @Test
    public void downloadReceivedMessage() throws Exception {

        Map<String, Object> headers = new HashMap<>();

        headers.put(Constants.MSG_ID, fixtureMessage.getEnvelope().getMessageID());
        headers.put(Constants.MSG_TYPE, "received");

        log.info("downloading message {}", fixtureMessage.getEnvelope().getMessageID());
        Message m = start.requestBodyAndHeaders(
                "direct:download-received", "message body",
                headers, Message.class);

        System.out.println(m);
        assertEquals("Subject of sent and downloaded message must match",
                fixtureMessage.getEnvelope().getAnnotation(),
                m.getEnvelope().getAnnotation());
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                initProperties();
                from("direct:download-sent")
                        .to("isds:download?username={{isds.fo.login}}&password={{isds.fo.password}}&environment=test")
                        .log("done");

                from("direct:download-received")
                        .to("isds:download?username={{isds.ovm.login}}&password={{isds.ovm.password}}" +
                                "&environment=test&downloadListMessages=true")
                        .log("done");
            }
        };
    }
}
