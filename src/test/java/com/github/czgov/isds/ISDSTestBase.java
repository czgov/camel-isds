package com.github.czgov.isds;

import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Before;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import cz.abclinuxu.datoveschranky.common.Config;
import cz.abclinuxu.datoveschranky.common.DataBoxEnvironment;
import cz.abclinuxu.datoveschranky.common.entities.Attachment;
import cz.abclinuxu.datoveschranky.common.entities.DataBox;
import cz.abclinuxu.datoveschranky.common.entities.Message;
import cz.abclinuxu.datoveschranky.common.entities.MessageEnvelope;
import cz.abclinuxu.datoveschranky.common.entities.content.ByteContent;
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

    public static Message createMessage(String recipientId, String subject, Attachment... attachments) {
        MessageEnvelope env = new MessageEnvelope();
        env.setRecipient(new DataBox(recipientId));
        env.setAnnotation(subject);
        return new Message(env, Arrays.asList(attachments));
    }

    public static Message createMessage(String recipientId, String subject) {
        Attachment attachment = new Attachment("Awesome attachment.txt", new ByteContent("Červeňoučký kůň.\n".getBytes(Charset.forName("utf-8"))));
        attachment.setMetaType("main");
        return createMessage(recipientId, subject, attachment);
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

        // mark all messages as read to have clean state every time
        // unfortunately isds doesn't allow deleting
        markMessagesRead(context.resolvePropertyPlaceholders("{{isds.ovm.login}}"), context.resolvePropertyPlaceholders("{{isds.ovm.password"));
        markMessagesRead(context.resolvePropertyPlaceholders("{{isds.fo.login}}"), context.resolvePropertyPlaceholders("{{isds.fo.password"));
        markMessagesRead(context.resolvePropertyPlaceholders("{{isds.fo2.login}}"), context.resolvePropertyPlaceholders("{{isds.fo2.password"));
    }

    /**
     * Mark all messages read for given accountovmId
     *
     * @param login isds login
     * @param password isds password
     * @throws NoTypeConversionAvailableException
     */
    public void markMessagesRead(String login, String password) throws NoTypeConversionAvailableException {
        Authentication auth = new BasicAuthentication(TEST_CONFIG, login, password);
        DataBoxManager manager = new DataBoxManager(TEST_CONFIG, auth);

        log.info("Marking meessages as downloaded for login {}", login);
        List<MessageEnvelope> envs = manager.getDataBoxMessagesService()
                .getListOfReceivedMessages(
                        new Date(0L), new Date(Long.MAX_VALUE),
                        context().getTypeConverter().mandatoryConvertTo(EnumSet.class, "!read"),
                        1, Integer.MAX_VALUE);
        envs.stream().forEach(env -> manager.getDataBoxMessagesService().markMessageAsDownloaded(env));
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
