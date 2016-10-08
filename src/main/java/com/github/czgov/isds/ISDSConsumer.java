package com.github.czgov.isds;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.abclinuxu.datoveschranky.common.entities.Message;
import cz.abclinuxu.datoveschranky.common.entities.MessageEnvelope;
import cz.abclinuxu.datoveschranky.common.entities.MessageState;
import cz.abclinuxu.datoveschranky.common.impl.FileAttachmentStorer;
import cz.abclinuxu.datoveschranky.common.interfaces.AttachmentStorer;
import cz.abclinuxu.datoveschranky.impl.DataBoxManager;

/**
 * The ISDS consumer.
 */
public class ISDSConsumer extends ScheduledPollConsumer {

    public static final Logger log = LoggerFactory.getLogger(ISDSConsumer.class);

    private final ISDSEndpoint endpoint;

    public ISDSConsumer(ISDSEndpoint endpoint, Processor processor, DataBoxManager manager) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    public static Map<String, Object> getMessageHeaders(Message m) {
        Map<String, Object> headers = new HashMap<>();

        headers.put(Constants.MSG_ID, m.getEnvelope().getMessageID());
        headers.put(Constants.MSG_SUBJECT, m.getEnvelope().getAnnotation());
        headers.put(Constants.MSG_FROM, m.getEnvelope().getSender());
        headers.put(Constants.MSG_TO, m.getEnvelope().getRecipient());
        System.out.println(m);
        return headers;
    }

    @Override
    protected int poll() throws Exception {
        Exchange exchange = endpoint.createExchange();

        // compute from - to time window to fetch
        Duration d = Duration.ofMillis(getDelay());
        Instant now = Instant.now();
        Temporal before = d.subtractFrom(now);

        Date from = Date.from(Instant.from(before));
        Date to = Date.from(now);

        EnumSet<MessageState> messageFilter = null;
        // offset is indexed from 1 according ot isds javadoc
        int offset = 1;
        int limit = Integer.MAX_VALUE;


        List<MessageEnvelope> envelopes = endpoint.getDataBoxManager()
                .getDataBoxMessagesService()
                .getListOfReceivedMessages(from, to, messageFilter, offset, limit);

        log.info("Poll {} messsage envelopes.", envelopes.size());

        AttachmentStorer storer = new FileAttachmentStorer(endpoint.getAttachmentStore().toFile());
        for (MessageEnvelope e : envelopes) {
            log.info("Recieving isds msg  id {}.", e.getMessageID());
            Message m = endpoint.getDataBoxManager()
                    .getDataBoxDownloadService()
                    .downloadMessage(e, storer);

            exchange.getIn().setBody(m);
            exchange.getIn().setHeaders(getMessageHeaders(m));

            try {
                // send message to next processor in the route
                getProcessor().process(exchange);
                return envelopes.size(); // number of messages polled
            } finally {
                // log exception if an exception occurred and was not handled
                if (exchange.getException() != null) {
                    getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
                }
            }
        }
        return envelopes.size();
    }
}
