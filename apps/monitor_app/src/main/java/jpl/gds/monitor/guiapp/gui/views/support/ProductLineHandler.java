/*
 * Copyright 2006-2020. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.monitor.guiapp.gui.views.support;

import jpl.gds.shared.process.LineHandler;

import static jpl.gds.shared.string.StringUtil.cleanUpString;

/**
 * Stores product decom text in a string buffer
 *
 */
class ProductLineHandler implements LineHandler {

    private final StringBuffer decomBuffer;
    private int lineCount = 0;
    private boolean firstBufferError = false;
    private int maxDecomDisplayLines;

    public ProductLineHandler(final StringBuffer decomBuffer, final int maxDecomDisplayLines) {
        this.decomBuffer = decomBuffer;
        this.maxDecomDisplayLines = maxDecomDisplayLines;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.process.LineHandler#handleLine(java.lang.String)
     */
    @Override
    public void handleLine(final String line) {
        if (lineCount ==  0 && line.startsWith("INFO")) {
            return;
        }
        if (lineCount < maxDecomDisplayLines) {
            decomBuffer.append(cleanUpString(line) + "\n");
        } else if (!firstBufferError) {
            firstBufferError = true;
            decomBuffer.append("\nOutput is too long for text viewer. Remainder of text truncated.\n");
        }
        lineCount ++;
    }

    /**
     * Converts the decom buffer into a string
     * @return a string representation of the buffer
     */
    public String getDecomText() {
        return decomBuffer.toString();
    }
}
