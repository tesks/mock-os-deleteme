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
package ammos.datagen.generators.util;

import java.io.FileWriter;
import java.io.IOException;

import jpl.gds.ccsds.api.packet.IPacketFormatDefinition;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.ISclk;

/**
 * This class provides methods for generating a truth file for the data
 * generator. Truth data is a packet by packet description of the data file
 * generated.
 * 
 *
 */
public class TruthFile {
    private static final String START_RECORD_DELIM = "Record: ";
    private static final String END_RECORD_DELIM = "";

    private final Tracer statusLogger = TraceManager.getDefaultTracer();

    private FileWriter truthWriter;
    private final int packetHeaderLength;

    /**
     * Creates and opens the truth file.
     * 
     * @param filePath
     *            path to the truth file
     * @throws IOException
     *             if the file cannot be opened
     */
    public TruthFile(final String filePath, IPacketFormatDefinition packetFormat) throws IOException {

        this.truthWriter = new FileWriter(filePath);
        final ISpacePacketHeader header = PacketHeaderFactory.create(packetFormat);
        this.packetHeaderLength = header.getPrimaryHeaderLength();
    }

    /**
     * Writes a line of text to the truth file. Attaches a line feed to the
     * specified text.
     * 
     * @param line
     *            text to write
     */
    public synchronized void writeLine(final String line) {

        try {
            if (this.truthWriter != null) {
                this.truthWriter.write(line + "\n");
                this.truthWriter.flush();
            }
        } catch (final IOException e) {
            this.statusLogger.error("I/O Error writing truth file", e);
        }
    }

    /**
     * Writes a packet entry to the truth file.
     * 
     * @param header
     *            the ISpacePacketHeader object for the current packet
     * @param sclk
     *            the SCLK value for the current packet
     */
    public void writePacket(final ISpacePacketHeader header, final ISclk sclk) {

        if (this.truthWriter != null) {
            writeLine("Packet: "
                    + header.getApid()
                    + ","
                    + header.getSourceSequenceCount()
                    + ","
                    + (header.getPacketDataLength() + this.packetHeaderLength + 1)
                    + "," + sclk.toString());
        }
    }

    /**
     * Writes a fill packet entry to the truth file.
     * 
     * @param header
     *            the ISpacePacketHeader object for the current packet
     * @param sclk
     *            the SCLK value for the current packet
     */
    public void writeFillPacket(final ISpacePacketHeader header, final ISclk sclk) {

        if (this.truthWriter != null) {
            writeLine("Fill Packet: "
                    + header.getApid()
                    + ","
                    + header.getSourceSequenceCount()
                    + ","
                    + (header.getPacketDataLength() + this.packetHeaderLength + 1)
                    + "," + sclk.toString());
        }
    }

    /**
     * Writes a record delimiter to the truth file.
     * 
     * @param recordCounter
     *            the current record count
     */
    public void writeRecord(final long recordCounter) {

        writeLine(START_RECORD_DELIM + recordCounter + END_RECORD_DELIM);
    }

    /**
     * Closes the truth file. It is important to synchronize the close with
     * writes.
     */
    public synchronized void close() {

        try {
            if (this.truthWriter != null) {
                this.truthWriter.close();
                this.truthWriter = null;
            }
        } catch (final IOException e) {
            this.statusLogger.error("IO Error closing truth file", e);
        }
    }
}
