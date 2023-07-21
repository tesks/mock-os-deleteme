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
package ammos.datagen.pdu.generators;

import ammos.datagen.generators.ISeededGenerator;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.TruthFile;
import ammos.datagen.pdu.PduType;
import ammos.datagen.pdu.app.client.CfdpClient;
import ammos.datagen.pdu.generators.seeds.PduBodyGeneratorSeed;
import cfdp.engine.Data;
import cfdp.engine.TransID;
import jpl.gds.shared.types.Triplet;

/**
 * This is the data generator for PDU bodies. It creates the portion of an PDU
 * packet from the end of the header onwards. It must be supplied a
 * seed object that tells it which PDUs to generate.
 * 
 *
 */
public class PduBodyGenerator implements ISeededGenerator {

    private TruthFile            truthWriter;
    private PduBodyGeneratorSeed seedData;
    private final CfdpClient           client;

    /**
     * Constructor
     * 
     * @param client CFDP client
     */
    public PduBodyGenerator(final CfdpClient client) {
        this.client = client;
    }

    /**
     * Constructor that sets the truth file writer
     * 
     * @param client CFDP client
     * @param truthWriter TruthFile object for writing truth data to
     */
    public PduBodyGenerator(final CfdpClient client, final TruthFile truthWriter) {
        this(client);
        this.truthWriter = truthWriter;
    }

    @Override
    public void setSeedData(final ISeedData seed) throws InvalidSeedDataException, IllegalArgumentException {
        if (!(seed instanceof PduBodyGeneratorSeed)) {
            throw new IllegalArgumentException("Seed must be of type PduBodyGeneratorSeed");
        }
        this.seedData = (PduBodyGeneratorSeed) seed;
    }

    @Override
    public void reset() {
        this.seedData = null;
    }

    @Override
    public Object getNext() {
        if (this.seedData == null) {
            throw new IllegalStateException("PDU body generator is not seeded");
        }

        return getPduBody(client);
    }

    @Override
    public Object getRandom() {
        // not supported
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get() {
        if (this.seedData == null) {
            throw new IllegalStateException("PDU body generator is unseeded");
        }
        return getNext();
    }

    /**
     * Writes a line of test to the truth file. Does nothing if no truth file
     * writer has been set.
     * 
     * @param truth
     *            the truth data to write
     */
    private void writeTruthLine(final String truth) {
        if (this.truthWriter != null) {
            this.truthWriter.writeLine(truth);
        }
    }

    /**
     * Writes the given PDU and its metadata to the truth file.
     * 
     * @param pdu PDU as Triplet<Data, PduType, TransID>  
     */
    private void writePduToTruth(final Triplet<Data, PduType, TransID> pdu) {
        final StringBuilder builder = new StringBuilder();
        builder.append("PDU: ");
        builder.append(pdu.getTwo().name());
        builder.append(",");
        builder.append(pdu.getOne().length);
        builder.append(",");
        builder.append(pdu.getThree().toString());
        writeTruthLine(builder.toString());
    }

    /**
     * Generates a binary PDU body and writes to truth file
     * 
     * @param client CFDP client
     * @return array of bytes containing the PDU data, or null if we skip the PDU
     */
    private byte[] getPduBody(final CfdpClient client) {
        final Triplet<Data, PduType, TransID> pdu = client.getCurrentPdu();
        if (pdu != null) {
            writePduToTruth(pdu);
            return pdu.getOne().content;
        }
        else {
            writeTruthLine("<skipped>");
        }
        return null;
    }

}
