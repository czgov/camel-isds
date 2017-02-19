package com.github.czgov.isds.internal;

import org.apache.camel.Exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.czgov.isds.ISDSEndpoint;
import com.github.czgov.isds.ISDSProducer;

import cz.abclinuxu.datoveschranky.common.entities.Message;

/**
 * Created by jludvice on 17.2.17.
 */
public class ISDSMessagesProducer extends ISDSProducer {
    private static final Logger log = LoggerFactory.getLogger(ISDSMessagesProducer.class);

    public ISDSMessagesProducer(ISDSEndpoint endpoint) {
        super(endpoint);
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
