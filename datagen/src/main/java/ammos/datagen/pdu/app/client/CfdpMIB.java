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
package ammos.datagen.pdu.app.client;

import static cfdp.engine.Response.RESPONSE_CANCEL;

import cfdp.engine.ConditionCode;
import cfdp.engine.ID;
import cfdp.engine.MIB;
import cfdp.engine.Response;

/**
 * MIB implementation to be able to set PDU size etc
 * 
 *
 */
public class CfdpMIB implements MIB {
    ID      my_id;
    int     ackLimit                  = 10;
    int     ackTimeout                = 15;
    int     inactivityTimeout         = 3600;
    int     nakLimit                  = 10;
    int     nakTimeout                = 15;

    // PDU preferred size
    int     outgoingFileChunkSize     = 250;
    int     maxConcurrentTransactions = 200;
    int     maxFileChunkLength        = 997;
    int     maxGapsPerNakPDU          = 50;
    int     maxFilenameLength         = 100;
    int     transSeqNumLength         = 4;
    boolean issueEofRecv              = true;
    boolean issueEofSent              = true;
    boolean issueFileSegmentRecv      = false;
    boolean issueFileSegmentSent      = false;
    boolean saveIncompleteFiles       = true;
    int     oneWayLightTime           = 0;
    boolean class1TimersRunning       = true;
    boolean class2TimersRunning       = true;
    boolean uplinkOn                  = true;
    boolean reportUnknownTransaction  = true;

    private boolean generateCrc       = false;

    /**
     * Constructor
     * 
     * @param id
     */
    public CfdpMIB(final ID id) {
        my_id = id;
    }

    @Override
    public int ackLimit(final ID node_id) {
        return (this.ackLimit);
    }

    @Override
    public int ackTimeout(final ID node_id) {
        return (this.ackTimeout + 2 * oneWayLightTime(node_id));
    }

    @Override
    public int inactivityTimeout(final ID node_id) {
        return (this.inactivityTimeout + 2 * oneWayLightTime(node_id));
    }

    @Override
    public boolean issueEofRecv() {
        return (this.issueEofRecv);
    }

    @Override
    public boolean issueEofSent() {
        return (this.issueEofSent);
    }

    @Override
    public boolean issueFileSegmentRecv() {
        return (this.issueFileSegmentRecv);
    }

    @Override
    public boolean issueFileSegmentSent() {
        return (this.issueFileSegmentSent);
    }

    @Override
    public int outgoingFileChunkSize(final ID node_id) {
        return (this.outgoingFileChunkSize);
    }

    @Override
    public boolean saveIncompleteFiles(final ID node_id) {
        return (this.saveIncompleteFiles);
    }

    @Override
    public int nakLimit(final ID node_id) {
        return (this.nakLimit);
    }

    @Override
    public int nakTimeout(final ID node_id) {
        return (this.nakTimeout + 2 * oneWayLightTime(node_id));
    }

    @Override
    public Response response(final ConditionCode condition_code) {
        return (RESPONSE_CANCEL);
    }

    @Override
    public int maxConcurrentTransactions() {
        return (this.maxConcurrentTransactions);
    }

    @Override
    public int genTransSeqNumLength() {
        return (this.transSeqNumLength);
    }

    @Override
    public int maxFileChunkLength() {
        return (this.maxFileChunkLength);
    }

    @Override
    public int maxGapsPerNakPDU() {
        return (this.maxGapsPerNakPDU);
    }

    @Override
    public int maxFilenameLength() {
        return (this.maxFilenameLength);
    }

    @Override
    public boolean uplinkOn(final ID node_id) {
        return (this.uplinkOn);
    }

    @Override
    public boolean class1TimersRunning() {
        return this.class1TimersRunning;
    }

    @Override
    public boolean class2TimersRunning() {
        return this.class1TimersRunning;
    }

    @Override
    public boolean reportUnknownTransaction() {
        return (this.reportUnknownTransaction);
    }

    /**
     * Set PDU preferred size
     * 
     * @param outgoingFileChunkSize Desired PDU size
     */
    public void setOutgoingFileChunkSize(final int outgoingFileChunkSize) {
        this.outgoingFileChunkSize = outgoingFileChunkSize;
    }

    /**
     * Set Transaction sequence length
     * 
     * @param transSeqNumLength Transaction sequence length
     */
    public void setTransSeqNumLength(final int transSeqNumLength) {
        this.transSeqNumLength = transSeqNumLength;
    }

    /**
     * Set Max file chunk length
     * 
     * @param maxFileChunkLength Max PDU length
     */
    public void setMaxFileChunkLength(final int maxFileChunkLength) {
        this.maxFileChunkLength = maxFileChunkLength;
    }

    /**
     * Enable CRCs on transmission.
     * 
     * @param nodeID
     *            the node being queried (null for default)
     * @return true if CRCs are required on transmission to the given entity
     */
    @Override
    public boolean applyCRC(final ID nodeID) {
        return generateCrc;
    }

    /**
     * Setter for the generateCrc flag
     * 
     * @param generateCrc
     *            True if CRCs are desired
     */
    public void setGenerateCrc(final boolean generateCrc) {
        this.generateCrc = generateCrc;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("ackTimeout: " + this.ackTimeout + "\n");
        sb.append("ackLimit: " + this.ackLimit + "\n");
        sb.append("nakTimeout: " + this.nakTimeout + "\n");
        sb.append("nakLimit: " + this.nakLimit + "\n");
        sb.append("inactivityTimeout: " + this.inactivityTimeout + "\n");
        sb.append("transSeqNumLength: " + this.transSeqNumLength + "\n");
        sb.append("outgoingFileChunkSize: " + this.outgoingFileChunkSize + "\n");
        sb.append("maxFileChunkLength: " + this.maxFileChunkLength + "\n");
        sb.append("maxFilenameLength: " + this.maxFilenameLength + "\n");
        sb.append("maxGapsPerNakPDU: " + this.maxGapsPerNakPDU + "\n");
        sb.append("saveIncompleteFiles? " + this.saveIncompleteFiles + "\n");
        sb.append("oneWayLightTime: " + this.oneWayLightTime + "\n");
        sb.append("class1TimersRunning? " + this.class1TimersRunning + "\n");
        sb.append("class2TimersRunning? " + this.class2TimersRunning + "\n");
        sb.append("uplinkOn? " + this.uplinkOn + "\n");
        sb.append("reportUnknownTransaction? " + this.reportUnknownTransaction + "\n");

        return (sb.toString());
    }

    private int oneWayLightTime(final ID node_id) {
        return (this.oneWayLightTime);
    }

}
