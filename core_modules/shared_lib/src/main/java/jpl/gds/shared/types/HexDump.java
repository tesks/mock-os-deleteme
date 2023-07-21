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
package jpl.gds.shared.types;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;


/**
 * Dump out bytes in hex.
 *
 */
public class HexDump {

    private static final String[] hexDigits = {
        "0", "1", "2", "3", "4", "5", "6", "7",
        "8", "9", "a", "b", "c", "d", "e", "f"
    };
    private int counter = 0;
    private StringBuilder hexBuffer = new StringBuilder();
    private StringBuilder asciiBuffer = new StringBuilder();
    private long lineAddress = 0;
    private List<Object> printArgs = new ArrayList<Object>();
    private Writer writer;

    /**
     * Constructor.
     */
    public HexDump() { }


    /**
     * Constructor.
     *
     * @param writer Writer
     */
    public HexDump(Writer writer) {
        setWriter(writer);
    }


    /**
     * Set writer.
     *
     * @param writer Writer
     */
    public void setWriter(Writer writer) {
        this.writer = writer;
    }


    /**
     * Reset.
     */
    public void reset() {
        counter = 0;
        hexBuffer = new StringBuilder();
        asciiBuffer = new StringBuilder();
        lineAddress = 0;
    }


    /**
     * Handle bytes. Don't forget to call handleEof() at the end of the stream.
     *
     * @param bytes Byte array
     *
     * @throws IOException I/O error
     */
    public void handleBytes(byte[] bytes) throws IOException {
        for (int i = 0; i < bytes.length; ++i) {
            handleByte(bytes[i]);
        }
    }


    /**
     * Handle bytes. Don't forget to call handleEof() at the end of the stream.
     *
     * @param bytes Byte array
     * @param off   Offset
     * @param len   Length
     *
     * @throws IOException I/O error
     */
    public void handleBytes(byte[] bytes, int off, int len) throws IOException {
        for (int i = 0; i < len; ++i) {
            handleByte(bytes[off+i]);
        }
    }

    /**
     * Handle byte. Don't forget to call handleEof() at the end of the stream.
     *
     * @param b Byte
     *
     * @throws IOException I/O error
     */
    public void handleByte(byte b) throws IOException {
        if ((counter % 4) == 0) {
            hexBuffer.append(' ');
        }
        hexBuffer.append(makeHex(b));
        asciiBuffer.append(makeAscii(b));
        ++counter;
        if (counter == 16) {
            printLine();

            hexBuffer = new StringBuilder();
            asciiBuffer = new StringBuilder();
            counter = 0;
            lineAddress += 16;
        }
    }

    /**
     * Handle byte stream. Implicitly handles end of stream also.
     *
     * @param stream Byte stream
     *
     * @throws IOException I/O error
     */
    public void handleByteStream(ByteStream stream) throws IOException {
        ByteArraySlice slice = null;
        while (stream.hasMore()) {
            slice = stream.read(1);
            handleByte(slice.array[slice.offset]);
        }

        writer.flush();
    }

    /**
     * Must be called to flush any remaining partial line
     *
     * @throws IOException I/O error
     */
    public void handleEof() throws IOException {
        if (counter != 0) {
            for (int i = counter; i < 16; ++i) {
                if ((i % 4) == 0) {
                    hexBuffer.append(' ');
                }
                hexBuffer.append("  ");
            }
            printLine();
        }
        writer.flush();
    }

    private void printLine() throws IOException {
        printArgs.clear();
        printArgs.add(Long.valueOf(lineAddress));
        printArgs.add(hexBuffer.toString());
        printArgs.add(asciiBuffer.toString());

        writer.write(String.format("%08x   %s    %s\n", printArgs.get(0), printArgs.get(1), printArgs.get(2)));
    }

    private String makeHex(byte b) {
        return hexDigits[(b >> 4) & 0x0F] + hexDigits[b & 0x0F];
    }

    private String makeAscii(byte b) {
        if ((b < 32) || (b > 126)) {
            return ".";
        }
        byte[] bytes = { b };
        return new String(bytes);
    }

}
