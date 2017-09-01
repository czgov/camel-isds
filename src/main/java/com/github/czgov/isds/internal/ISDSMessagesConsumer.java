package com.github.czgov.isds.internal;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.SynchronizationAdapter;

import com.github.czgov.isds.ISDSConsumer;
import com.github.czgov.isds.ISDSEndpoint;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import cz.abclinuxu.datoveschranky.common.entities.Attachment;
import cz.abclinuxu.datoveschranky.common.entities.Message;
import cz.abclinuxu.datoveschranky.common.entities.MessageEnvelope;
import cz.abclinuxu.datoveschranky.common.entities.content.FileContent;
import cz.abclinuxu.datoveschranky.common.interfaces.AttachmentStorer;

/**
 * Created by jludvice on 17.2.17.
 */
public class ISDSMessagesConsumer extends ISDSConsumer {
    public ISDSMessagesConsumer(ISDSEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
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

        for (MessageEnvelope env : envelopes) {
            log.info("Extracting headers of message {}.", env.getMessageID());
            exchange.getIn().setHeaders(getMessageHeaders(env));

            if (endpoint.isZfo()) {
                downloadZFO(exchange, env, endpoint);
            } else {
                downloadMessage(exchange, env, endpoint, storer);
            }

            if (endpoint.isMarkDownloaded()) {
                // mark as downloaded only when exchange is successfully routed
                exchange.addOnCompletion(new SynchronizationAdapter() {
                    @Override
                    public void onComplete(Exchange exchange) {
                        log.info("Setting message {} as downloaded", env);
                        endpoint.getDataBoxManager()
                                .getDataBoxMessagesService()
                                .markMessageAsDownloaded(env);
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

    static void downloadMessage(Exchange exchange, MessageEnvelope env, ISDSEndpoint endpoint, AttachmentStorer storer) {
        log.info("Downloading message {} in unmarshalled Message instance.", env.getMessageID());
        Message m = endpoint.getDataBoxManager()
                .getDataBoxDownloadService()
                .downloadMessage(env, storer);

        exchange.getIn().setBody(m);
        // map attachments to camel
        for (Attachment a : m.getAttachments()) {
            // get attachment file and create data handler from it
            FileContent fc = (FileContent) a.getContent();
            exchange.getIn().addAttachment(a.getDescription(), createDataHandler(fc.getFile()));
        }
    }

    /**
     * Download message in zfo format and store it as OutputStream in Exchange body and set Camel file name header.
     *
     * @param exchange store message to exchange body
     * @param env envelope of requested message
     * @param endpoint endpoint providing databox manager instance
     */
    static void downloadZFO(Exchange exchange, MessageEnvelope env, ISDSEndpoint endpoint) {
        log.info("Downloading message {} in binary pkcs signed zfo stream.", env.getMessageID());
        // download data to this output stream
        OutputStream os = new ByteArrayOutputStream();
        endpoint.getDataBoxManager().getDataBoxDownloadService().downloadSignedMessage(env, os);
        exchange.getIn().setBody(os);
        // assume saving exchange to file and by default fill proper filename header
        exchange.getIn().setHeader(Exchange.FILE_NAME, env.getMessageID() + ".zfo");
    }
}
