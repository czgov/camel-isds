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
import cz.abclinuxu.datoveschranky.common.impl.Config;
import cz.abclinuxu.datoveschranky.common.impl.DataBoxEnvironment;
import cz.abclinuxu.datoveschranky.impl.Authentication;
import cz.abclinuxu.datoveschranky.impl.BasicAuthentication;
import cz.abclinuxu.datoveschranky.impl.DataBoxManager;

/**
 * Created by jludvice on 9/30/16.
 */
public abstract class ISDSTestBase extends CamelTestSupport {
    private static final Logger log = LoggerFactory.getLogger(ISDSTestBase.class);
    private String pathToConfigProperties = "file:isds-config.properties";

    private Authentication authFo;
    private DataBoxManager manager;
    
    // ids of test data boxex
    private String ovmId;
    private String foId;
    private String fo2Id;
    
    public static final Config TEST_CONFIG = new Config(DataBoxEnvironment.TEST);
    
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
    public void initProperties() throws Exception {
        log.info("Setting up test properties from {}", getPathToConfigProperties());
        PropertiesComponent testProperties = context.getComponent("properties", PropertiesComponent.class);
        testProperties.setLocation(getPathToConfigProperties());
        
        log.info("init databox ids and manager");
        ovmId = context.resolvePropertyPlaceholders("{{isds.ovm.id}}");
        foId = context.resolvePropertyPlaceholders("{{isds.fo.id}}");
        fo2Id = context.resolvePropertyPlaceholders("{{isds.fo2.id}}");
        
        String username = context.resolvePropertyPlaceholders("{{isds.fo.login}}");
        String password = context.resolvePropertyPlaceholders("{{isds.fo.password}}");
        authFo = new BasicAuthentication(TEST_CONFIG, username, password);
        manager = new DataBoxManager(TEST_CONFIG, authFo);
    }
    
 

    public String getPathToConfigProperties() {
        return pathToConfigProperties;
    }

    public Authentication getAuthFo() {
        return authFo;
    }

    public DataBoxManager getManager() {
        return manager;
    }

    public String getOvmId() {
        return ovmId;
    }

    public String getFoId() {
        return foId;
    }

    public String getFo2Id() {
        return fo2Id;
    }
    
    
}
