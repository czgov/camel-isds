package com.github.czgov.isds;

import static com.github.czgov.isds.internal.Utils.createInstance;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.czgov.isds.internal.ISDSEnvironment;
import com.github.czgov.isds.internal.ISDSOperation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.EnumSet;

import cz.abclinuxu.datoveschranky.common.entities.MessageState;
import cz.abclinuxu.datoveschranky.impl.Authentication;
import cz.abclinuxu.datoveschranky.impl.BasicAuthentication;
import cz.abclinuxu.datoveschranky.impl.DataBoxManager;

/**
 * Represents a ISDS endpoint.
 */
@UriEndpoint(scheme = "isds", title = "ISDS", syntax = "isds:messages?opt=value", consumerClass = ISDSConsumer.class, label = "ISDS")
public class ISDSEndpoint extends DefaultEndpoint {
    private static final Logger log = LoggerFactory.getLogger(ISDSEndpoint.class);

    @UriPath
    @Metadata(required = "true")
    private ISDSOperation operation;

    @UriParam(enums = "production,test", defaultValue = "production")
    @Metadata(required = "false")
    private ISDSEnvironment environment = ISDSEnvironment.PRODUCTION;

    @UriParam(description = "Username for ISDS system.")
    @Metadata(required = "true")
    private String username;

    @UriParam(description = "Password for ISDS system")
    @Metadata(required = "true")
    private String password;

    @UriParam(defaultValue = "true", label = "consumer", description = "Set ISDS message as downloaded after successful processing of exchange?")
    private boolean markDownloaded = true;

    @UriParam(defaultValue = Constants.DEFAULT_ATTACHMENT_STORE, description = "folder for storing message attachments")
    private Path attachmentStore = Paths.get(Constants.DEFAULT_ATTACHMENT_STORE);

    @UriParam(defaultValue = "false", label = "consumer", description = "Download message as binary (signed) zfo data instead of Message instance.")
    private boolean zfo = false;

    @UriParam(defaultValue = "!read", label = "consumer", description = "Download only messages which are specified in filter. Null or empty for all.")
    private EnumSet<MessageState> filter = getCamelContext().getTypeConverter().convertTo(EnumSet.class, "!read");

    @UriParam(defaultValue = "0L", label = "consumer,advanced")
    private Date from = new Date(0L);

    @UriParam(defaultValue = "Long.MAX_VALUE", label = "consumer,advanced")
    private Date to = new Date(Long.MAX_VALUE);

    @UriParam(defaultValue = "false")
    private boolean realtime = false;

    private Authentication dataBoxAuth;
    private DataBoxManager dataBoxManager;

    public ISDSEndpoint(String uri, ISDSComponent component) {
        super(uri, component);
    }

    /**
     * Populate instance of {@link Authentication} and {@link DataBoxManager}.
     * This method must be called once {@code @UriParam} fields are resolved.
     * In constructor it's too early.
     */
    private void initDataBox() throws IOException {
        if (dataBoxAuth == null) {
            log.debug("Initializing DataBoxManager with env {} and login {}.", environment, username);
            dataBoxAuth = new BasicAuthentication(environment.getConfig(), username, password);
            dataBoxManager = new DataBoxManager(environment.getConfig(), dataBoxAuth);
            attachmentStore = attachmentStore.toAbsolutePath();
            log.info("Initializing attachment store {}", attachmentStore);
            Files.createDirectories(attachmentStore);
        }
    }

    /**
     * Create producer for specified in endpoint {@link ISDSEndpoint#getOperation()}.
     *
     * @return producer instance
     * @throws Exception
     */
    public Producer createProducer() throws Exception {
        initDataBox();
        return createInstance(operation.getProducerClass(), this);
    }

    /**
     * Create consumer specified in endpoint {@link ISDSEndpoint#getOperation()}.
     *
     * @param processor processor
     * @return consumer instance
     * @throws Exception
     */
    public Consumer createConsumer(Processor processor) throws Exception {
        initDataBox();
        // each operation has it's own consumer class
        Consumer consumer = createInstance(operation.getConsumerClass(), this, processor);
        configureConsumer(consumer);
        return consumer;
    }

    public boolean isSingleton() {
        return true;
    }

    public ISDSOperation getOperation() {
        return operation;
    }

    /**
     * Which operation should be used with isds.
     */
    public void setOperation(ISDSOperation operation) {
        this.operation = operation;
    }

    public ISDSEnvironment getEnvironment() {
        return environment;
    }

    /**
     * Determine if you want to use real ISDS system, or just testing one.
     * Default value is real (production) system.
     *
     * @param environment allowed values are {@link ISDSEnvironment#PRODUCTION} or {@link ISDSEnvironment#TEST}
     */
    public void setEnvironment(ISDSEnvironment environment) {
        this.environment = environment;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Authentication getDataBoxAuth() {
        return dataBoxAuth;
    }

    public DataBoxManager getDataBoxManager() {
        return dataBoxManager;
    }

    public boolean isMarkDownloaded() {
        return markDownloaded;
    }

    public void setMarkDownloaded(boolean markDownloaded) {
        this.markDownloaded = markDownloaded;
    }

    public Path getAttachmentStore() {
        return attachmentStore;
    }

    public void setAttachmentStore(Path attachmentStore) {
        this.attachmentStore = attachmentStore;
    }

    public boolean isZfo() {
        return zfo;
    }

    public void setZfo(boolean zfo) {
        this.zfo = zfo;
    }

    public EnumSet<MessageState> getFilter() {
        return filter;
    }

    public void setFilter(EnumSet<MessageState> filter) {
        this.filter = filter;
    }

    public Date getFrom() {
        return from;
    }

    /**
     * Download only messages received after this date.
     * If using the URI, the pattern expected is: {@code yyyy-MM-dd HH:mm:ss} or {@code yyyy-MM-dd'T'HH:mm:ss}.
     */
    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    /**
     * Download only messages received before this date.
     * If using the URI, the pattern expected is: {@code yyyy-MM-dd HH:mm:ss} or {@code yyyy-MM-dd'T'HH:mm:ss}.
     */
    public void setTo(Date to) {
        this.to = to;
    }

    public boolean isRealtime() {
        return realtime;
    }

    /**
     * Setting realtime to {@code true} will override options {@code from,to}.
     * Assuming {@code consumer.delay=1m}, then {@code from=now - 1 minute} and {@code to=now}.
     */
    public void setRealtime(boolean realtime) {
        this.realtime = realtime;
    }
}
