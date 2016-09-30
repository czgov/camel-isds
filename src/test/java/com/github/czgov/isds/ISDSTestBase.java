package com.github.czgov.isds;

import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Before;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import cz.abclinuxu.datoveschranky.common.entities.Attachment;
import cz.abclinuxu.datoveschranky.common.entities.DataBox;
import cz.abclinuxu.datoveschranky.common.entities.Message;
import cz.abclinuxu.datoveschranky.common.entities.MessageEnvelope;
import cz.abclinuxu.datoveschranky.common.entities.content.ByteContent;

/**
 * Created by jludvice on 9/30/16.
 */
public abstract class ISDSTestBase extends CamelTestSupport {
    public static final Logger log = LoggerFactory.getLogger(ISDSTestBase.class);
    private String pathToConfigProperties = "file:isds-config.properties";

    public static Message createMessage(String recipientId, String subject) {
        MessageEnvelope env = new MessageEnvelope();
        env.setRecipient(new DataBox(recipientId));
        env.setAnnotation(subject);

        Attachment attachment = new Attachment("Awesome attachment.txt", new ByteContent("Červeňoučký kůň.\n".getBytes(Charset.forName("utf-8"))));
        attachment.setMetaType("main");
        List<Attachment> attachments = Arrays.asList(attachment);
        return new Message(env, attachments);
    }

    @Before
    public void initProperties() {
        log.info("Setting up test properties from {}", getPathToConfigProperties());
        PropertiesComponent testProperties = context.getComponent("properties", PropertiesComponent.class);
        testProperties.setLocation(getPathToConfigProperties());
    }

    public String getPathToConfigProperties() {
        return pathToConfigProperties;
    }
}
