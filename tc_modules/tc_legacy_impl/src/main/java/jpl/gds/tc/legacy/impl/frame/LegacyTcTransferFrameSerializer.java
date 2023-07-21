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
package jpl.gds.tc.legacy.impl.frame;

import jpl.gds.shared.checksum.CcsdsCrc16ChecksumAdaptor;
import jpl.gds.shared.checksum.EndAroundCarrySumAlgorithm;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.frame.FrameErrorControlFieldAlgorithm;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;
import jpl.gds.tc.impl.frame.AbstractTcTransferFrameSerializer;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

/**
 * The old (AMPCS) TcTransferFrame serialization process
 *
 */
public class LegacyTcTransferFrameSerializer extends AbstractTcTransferFrameSerializer {

    private final CommandFrameProperties frameConfig;

    public LegacyTcTransferFrameSerializer(final ApplicationContext appContext) {
        this(appContext.getBean(CommandFrameProperties.class));
    }

    public LegacyTcTransferFrameSerializer(final CommandFrameProperties frameConfig) {
        this.frameConfig = frameConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getBytes(final ITcTransferFrame tcFrame) {
        if(tcFrame.getData() == null) {
            throw new IllegalStateException("Can't get frame bytes.  The frame data is null.");
        }

        //allocate a byte array to hold the entire frame
        final byte[] bytes = new byte[calculateLength(tcFrame) + 1];

        final byte[] headerBytes = getHeaderBytes(tcFrame);
        final byte[] dataBytes   = tcFrame.getData();
        byte[] fecf = new byte[0];

        if(tcFrame.hasFecf()) {
            try {
                fecf = tcFrame.getFecf();
            } catch(IllegalStateException e) {
                fecf = calculateFecf(tcFrame);
                tcFrame.setFecf(fecf);
            }
        }

        //build the frame from the 3 pieces: header + data + fecf

        int offset = 0;
        System.arraycopy(headerBytes, 0, bytes, offset, headerBytes.length);
        offset += headerBytes.length;
        System.arraycopy(dataBytes, 0, bytes, offset, dataBytes.length);
        offset += dataBytes.length;
        System.arraycopy(fecf, 0, bytes, offset, fecf.length);

        return(bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short calculateLength(final ITcTransferFrame tcFrame) {
        if(tcFrame.getData() == null) {
            throw new IllegalStateException("Can't calculate frame length.  The frame data is null.");
        }

        short baseLength = (short)(ITcTransferFrameSerializer.TOTAL_HEADER_BYTE_LENGTH + tcFrame.getData().length - 1);
        if(tcFrame.hasFecf()) {
            try {
                baseLength += tcFrame.getFecf().length;
            } catch (IllegalStateException e) {
                //the above will throw if the FECF has not been set
                baseLength += frameConfig.getFecfLength();
            }
        }

        return baseLength;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] calculateFecf(final ITcTransferFrame tcFrame) {
        return calculateFecf(tcFrame, frameConfig.getChecksumCalcluator(tcFrame.getVirtualChannelNumber()));
    }

    public byte[] calculateFecf(final ITcTransferFrame tcFrame, final FrameErrorControlFieldAlgorithm algorithm) {
        if(tcFrame == null) {
            throw new IllegalStateException("Can't calculate FECF. The supplied frame is null.");
        }
        else if(tcFrame.getData() == null)
        {
            throw new IllegalStateException("Can't calculate FECF.  The frame data is null.");
        }

        byte[] data = ArrayUtils.addAll(getHeaderBytes(tcFrame), tcFrame.getData());

        byte[] calculatedFecf;

        switch(algorithm) {
            case CRC16CCITT:
                CcsdsCrc16ChecksumAdaptor algo = new CcsdsCrc16ChecksumAdaptor();
                long checksum;

                checksum = algo.calculateChecksum(data, 0, data.length);

                calculatedFecf = new byte[2];

                calculatedFecf[0] = (byte)((checksum & 0xFF00) >> 8);
                calculatedFecf[1] = (byte)(checksum & 0xFF);

                break;
            case EACSUM55AA:
            default:
                calculatedFecf = EndAroundCarrySumAlgorithm.doEncode(data);
        }

        return correctFecfLength(calculatedFecf);
    }

    //a calculated FECF may not have a length that's appropriate for the current frame configuration.
    private byte[] correctFecfLength(byte[] calculatedFecf) {
        int fecfLen = frameConfig.getFecfLength();
        byte[] adjustedFecf = calculatedFecf;

        if (calculatedFecf.length > fecfLen) { // take the rightmost bytes
            adjustedFecf = Arrays.copyOfRange(calculatedFecf, calculatedFecf.length - fecfLen, fecfLen);
        }
        else if(calculatedFecf.length < fecfLen) { //pad it out on the left
            adjustedFecf = ArrayUtils.addAll(new byte[fecfLen -calculatedFecf.length], calculatedFecf);
        }

        return adjustedFecf;
    }
}