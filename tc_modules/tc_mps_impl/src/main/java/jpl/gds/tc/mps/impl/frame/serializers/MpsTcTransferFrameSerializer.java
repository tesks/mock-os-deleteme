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
package jpl.gds.tc.mps.impl.frame.serializers;

import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import gov.nasa.jpl.uplinkutils.UplinkUtilsConstants;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.api.frame.FrameErrorControlFieldAlgorithm;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;
import jpl.gds.tc.impl.frame.AbstractTcTransferFrameSerializer;
import jpl.gds.tc.mps.impl.frame.MpsTcTransferFramesBuilder;
import jpl.gds.tc.mps.impl.session.MpsSession;

import java.util.List;

/**
 * The "Gold standard" TcTransferFrame serlialization process
 *
 */
public class MpsTcTransferFrameSerializer extends AbstractTcTransferFrameSerializer {

    @Override
    public byte[] calculateFecf(final ITcTransferFrame tcFrame) {
        throw new UnsupportedOperationException(
                "MPS library cannot calculate FECF without the appropriate algorithm. Please supply the algortihm to get a new FECF.");
    }

    // MPCS-11285  - 09/24/19 - reverted back to old logic. Unofortunately the TcTransferFramesBuilder always throws
    //      if the frame length was set to an invalid length (one that doesn't match the expected)
    @Override
    public byte[] calculateFecf(ITcTransferFrame tcFrame, FrameErrorControlFieldAlgorithm algorithm) {

        int fecType;

        if(algorithm.equals(FrameErrorControlFieldAlgorithm.EACSUM55AA)) {
            fecType = UplinkUtilsConstants.TC_FEC_EACSUM55AA;
        } else { //algoVal.equals(FrameErrorControlFieldAlgorithm.CRC16CCITT
            fecType = UplinkUtilsConstants.TC_FEC_SDLC;
        }

        try (final MpsSession session = new MpsSession(tcFrame.getSpacecraftId())) {
            TcSession.TcwrapGroup wrapGroup = session.createGlobalWrapGroup(tcFrame.getVirtualChannelId(), fecType, tcFrame.getSequenceNumber());

            wrapGroup.set_ver(tcFrame.getVersionNumber());
            wrapGroup.set_spare(tcFrame.getSpare());

            if(tcFrame.getControlCommandFlag() != 0) {
                wrapGroup.set_cc();
            } else {
                wrapGroup.reset_cc();
            }
            if(tcFrame.getBypassFlag() != 0) {
                wrapGroup.set_bp();
            } else {
                wrapGroup.reset_bp();
            }

            wrapGroup.set_flen_override(tcFrame.getLength());

            String data = BinOctHexUtility.toHexFromBytes(tcFrame.getData());

            wrapGroup.HexstrFrmTcwrapToFrm(data);

            TcSession.bufitem frameBuffer = wrapGroup.getTcwrapBuffer(0);

            String frameHexStr = UplinkUtils.bintoasciihex(frameBuffer.buf, frameBuffer.nbits, 0);

            int fecfOffset = (ITcTransferFrameSerializer.TOTAL_HEADER_BYTE_LENGTH * 2) + data.length();

            String fecfHex = frameHexStr.substring(fecfOffset);

            return BinOctHexUtility.toBytesFromHex(fecfHex);

//        try {
//            MpsTcTransferFramesBuilder builder = new MpsTcTransferFramesBuilder();
//            if (tcFrame.getBypassFlag() > 0) {
//                builder.setBypassFlagOn();
//            } else {
//                builder.setBypassFlagOff();
//            }
//            if (tcFrame.getControlCommandFlag() > 0) {
//                builder.setControlCommandFlagOn();
//            } else {
//                builder.setControlCommandFlagOff();
//            }
//
//            builder.setTransferFrameVersionNumber(tcFrame.getVersionNumber())
//                    .setReservedSpare(tcFrame.getSpare())
//                    .setScid(tcFrame.getSpacecraftId())
//                    .setVcid(tcFrame.getVirtualChannelId())
//                    .setFrameLength(tcFrame.getLength())
//                    .setFrameSequenceNumber(tcFrame.getSequenceNumber())
//                    .setData(tcFrame.getData())
//                    .setFrameErrorControlFieldAlgorithm(algorithm);
//
//            List<ITcTransferFrame> frames = builder.build();
//
//            return frames.get(0).getFecf();
        }catch (Exception e) {
            TraceManager.getTracer(Loggers.UPLINK).info("Encountered an exception while attempting to calculate new FECF for frame using CTS: " + ExceptionTools.getMessage(e));
        }

        return new byte[0];
    }
}
