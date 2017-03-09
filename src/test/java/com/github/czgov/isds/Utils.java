package com.github.czgov.isds;

import static org.junit.Assert.assertEquals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by jludvice on 9.3.17.
 */
public class Utils {
    private static final Logger log = LoggerFactory.getLogger(ISDSComponentTest.class);

    /**
     * Compute md5 hash of given stream
     *
     * @param stream stream to hash
     * @return hex string md5 hash
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String md5HashStream(InputStream stream) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (DigestInputStream expectDigestStream = new DigestInputStream(stream, digest)) {
            int read = 0;
            while ((read = expectDigestStream.read()) >= 0) {
                // don't care about the data
            }
        }
        return new HexBinaryAdapter().marshal(digest.digest());
    }

    /**
     * Assert two input streams have equal md5 hash
     *
     * @param message assert message
     * @param expectedStream expected data stream
     * @param dataStream the real / tested data stream
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static void assertMD5equals(String message, InputStream expectedStream, InputStream dataStream) throws NoSuchAlgorithmException, IOException {
        String expectedMD5 = md5HashStream(expectedStream);
        String realMD5 = md5HashStream(dataStream);
        log.debug("Comparing hashes expected '{}' and real {}", expectedMD5, realMD5);
        assertEquals(message, expectedMD5, realMD5);
    }
}
