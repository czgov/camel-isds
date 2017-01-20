package com.github.czgov.isds.types;

import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Test;

import java.nio.file.Path;
import java.util.EnumSet;

import cz.abclinuxu.datoveschranky.common.entities.MessageState;

/**
 * Created by jludvice on 8.10.16.
 */
public class ConverterTest extends CamelTestSupport {

    @Test
    public void testPathConverter() {
        String p = "/some/crazy/path";
        Path path = context.getTypeConverter().convertTo(Path.class, p);
        assertNotNull("Converter must return some value", path);
        assertTrue("Path.getParent must contain 'crazy'", path.getParent().toString().contains("crazy"));
        // check we don't throw NPE accidentally
        assertNull("Convert of null must return null", context.getTypeConverter().convertTo(Path.class, null));
    }

    @Test
    public void testFilterParser() {
        String unread = "!read";
        @SuppressWarnings("unchecked")
        EnumSet<MessageState> unreadFilter = context.getTypeConverter().convertTo(EnumSet.class, unread);
        assertNotNull("String filter must be converted to enumset", unreadFilter);
        assertFalse("There can't be element read", unreadFilter.contains(MessageState.READ));
        // there must be everything else
        for (MessageState s : MessageState.values()) {
            if (MessageState.READ.equals(s)) {
                continue;
            }
            assertTrue("There must be element " + s, unreadFilter.contains(s));
        }
        assertEquals("Enumset must have proper size", MessageState.values().length - 1, unreadFilter.size());

        String deliveredBy = "delivered_by_login, delivered_by_fiction";
        @SuppressWarnings("unchecked")
        EnumSet<MessageState> deliveredByFilter = context.getTypeConverter().convertTo(EnumSet.class, deliveredBy);
        System.out.println("deliveredByFilter: " + deliveredByFilter);
        for (MessageState s : MessageState.values()) {
            if (MessageState.DELIVERED_BY_FICTION.equals(s) || MessageState.DELIVERED_BY_LOGIN.equals(s)) {
                assertTrue("There must be element " + s, deliveredByFilter.contains(s));
            } else {
                assertFalse("There can't be element " + s, deliveredByFilter.contains(s));
            }
        }
        assertEquals("Enumset must have proper size", 2, deliveredByFilter.size());
    }
}
