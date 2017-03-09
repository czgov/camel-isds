package com.github.czgov.isds.internal;

import javax.activation.DataHandler;

import java.io.IOException;
import java.io.InputStream;

import cz.abclinuxu.datoveschranky.common.entities.content.Content;

/**
 * Wrapper for passing Camel attachment data handler into ISDS {@link cz.abclinuxu.datoveschranky.common.entities.Attachment} content representation.
 */
public class DataHandlerContent implements Content {
    private DataHandler dataHandler;

    public DataHandlerContent(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return dataHandler.getInputStream();
    }

    /**
     * Not supported
     *
     * @return always {@code -1}
     */
    @Override
    public long estimatedSize() {
        return -1;
    }
}
