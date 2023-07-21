/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.shared.types;

/**
 * BitStringBuffer holds a string and the current offset.
 *
 */
public class AmpcsStringBuffer {

    private int offset;
    private String buffer;

    public AmpcsStringBuffer(final String buffer) {
        this(buffer, 0);
    }

    public AmpcsStringBuffer(final String buffer, final int offset) {
        setBuffer(buffer);
        toOffset(offset);
    }

    /**
     * Move the offset to the specified value
     * @param offset the new requested offset
     */
    public void toOffset(final int offset) {
        if(offset < 0) {
            throw new IllegalArgumentException("The supplied offset cannot be negative");
        } else if(offset > buffer.length()) {
            throw new IllegalArgumentException("The supplied offset must be within the buffer string range");
        }
        this.offset = offset;
    }

    /**
     * Get the current offset
     * @return the current offset
     */
    public int getOffset() {
        return this.offset;
    }

    /**
     * Get the number of characters available from the buffer at the current offset
     * @return the number of available characters
     */
    public int available() {
        return (this.buffer.length() - this.offset);
    }

    private void setBuffer(final String buffer) {
        if(buffer == null) {
            throw new IllegalArgumentException("The supplied buffer cannot be empty");
        }
        this.buffer = buffer;
    }

    /**
     * Read a single character and return it as a char
     * @return the next char in the buffer
     */
    public char readChar() {
        if(offset < buffer.length()) {
            return buffer.charAt(offset++);
        } else {
            throw new StringIndexOutOfBoundsException("Cannot read a character when the offset is at the end of the buffer");
        }
    }

    /**
     * Read a single character and return it as a String.
     * @return A string of the next chacracter in the buffer
     */
    public String read() {
        if(offset < buffer.length()) {
            return buffer.substring(offset++, offset);
        } else {
            return "";
        }
    }

    /**
     * Read and return the specified number of characters from the buffer. If the number of requested characters are not
     * available, the remainder of the string is returned.
     * @param requestedLength the number of characters to be read
     * @return the requested substring
     */
    public String read(final int requestedLength) {
        if(requestedLength < 0) {
            throw new IllegalArgumentException("Cannot request a negative length");
        }
        String retString;
        if(requestedLength >= available()) {
            retString = buffer.substring(offset);
        } else {
            retString = buffer.substring(offset, offset + requestedLength);

        }

        offset += retString.length();

        return retString;
    }

    public String getBuffer() {
        return this.buffer;
    }
}
