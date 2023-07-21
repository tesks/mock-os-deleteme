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

package jpl.gds.tc.impl.scmf;

import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.message.IScmfCommandMessage;

/**
 * SCMF Command message holder for MPS/CTS
 *
 */
public class ScmfCommandMessage implements IScmfCommandMessage {
    private String closeWindow;
    private String messageComment;
    private long   messageNumber;
    private String openWindow;
    private String transmissionStartTime;
    private int    messageChecksum;
    private byte[] data;
    private ICltu  cltu;

    @Override
    public int getMessageByteLength() {
        throw new UnsupportedOperationException("Message byte length is not supported for SCMF command messages.");
    }

    @Override
    public byte[] getBytes() {
        throw new UnsupportedOperationException("Get bytes is not supported for SCMF command messages.");
    }

    /**
     * Set the SCM bytes
     *
     * @param bytes spacecraft message bytes
     */
    public void setBytes(final byte[] bytes) {
        throw new UnsupportedOperationException("Set bytes is not supported for SCMF command messages.");
    }

    @Override
    public String getCloseWindow() {
        return this.closeWindow;
    }

    @Override
    public void setCloseWindow(final String closeWindow) {
        this.closeWindow = closeWindow;
    }

    @Override
    public String getMessageComment() {
        return this.messageComment;
    }

    @Override
    public void setMessageComment(final String messageComment) {
        this.messageComment = messageComment;
    }

    @Override
    public long getMessageNumber() {
        return this.messageNumber;
    }

    @Override
    public void setMessageNumber(final long messageNumber) {
        this.messageNumber = messageNumber;
    }

    @Override
    public String getOpenWindow() {
        return this.openWindow;
    }

    @Override
    public void setOpenWindow(final String openWindow) {
        this.openWindow = openWindow;
    }

    @Override
    public String getTransmissionStartTime() {
        return this.transmissionStartTime;
    }

    @Override
    public void setTransmissionStartTime(final String transmissionStartTime) {
        this.transmissionStartTime = transmissionStartTime;
    }

    @Override
    public int getMessageChecksum() {
        return this.messageChecksum;
    }

    @Override
    public void setMessageChecksum(final int messageChecksum) {
        this.messageChecksum = messageChecksum;
    }

    @Override
    public byte[] getData() {
        return this.data;
    }

    @Override
    public void setData(final byte[] data) {
        this.data = data;
    }

    @Override
    public ICltu getCltuFromData() {
        return this.cltu;
    }

    @Override
    public void setCltuFromData(final ICltu cltu) {
        this.cltu = cltu;
    }

    @Override
    public byte[] getPlopBytes() {
        return cltu.getPlopBytes();
    }
}
