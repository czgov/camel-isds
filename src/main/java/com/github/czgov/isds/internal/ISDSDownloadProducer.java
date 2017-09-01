package com.github.czgov.isds.internal;

import org.apache.camel.Exchange;
import org.apache.camel.util.ObjectHelper;

import com.github.czgov.isds.Constants;
import com.github.czgov.isds.ISDSEndpoint;
import com.github.czgov.isds.ISDSProducer;

import java.util.Date;

import cz.abclinuxu.datoveschranky.common.FileAttachmentStorer;
import cz.abclinuxu.datoveschranky.common.entities.MessageEnvelope;
import cz.abclinuxu.datoveschranky.common.entities.MessageType;

/**
 * Created by jludvice on 19.7.17.
 */
public class ISDSDownloadProducer extends ISDSProducer {
    public ISDSDownloadProducer(ISDSEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        final String id = exchange.getIn().getHeader(Constants.MSG_ID, String.class);
        final MessageType type = exchange.getIn().getHeader(Constants.MSG_TYPE, "received", MessageType.class);

        ObjectHelper.notNull(id, "Camel header " + Constants.MSG_ID);

        System.out.printf("fetching %s message #%s\n", type, id);

        MessageEnvelope env = new MessageEnvelope();
        env.setMessageID(id);
        env.setType(type);

        if (MessageType.RECEIVED.equals(type) && endpoint.isDownloadListMessages()) {
            // hack around isds policy which doesn't allow downloading of messages
            // which arrived before user logged in (or called getListOfReceivedMessages API method
            Date d = new Date();
            log.info("Calling getListOfReceivedMessages before downloading message by id {}", id);
            // don't care about result, just need ISDS to mark messages as delivered
            endpoint.getDataBoxManager()
                    .getDataBoxMessagesService()
                    .getListOfReceivedMessages(d, d, null, 1, 1);
        }

        if (MessageType.SENT.equals(type) || endpoint.isZfo()) {
            ISDSMessagesConsumer.downloadZFO(exchange, env, endpoint);
        } else {
            ISDSMessagesConsumer.downloadMessage(exchange, env, endpoint,
                    new FileAttachmentStorer(endpoint.getAttachmentStore().toFile()));
        }
        log.info("probably need TODO :)");
    }
}
