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
 * {@code DownlinkTransactionStatistics} keeps statistics for a downlink transaction.
 *
 */
class DownlinkTransactionStatistics extends ATransactionStatistics {

    private boolean fileSizeDetermined = false;
    private long fileDataBytesDownlinked;
    private boolean finishedPduQueued;
    private int finishedPduBytesSent;
    private boolean nakPduQueued;
    private int nakPduBytesSent;

    @Override
    public String toString() {
        return super.toString() + " fileSizeDetermined=" + isFileSizeDetermined() + " fileDataBytesDownlinked=" + getFileDataBytesDownlinked() + " finishedPduQueued="
                + isFinishedPduQueued() + " finishedPduBytesSent=" + getFinishedPduBytesSent() + " nakPduQueued="
                + isNakPduQueued() + " nakPduBytesSent=" + getNakPduBytesSent();
    }

    /**
     * @return
     */
    boolean isFileSizeDetermined() {
        return this.fileSizeDetermined;
    }

    /**
     * @param fileSizeDetermined
     */
    void setFileSizeDetermined(final boolean fileSizeDetermined) {
        this.fileSizeDetermined = fileSizeDetermined;
    }

    /**
     * @return
     */
    long getFileDataBytesDownlinked() {
        return this.fileDataBytesDownlinked;
    }

    /**
     * @param newFileDataBytesDownlinked
     */
    void addFileDataBytesDownlinked(final int newFileDataBytesDownlinked) {
        this.fileDataBytesDownlinked += newFileDataBytesDownlinked;
    }

    /**
     * @return
     */
    boolean isFinishedPduQueued() {
        return this.finishedPduQueued;
    }

    /**
     * @param finishedPduQueued
     */
    void setFinishedPduQueued(final boolean finishedPduQueued) {
        this.finishedPduQueued = finishedPduQueued;
    }

    /**
     * @return
     */
    int getFinishedPduBytesSent() {
        return this.finishedPduBytesSent;
    }

    /**
     * @param newFinishedPduBytesSent
     */
    public void addFinishedPduBytesSent(final int newFinishedPduBytesSent) {
        this.finishedPduBytesSent += newFinishedPduBytesSent;
    }

    /**
     * @return
     */
    boolean isNakPduQueued() {
        return this.nakPduQueued;
    }

    /**
     * @param nakPduQueued
     */
    void setNakPduQueued(final boolean nakPduQueued) {
        this.nakPduQueued = nakPduQueued;
    }

    /**
     * @return
     */
    int getNakPduBytesSent() {
        return this.nakPduBytesSent;
    }

    /**
     * @param newNakPduBytesSent
     */
    void addNakPduBytesSent(final int newNakPduBytesSent) {
        this.nakPduBytesSent += newNakPduBytesSent;
    }

}