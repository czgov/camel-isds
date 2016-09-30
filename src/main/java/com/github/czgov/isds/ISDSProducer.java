package com.github.czgov.isds;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.abclinuxu.datoveschranky.common.entities.Message;

/**
 * The ISDS producer.
 */
public class ISDSProducer extends DefaultProducer {
    private static final Logger log = LoggerFactory.getLogger(ISDSProducer.class);
    private ISDSEndpoint endpoint;

    public ISDSProducer(ISDSEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn().getBody(Message.class);
        if (message == null) {
            //todo(jludvice) we need to construct message from exchange headers and attachments
        }

        log.info("Sending message '{}' to data box id: {}",
                message.getEnvelope().getAnnotation(),
                message.getEnvelope().getRecipient().getDataBoxID()
        );
        log.info("Message id before send: " + message.getEnvelope().getMessageID());
        endpoint.getDataBoxManager().getDataBoxUploadService().sendMessage(message);
        log.info("Message id after send: " + message.getEnvelope().getMessageID());
    }
}
