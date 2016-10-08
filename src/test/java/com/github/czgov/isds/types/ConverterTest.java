package com.github.czgov.isds.types;

import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Test;

import java.nio.file.Path;

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
}
