package com.github.czgov.isds.internal;

import org.apache.camel.util.component.ApiName;

import com.github.czgov.isds.ISDSConsumer;
import com.github.czgov.isds.ISDSProducer;

/**
 * Supported ISDS operations.
 */
public enum ISDSOperation implements ApiName {
    MESSAGES("messages", ISDSMessagesConsumer.class, ISDSMessagesProducer.class),
    DOWNLOAD("download", null, ISDSDownloadProducer.class);

    private final String name;
    private final Class<? extends ISDSConsumer> consumerClass;
    private final Class<? extends ISDSProducer> producerClass;

    ISDSOperation(String name,
            Class<? extends ISDSConsumer> consumerClass,
            Class<? extends ISDSProducer> producerClass
    ) {
        this.name = name;
        this.consumerClass = consumerClass;
        this.producerClass = producerClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public Class<? extends ISDSConsumer> getConsumerClass() {
        return consumerClass;
    }

    public Class<? extends ISDSProducer> getProducerClass() {
        return producerClass;
    }

    /**
     * Same like {@code valueOf(String)} except this is case insensitive.
     *
     * @param value case insensitive value name
     * @return enum value
     */
    public static ISDSOperation fromValue(String value) {
        return ISDSOperation.valueOf(String.valueOf(value).toUpperCase());
    }
}
