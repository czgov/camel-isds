package com.github.czgov.isds;

import org.apache.camel.EndpointInject;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import cz.abclinuxu.datoveschranky.common.ByteArrayAttachmentStorer;
import cz.abclinuxu.datoveschranky.common.entities.Attachment;
import cz.abclinuxu.datoveschranky.common.entities.Message;
import cz.abclinuxu.datoveschranky.common.entities.content.ByteContent;
import cz.abclinuxu.datoveschranky.impl.MessageValidator;

/**
 * Created by jludvice on 9/30/16.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ISDSComponentTest extends ISDSTestBase {

    @EndpointInject(uri = "mock:producer-basic")
    private MockEndpoint mockEndpoint;

    @EndpointInject(uri = "mock:zfo")
    private MockEndpoint zfoMock;

    @Produce(uri = "direct:sender-fo")
    private ProducerTemplate senderFo;

    @Before
    public void initTimeout() throws InterruptedException {
        // avoid sending of test messages before consumer routes are up and running
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
    }

    @Test
    public void sendMessageInBody() {
        String subject = "Test Message OVM -> FO " + new Date();

        Message message = createMessage(getFoId(), subject);
        Message response = template.requestBody(
                "isds:messages?environment=test&username={{isds.ovm.login}}&password={{isds.ovm.password}}", message,
                Message.class);

        String msgId = response.getEnvelope().getMessageID();
        assertNotNull("Sent message must have ID defined", msgId);
        assertTrue("Sent message must have numeric id. Found: " + msgId, Long.parseLong(msgId) > 0);
        System.out.println("result: " + response);
    }

    @Test
    public void recieveMessageInBody() throws InterruptedException, IOException, NoTypeConversionAvailableException, NoSuchAlgorithmException {

        String fileName = "sample.pdf";
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] originalContent;
        // read content and count hash during reading
        try (DigestInputStream sampleStream = new DigestInputStream(this.getClass().getResourceAsStream("/" + fileName), md5)) {
            originalContent = context().getTypeConverter().mandatoryConvertTo(byte[].class, sampleStream);
        }
        // md5 hash of original attachment
        byte[] originalMD5 = md5.digest();

        Attachment sourceAttachment = new Attachment(fileName, new ByteContent(originalContent));
        sourceAttachment.setMetaType("main");
        // send message from FO to OVM
        Message message = createMessage(getOvmId(), "FO->OVM at " + new Date(), sourceAttachment);
        Message response = senderFo.requestBody(message, Message.class);

        // assert we received message with given id
        mockEndpoint.expectedBodiesReceived(response);
        mockEndpoint.assertIsSatisfied(TimeUnit.MINUTES.toMillis(1));

        // assert attachment was stored to filesystem correctly
        Message received = mockEndpoint.getReceivedExchanges().get(0).getIn().getBody(Message.class);
        // assume we send one attachment in test messages
        Attachment a = received.getAttachments().get(0);
        System.out.println("received attachment: " + a);
        String name = a.getDescription();
        Path filePath = Paths.get("target", "atts-ovm", String.format("%s_%s", received.getEnvelope().getMessageID(), name));
        assertTrue("Attachment file should exist: " + filePath, Files.exists(filePath));

        md5.reset();

        try (DigestInputStream digestInputStream = new DigestInputStream(Files.newInputStream(filePath), md5)) {
            int i;
            while ((i = digestInputStream.read()) != -1) {
                // don't care about the data, just need to read it to get digest
            }
        }
        byte[] receivedMD5 = md5.digest();
        assertTrue("Hash of original and received message attachment must be the same", MessageDigest.isEqual(originalMD5, receivedMD5));
    }

    @Test
    public void testZFOrecieve() throws InterruptedException, IOException {
        String msgSubject = "FO to FO2 at " + new Date();
        Message m = createMessage(getFo2Id(), msgSubject);
        senderFo.sendBody(m);

        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.message(0).body().isEqualTo(m.getEnvelope().getMessageID());
        mockEndpoint.assertIsSatisfied();

        Path zfoPath = Paths.get("target", "camel-isds-zfo", m.getEnvelope().getMessageID() + ".zfo");
        assertTrue("There must be file with zfo content at", Files.exists(zfoPath));

        MessageValidator validator = new MessageValidator();
        Message unmarshalled = validator.validateAndCreateMessage(Files.readAllBytes(zfoPath), new ByteArrayAttachmentStorer(), true);
        assertEquals("Unmarshalled subject must be equal to original one.", msgSubject, unmarshalled.getEnvelope().getAnnotation());
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        initProperties();
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("isds:messages?environment=test&username={{isds.ovm.login}}&password={{isds.ovm.password}}" +
                        "&consumer.delay=5s" +
                        "&realtime=true" +
                        "&attachmentStore=target/atts-ovm")
                        .id("from-ovm")
                        .log("new message in OVM inbox - id ${body.envelope.messageID}")
                        .to(mockEndpoint.getEndpointUri());

                from("isds:messages?environment=test&zfo=true&username={{isds.fo2.login}}&password={{isds.fo2.password}}" +
                        "&consumer.delay=5s" +
                        "&realtime=true" +
                        "&attachmentStore=target/atts-fo2")
                        .id("from-fo2")
                        .log("databox id {{isds.fo2.id}} got new message in zfo format")
                        .to("file:target/camel-isds-zfo")
                        .setBody().header(Constants.MSG_ID)
                        .to(mockEndpoint.getEndpointUri());

                from("direct:sender-fo").id("sender-fo").startupOrder(10)
                        .log("sending message as {{isds.fo.login}}")
                        .to("isds:messages?environment=test&username={{isds.fo.login}}&password={{isds.fo.password}}")
                        .log("Message was sent: ${body}");
            }
        };
    }
}
