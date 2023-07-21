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
package jpl.gds.tcapp.app.reverse;

import jpl.gds.shared.util.BinOctHexUtility;

import java.io.PrintWriter;

/**
 * Class to contain utilities for other data writing classes, such as FrameWriter and PduWriter
 *
 */
public class AbstractDataWriter implements IDataWriter{
    public static final String DOUBLE_LINE              = "\n\n";
    public static final String SINGLE_LINE              = "\n";
    protected final int FRAME_HEADER_SIZE_BYTES     = 5;
    protected final int HEX_BYTES_PER_LINE          = 28;

    protected final PrintWriter printWriter;
    private boolean suppressOutput = false;

    /**
     * Constructor
     *
     * @param printWriter PrintWriter to use to write to the console
     */
    public AbstractDataWriter(final PrintWriter printWriter) {
        this.printWriter = printWriter;
    }

    /**
     * Utility to pretty-print hex to the console
     *
     * @param key
     *          the name of the hex to print ("frame = " for example)
     * @param bytes
     *          the byte array to be printed as hex
     */
    protected void writeHexBlob(String key, byte[] bytes) {
        if (!getSuppressOutput()) {
            String keyFormat = String.format("%s%s%s", key, SINGLE_LINE, "%s%s");
            printWriter.write(
                    String.format(
                            keyFormat,
                            BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(bytes), HEX_BYTES_PER_LINE),
                            SINGLE_LINE)
            );

            printWriter.flush();
        }
    }

    /**
     * Cleanly shuts down the PrintWriter
     */
    public void cleanUp() {
        if (!getSuppressOutput()) {
            printWriter.write(DOUBLE_LINE);
        }

        printWriter.flush();
        printWriter.close();
    }

    @Override
    public void setSuppressOutput(final boolean quiet) {
        this.suppressOutput = quiet;
    }

    @Override
    public boolean getSuppressOutput(){
        return this.suppressOutput;
    }
}
