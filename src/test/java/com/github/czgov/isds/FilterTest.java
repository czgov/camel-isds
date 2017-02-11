package com.github.czgov.isds;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import cz.abclinuxu.datoveschranky.common.entities.Message;
import cz.abclinuxu.datoveschranky.common.entities.MessageEnvelope;
import cz.abclinuxu.datoveschranky.common.entities.MessageState;
import cz.abclinuxu.datoveschranky.common.entities.MessageType;
import cz.abclinuxu.datoveschranky.impl.Authentication;
import cz.abclinuxu.datoveschranky.impl.BasicAuthentication;
import cz.abclinuxu.datoveschranky.impl.DataBoxManager;

/**
 * Created by jludvice on 29.11.16.
 */
public class FilterTest extends ISDSTestBase {

    private static final Logger log = LoggerFactory.getLogger(FilterTest.class);
    private static final String ROUTE_ID = "from-ovm-unread";
    private final String subject = "Msg which will be received only once and then marked as downloaded.";
    private final String subject2 = "Msg 2 which will be received only once and then marked as downloaded.";

    private DataBoxManager ovmManger;

    @EndpointInject(uri = "mock:msg-ovm")
    private MockEndpoint mockEndpoint;

    @Before
    public void markAllMessagesDownloaded() throws Exception {
        //        System.out.println("before");
        //        Message m = createMessage(getOvmId(), "dummy message " + new Date());
        //        getManager().getDataBoxUploadService().sendMessage(m);

        log.info("Marking all messages in OVM databox as read");
        String username = context.resolvePropertyPlaceholders("{{isds.ovm.login}}");
        String password = context.resolvePropertyPlaceholders("{{isds.ovm.password}}");
        Authentication ovmAuth = new BasicAuthentication(TEST_CONFIG, username, password);
        ovmManger = new DataBoxManager(TEST_CONFIG, ovmAuth);

        for (MessageEnvelope env : ovmManger.getDataBoxMessagesService().getListOfReceivedMessages(new Date(0L), new Date(Long.MAX_VALUE), null, 1, Integer.MAX_VALUE)) {
            if (MessageState.READ != env.getState()) {
                log.info("Before cleanup - marking message '{}' as read", env);
                ovmManger.getDataBoxMessagesService().markMessageAsDownloaded(env);
            }
        }

        Message m = createMessage(getOvmId(), subject);
        Message m2 = createMessage(getOvmId(), subject2);
        Message m3 = createMessage(getOvmId(), "m3 will be marked as downloaded");

        getManager().getDataBoxUploadService().sendMessage(m);
        getManager().getDataBoxUploadService().sendMessage(m2);
        getManager().getDataBoxUploadService().sendMessage(m3);

        log.info("Messages sent. ID1: '{}', ID2: '{}', ID3: '{}'", m.getEnvelope().getMessageID(), m2.getEnvelope().getMessageID(), m3.getEnvelope().getMessageID());
        log.info("marking message as downloaded {}", m3);

        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        List<MessageEnvelope> env = ovmManger.getDataBoxMessagesService().getListOfReceivedMessages(new Date(0L), new Date(Long.MAX_VALUE), null, 1, 99);

        MessageEnvelope e = env.stream().filter(i -> i.getAnnotation().equals(m3.getEnvelope().getAnnotation())).findFirst().get();
        log.warn("found {}", e);

        try (OutputStream os = new ByteArrayOutputStream()) {
            log.info("downoading message ID {}", m3.getEnvelope().getMessageID());

            MessageEnvelope me = new MessageEnvelope();
            me.setMessageID(m3.getEnvelope().getMessageID());
            me.setType(MessageType.RECEIVED);

            ovmManger.getDataBoxDownloadService().downloadSignedMessage(me, os);
            ovmManger.getDataBoxMessagesService().markMessageAsDownloaded(m3.getEnvelope());
            log.info("marked message {} as read", m3.getEnvelope().getMessageID());
        }
    }

    @Test
    public void testUnread() throws Exception {

        log.info("Starting route {}", ROUTE_ID);
        context.startRoute(ROUTE_ID);

        int expectedCount = 2;
        mockEndpoint.expectedMessageCount(expectedCount);
        mockEndpoint.assertIsSatisfied();
        // there might more messages coming but mock endpoint will pass once it reaches expected count
        // and won't accept additional msg
        List<Exchange> received = mockEndpoint.getReceivedExchanges();
        log.info("Received messages ({}) '{}'", received.size(), received);
        assertEquals("There should be 2 messages received", expectedCount, received.size());

        Optional<Exchange> ex = received.stream()
                .filter(e -> e.getIn().getHeader(Constants.MSG_SUBJECT).equals(subject))
                .findFirst();

        assertTrue("There must be msg with subject " + subject, ex.isPresent());

        ex = received.stream()
                .filter(e -> e.getIn().getHeader(Constants.MSG_SUBJECT).equals(subject2))
                .findFirst();
        assertTrue("There must be msg with subject " + subject2, ex.isPresent());
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        initProperties();
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("isds:fetch?environment=test&username={{isds.ovm.login}}"
                        + "&password={{isds.ovm.password}}"
                        + "&consumer.delay=1s"
                        + "&filter=!read")
                        .id(ROUTE_ID)
                        .autoStartup(false)
                        .log("new message in OVM inbox - id ${body.envelope.messageID}")
                        .to(mockEndpoint.getEndpointUri());
            }
        };
    }
}
