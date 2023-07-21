/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.processor.stat;

/**
 * {@code UplinkTransactionStatistics} keeps statistics for an uplink transaction.
 *
 */
class UplinkTransactionStatistics extends ATransactionStatistics {

    private long fileDataBytesUplinked;
    private long fileDataPduBytesSent;
    private boolean eofPduQueued;
    private int eofPduBytesSent;
    private boolean metadataPduQueued;
    private int metadataPduBytesSent;

    @Override
    public String toString() {
        return super.toString() + " fileDataBytesUplinked=" + getFileDataBytesUplinked()
                + " fileDataPduBytesSent=" + getFileDataPduBytesSent() + " eofPduQueued=" + isEofPduQueued()
                + " eofPduBytesSent=" + getEofPduBytesSent() + " metadataPduQueued=" + isMetadataPduQueued()
                + " metadataPduBytesSent=" + getMetadataPduBytesSent();
    }

    /**
     * @return
     */
    long getFileDataBytesUplinked() {
        return this.fileDataBytesUplinked;
    }

    /**
     * @param newFileDataBytesUplinked
     */
    void addFileDataBytesUplinked(final int newFileDataBytesUplinked) {
        this.fileDataBytesUplinked += newFileDataBytesUplinked;
    }

    /**
     * @return
     */
    long getFileDataPduBytesSent() {
        return this.fileDataPduBytesSent;
    }

    /**
     * @param newFileDataPduBytesSent
     */
    void addFileDataPduBytesSent(final int newFileDataPduBytesSent) {
        this.fileDataPduBytesSent += newFileDataPduBytesSent;
    }

    /**
     * @return
     */
    boolean isEofPduQueued() {
        return this.eofPduQueued;
    }

    /**
     * @param eofPduQueued
     */
    void setEofPduQueued(final boolean eofPduQueued) {
        this.eofPduQueued = eofPduQueued;
    }

    /**
     * @return
     */
    int getEofPduBytesSent() {
        return this.eofPduBytesSent;
    }

    /**
     * @param newEofPduBytesSent
     */
    void addEofPduBytesSent(final int newEofPduBytesSent) {
        this.eofPduBytesSent += newEofPduBytesSent;
    }

    /**
     * @return
     */
    boolean isMetadataPduQueued() {
        return this.metadataPduQueued;
    }

    /**
     * @param metadataPduQueued
     */
    void setMetadataPduQueued(final boolean metadataPduQueued) {
        this.metadataPduQueued = metadataPduQueued;
    }

    /**
     * @return
     */
    int getMetadataPduBytesSent() {
        return this.metadataPduBytesSent;
    }

    /**
     * @param newMetadataPduBytesSent
     */
    void addMetadataPduBytesSent(final int newMetadataPduBytesSent) {
        this.metadataPduBytesSent += newMetadataPduBytesSent;
    }

}