package com.github.czgov.isds;

import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cz.abclinuxu.datoveschranky.common.FileAttachmentStorer;
import cz.abclinuxu.datoveschranky.common.entities.MessageEnvelope;
import cz.abclinuxu.datoveschranky.common.interfaces.AttachmentStorer;

/**
 * The ISDS consumer.
 */
public abstract class ISDSConsumer extends ScheduledPollConsumer {

    public static final Logger log = LoggerFactory.getLogger(ISDSConsumer.class);

    protected final ISDSEndpoint endpoint;
    protected final AttachmentStorer storer;

    public ISDSConsumer(ISDSEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        storer = new FileAttachmentStorer(endpoint.getAttachmentStore().toFile());
        log.info("Attachment store: {}", storer);
    }

    @Override
    protected abstract int poll() throws Exception;

    /**
     * Create map of camel from isds message envelope.
     *
     * @param env isds message envelope
     * @return headers map to be put into camel exchange headers
     */
    public static Map<String, Object> getMessageHeaders(MessageEnvelope env) {
        Map<String, Object> headers = new HashMap<>();

        headers.put(Constants.MSG_ID, env.getMessageID());
        headers.put(Constants.MSG_SUBJECT, env.getAnnotation());
        headers.put(Constants.MSG_FROM, env.getSender());
        headers.put(Constants.MSG_TO, env.getRecipient());
        return headers;
    }

    /**
     * Create data handler for given file.
     *
     * @param file existing file
     * @return datahandler for file
     */
    public static DataHandler createDataHandler(File file) {
        log.debug("creating data handler for {}", file);
        return new DataHandler(new FileDataSource(file));
    }
}
