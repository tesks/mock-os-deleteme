/*
 * Copyright 2006-2018. California Institute of Technology.
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
package jpl.gds.shared.process;

import java.io.IOException;

/**
 * This can be used to collect all the output from a process into one string.
 * 
 */
public class BufferLineHandler implements LineHandler {

    /**
     * Buffer used to append new handled lines.
     */
    protected StringBuffer buffer = new StringBuffer();

    /**
     * Adds the line to the buffer, and also adds the end-of-line character.
     * 
     * @param line
     *            string to append to the buffer.
     * @throws IOException
     *             allows subclassed buffer handlers to throw an IOException
     */
    public void handleLine(String line) throws IOException {
        buffer.append(line);
        buffer.append(Character.LINE_SEPARATOR);
    }

    /**
     * Empties the string buffer.
     */
    public void clear() {
        buffer = new StringBuffer();
    }

    /**
     * Returns the contents of the string buffer.
     * 
     * @return buffer to string
     */
    public String toString() {
        return buffer.toString();
    }

}
