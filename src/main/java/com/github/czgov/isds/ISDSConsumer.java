package com.github.czgov.isds;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.apache.camel.support.SynchronizationAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.abclinuxu.datoveschranky.common.entities.Message;
import cz.abclinuxu.datoveschranky.common.entities.MessageEnvelope;
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

    public static Map<String, Object> getMessageHeaders(MessageEnvelope env) {
        Map<String, Object> headers = new HashMap<>();

        headers.put(Constants.MSG_ID, env.getMessageID());
        headers.put(Constants.MSG_SUBJECT, env.getAnnotation());
        headers.put(Constants.MSG_FROM, env.getSender());
        headers.put(Constants.MSG_TO, env.getRecipient());
        return headers;
    }

    @Override
    protected int poll() throws Exception {
        Exchange exchange = endpoint.createExchange();

        // offset is indexed from 1 according ot isds javadoc
        int offset = 1;
        int limit = Integer.MAX_VALUE;
        Date from;
        Date to;
        if (endpoint.isRealtime()) {
            // poll interval = from last poll till now
            to = new Date();
            from = new Date(System.currentTimeMillis() - this.getDelay());
        } else {
            // from stone age till future
            from = endpoint.getFrom();
            to = endpoint.getTo();
        }

        log.debug("Polling filter '{}'", endpoint.getFilter());
        log.debug("Polling interval from '{}' to '{}'", from, to);
        List<MessageEnvelope> envelopes = endpoint.getDataBoxManager()
                .getDataBoxMessagesService()
                .getListOfReceivedMessages(from, to, endpoint.getFilter(), offset, limit);
        log.info("Poll success, found {} message envelopes.", envelopes.size());

        AttachmentStorer storer = new FileAttachmentStorer(endpoint.getAttachmentStore().toFile());
        for (MessageEnvelope env : envelopes) {
            log.info("Extracting headers of message {}.", env.getMessageID());
            exchange.getIn().setHeaders(getMessageHeaders(env));

            if (endpoint.isZfo()) {
                log.info("Downloading message {} in binary pkcs signed zfo stream.", env.getMessageID());
                // download data to this output stream
                OutputStream os = new ByteArrayOutputStream();
                endpoint.getDataBoxManager().getDataBoxDownloadService().downloadSignedMessage(env, os);
                exchange.getIn().setBody(os);
                // assume saving exchange to file and by default fill proper filename header
                exchange.getIn().setHeader(Exchange.FILE_NAME, env.getMessageID() + ".zfo");
            } else {
                log.info("Downloading message {} in unmarshalled Message instance.", env.getMessageID());
                Message m = endpoint.getDataBoxManager()
                        .getDataBoxDownloadService()
                        .downloadMessage(env, storer);
                exchange.getIn().setBody(m);
            }

            if (endpoint.isMarkDownloaded()) {
                // mark as downloaded only when exchange is successfully routed
                exchange.addOnCompletion(new SynchronizationAdapter() {
                    @Override
                    public void onComplete(Exchange exchange) {
                        log.info("Setting message {} as downloaded", env);
                        // message is marked as downloaded regardless of errors in camel route
                        endpoint.getDataBoxManager().getDataBoxMessagesService().markMessageAsDownloaded(env);
                    }
                });
            }

            try {
                // send message to next processor in the route
                getProcessor().process(exchange);
            } finally {
                // log exception if an exception occurred and was not handled
                if (exchange.getException() != null) {
                    getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
                }
            }
        }
        // number of messages polled
        return envelopes.size();
    }
}
