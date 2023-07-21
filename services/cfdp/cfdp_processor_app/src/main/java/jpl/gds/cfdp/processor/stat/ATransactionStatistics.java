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

import java.io.Serializable;

/**
 * {@code ATransactionStatistics} is an abstract statistics for data keeping common stats for both downlink and
 * uplink transactions.
 *
 */
abstract class ATransactionStatistics implements Serializable {

    private long remoteEntity;
    private boolean timerPaused;
    private long fileSize;
    private boolean ackPduQueued;
    private int ackPduBytesSent;

    @Override
    public String toString() {
        return "remoteEntity=" + getRemoteEntity() + " timerPaused=" + isTimerPaused()
                + " fileSize=" + getFileSize() + " ackPduQueued=" + isAckPduQueued()
                + " ackPduBytesSent=" + getAckPduBytesSent();
    }

    /**
     * @return the remoteEntity
     */
    long getRemoteEntity() {
        return remoteEntity;
    }

    /**
     * @param remoteEntity the remoteEntity to set
     */
    void setRemoteEntity(final long remoteEntity) {
        this.remoteEntity = remoteEntity;
    }

    /**
     * @return the timerPaused
     */
    boolean isTimerPaused() {
        return timerPaused;
    }

    /**
     * @param timerPaused the timerPaused to set
     */
    void setTimerPaused(final boolean timerPaused) {
        this.timerPaused = timerPaused;
    }

    /**
     *
     * @return ackPduQueued
     */
    boolean isAckPduQueued() {
        return this.ackPduQueued;
    }

    /**
     * @param ackPduQueued
     */
    void setAckPduQueued(final boolean ackPduQueued) {
        this.ackPduQueued = ackPduQueued;
    }

    /**
     * @return ackPduBytesSent
     */
    int getAckPduBytesSent() {
        return this.ackPduBytesSent;
    }

    /**
     * @param newAckPduBytesSent
     */
    void addAckPduBytesSent(final int newAckPduBytesSent) {
        this.ackPduBytesSent += newAckPduBytesSent;
    }

    /**
     * @return
     */
    long getFileSize() {
        return this.fileSize;
    }

    /**
     * @param fileSize
     */
    void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }

}