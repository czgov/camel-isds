package com.github.czgov.isds.internal;

import org.apache.camel.Exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.czgov.isds.Constants;
import com.github.czgov.isds.ISDSEndpoint;
import com.github.czgov.isds.ISDSProducer;

import java.util.List;
import java.util.stream.Collectors;

import cz.abclinuxu.datoveschranky.common.entities.Attachment;
import cz.abclinuxu.datoveschranky.common.entities.DataBox;
import cz.abclinuxu.datoveschranky.common.entities.Message;
import cz.abclinuxu.datoveschranky.common.entities.MessageEnvelope;
import cz.abclinuxu.datoveschranky.common.entities.MessageType;

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
            // currently no upfront data validation. may throw exceptions from java-isds or web service
            log.debug("Building isds message from headers and attachments");
            MessageEnvelope env = new MessageEnvelope();
            env.setAnnotation(exchange.getIn().getHeader(Constants.MSG_SUBJECT, String.class));
            DataBox to = new DataBox(exchange.getIn().getHeader(Constants.MSG_TO, String.class));
            env.setRecipient(to);
            env.setType(MessageType.CREATED);

            List<Attachment> attachments = exchange.getIn().getAttachments().entrySet().stream()
                    // wrap camel attachments with Java ISDS attachment representation
                    .map(entry -> new Attachment(entry.getKey(), new DataHandlerContent(entry.getValue())))
                    // isds attachments must have meta type defined
                    // first message must be main
                    .map(attachment -> {
                        attachment.setMetaType("main");
                        return attachment;
                    })
                    .collect(Collectors.toList());

            message = new Message(env, attachments);
            log.info("Created message from headers and attachments: {}", message);
        }

        log.info("Sending message '{}' to data box id: {}",
                message.getEnvelope().getAnnotation(),
                message.getEnvelope().getRecipient().getDataBoxID()
        );
        log.info("Message id before send: " + message.getEnvelope().getMessageID());
        endpoint.getDataBoxManager().getDataBoxUploadService().sendMessage(message);
        log.info("Message id after send: " + message.getEnvelope().getMessageID());
        exchange.getIn().setBody(message);
    }
}
