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
package jpl.gds.tc.impl.frame;

import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.frame.ITcTransferFrameBuilder;
import org.eclipse.core.commands.Command;

import static jpl.gds.tc.api.ITcTransferFrame.*;

/**
 * The TcTransferFrameBuilder is a builder that allows something to prep all
 * of the values for creating a TcTransferFrame and ensures that all necessary
 * values have been set before creating it
 *
 */
public class TcTransferFrameBuilder implements ITcTransferFrameBuilder {

    private static int defaultVersion = 0;
    private static byte defaultSpare   = 0;

    private Integer sequenceNumber;
    private Integer version;
    private Integer bypassFlag;
    private Integer ctrlCmdFlag;
    private Byte    spare;
    private Integer spacecraftId;
    private Byte    executionString;
    private Byte    vcNumber;
    private Integer frameLength;
    private Integer orderId;
    private boolean hasFecf;
    private byte[]  fecf;
    private byte[]  data = new byte[0];

    private CommandFrameProperties frameConfig = null;

    public static void setDefaultVersion(final int version) {
        defaultVersion = version;
    }

    public static void setDefaultSpare(final byte spare) {
        defaultSpare = spare;
    }

    @Override
    public ITcTransferFrame build() {

        checkPreconditions();

        final ITcTransferFrame frame = new TcTransferFrame(frameConfig);
        frame.setVersionNumber(version.byteValue());
        frame.setBypassFlag(bypassFlag.byteValue());
        frame.setControlCommandFlag(ctrlCmdFlag.byteValue());
        frame.setData(data);
        if (hasFecf) {
            frame.setHasFecf(true);
            frame.setFecf(fecf);
        }
        frame.setSpacecraftId(spacecraftId);
        frame.setLength(frameLength);
        frame.setSpare(spare);
        frame.setSequenceNumber(sequenceNumber);
        if (orderId != null) {
            frame.setOrderId(orderId);
        }
        frame.setExecutionString(executionString);
        frame.setVirtualChannelNumber(vcNumber);

        return frame;
    }

    private void checkPreconditions() {
        if (sequenceNumber == null) {
            throw new IllegalStateException("The sequence number must be set.");
        }

        if (version == null) {
            setVersion(defaultVersion);
        }

        if (spare == null) {
            setSpare(defaultSpare);
        }

        if (bypassFlag == null) {
            throw new IllegalStateException("The bypass flag must be set.");
        }

        if (ctrlCmdFlag == null) {
            throw new IllegalStateException("The control command flag must be set.");
        }

        if (spacecraftId == null) {
            throw new IllegalStateException("The spacecraft ID must be set.");
        }

        if(executionString == null) {
            throw new IllegalStateException("The execution string must be set.");
        }

        if (vcNumber == null) {
            throw new IllegalStateException("The virtual channel number must be set.");
        }

        if (frameLength == null) {
            throw new IllegalStateException("The frame length must be set.");
        }

        if (hasFecf && (fecf == null)) {
            throw new IllegalStateException("FECF must be set.");
        }
    }

    @Override
    public ITcTransferFrameBuilder setFrameConfig(final CommandFrameProperties frameConfig) {
        this.frameConfig = frameConfig;

        return this;
    }

    @Override
    public ITcTransferFrameBuilder setSequenceNumber(final int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;

        return this;
    }

    @Override
    public ITcTransferFrameBuilder setVersion(final int version) {
        this.version = version;
        return this;
    }

    @Override
    public ITcTransferFrameBuilder setBypassFlag(final int bypassFlag) {
        this.bypassFlag = bypassFlag;
        return this;
    }

    @Override
    public ITcTransferFrameBuilder setCtrlCmdFlag(final int ctrlCmdFlag) {
        this.ctrlCmdFlag = ctrlCmdFlag;
        return this;
    }

    @Override
    public ITcTransferFrameBuilder setSpare(final byte spare) {
        this.spare = spare;
        return this;
    }

    @Override
    public ITcTransferFrameBuilder setSpacecraftId(final int spacecraftId) {
        this.spacecraftId = spacecraftId;
        return this;
    }

    @Override
    public ITcTransferFrameBuilder setVcid(final byte vcid) {
        this.setExecutionString((byte) ((vcid >> EXECUTION_STRING_BIT_OFFSET) & BITMASK_EXECUTION_STRING));
        this.setVirtualChannelNumber((byte)(vcid & BITMASK_VC_NUMBER));
        
        return this;
    }

    @Override
    public ITcTransferFrameBuilder setExecutionString(final byte executionString) {
        this.executionString = executionString;
        return this;
    }

    @Override
    public ITcTransferFrameBuilder setVirtualChannelNumber(final byte vcNumber) {
        this.vcNumber = vcNumber;
        return this;
    }

    @Override
    public ITcTransferFrameBuilder setFrameLength(final int frameLength) {
        this.frameLength = frameLength;
        return this;
    }

    @Override
    public ITcTransferFrameBuilder setOrderId(final int orderId) {
        this.orderId = orderId;
        return this;
    }

    @Override
    public ITcTransferFrameBuilder setHasFecf(final boolean hasFecf) {
        this.hasFecf = hasFecf;
        return this;
    }

    @Override
    public ITcTransferFrameBuilder setFecf(final byte[] fecf) {
        if (fecf == null) {
            throw new IllegalArgumentException("FECF must not be null.");
        }
        if (fecf.length > 0) {
            setHasFecf(true);
        }
        this.fecf = fecf;

        return this;
    }

    @Override
    public ITcTransferFrameBuilder setData(final byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null.");
        }
        this.data = data;

        return this;
    }
}
