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
package jpl.gds.tc.mps.impl.frame;


import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.exception.ArgumentParseException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.api.frame.FrameErrorControlFieldAlgorithm;
import jpl.gds.tc.api.frame.ITcTransferFramesBuilder;
import jpl.gds.tc.mps.impl.frame.parsers.MpsTcTransferFrameParser;
import jpl.gds.tc.mps.impl.session.MpsSession;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.uplinkutils.UplinkUtilsConstants.*;
import static jpl.gds.tc.api.frame.FrameErrorControlFieldAlgorithm.EACSUM55AA;

/**
 * {@code MpsTcTransferFramesBuilder} is a {@code ITcTransferFrame} builder that uses the MPSA UplinkUtils to build a
 * transfer frame. If the UplinkUtils library does not support setting a particular transfer frame field, an exception
 * is thrown.
 * <p>
 * Following fields must be set using their respective set methods, before calling {@code #build()}:
 * <ul>
 * <li>scid</li>
 * <li>data</li>
 * <li>vcid</li>
 * </ul>
 *
 * @since 8.2.0
 */
public class MpsTcTransferFramesBuilder implements ITcTransferFramesBuilder {

    private boolean transferFrameVersionNumberSet;
    private int     transferFrameVersionNumber;

    private boolean reservedSpareSet;
    private int     reservedSpare;

    private boolean bypassFlagSet;
    private boolean bypassFlag;

    private boolean controlCommandFlagSet;
    private boolean controlCommandFlag;

    private boolean scidSet;
    private int     scid;

    private boolean vcidSet;
    private int     vcid;

    private boolean frameSequenceNumberSet;
    private int     frameSequenceNumber;

    private boolean rollFrameSequenceNumberSet;

    private boolean dataSet;
    private byte[]  data;

    private boolean                         frameErrorControlFieldAlgorithmSet;
    private FrameErrorControlFieldAlgorithm frameErrorControlFieldAlgorithm;
    private boolean                         frameErrorControlFieldSetToNone;

    private boolean frameLengthSet;
    private int     frameLength;

    private boolean fecfValueSet;
    private byte[]  fecfValue;
    private int     fecLength;
    private boolean fecLengthSet;

    @Override
    public ITcTransferFramesBuilder setTransferFrameVersionNumber(final int transferFrameVersionNumber) {
        this.transferFrameVersionNumber = transferFrameVersionNumber;
        transferFrameVersionNumberSet = true;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setBypassFlagOn() {
        bypassFlag = true;
        bypassFlagSet = true;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setBypassFlagOff() {
        bypassFlag = false;
        bypassFlagSet = true;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setControlCommandFlagOn() {
        controlCommandFlag = true;
        controlCommandFlagSet = true;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setControlCommandFlagOff() {
        controlCommandFlag = false;
        controlCommandFlagSet = true;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setReservedSpare(final int reservedSpare) {
        this.reservedSpare = reservedSpare;
        reservedSpareSet = true;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setScid(final int scid) {

        if (scid < 0 || scid > 1023) {
            throw new IllegalArgumentException("scid must be between 0 to 1023 inclusive, but " + scid + " was " +
                    "supplied");
        }

        this.scid = scid;
        scidSet = true;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setVcid(final int vcid) {

        if (vcid < 0 || vcid > 63) {
            throw new IllegalArgumentException("vcid must be between 0 to 63 inclusive, but " + vcid + " was supplied");
        }

        this.vcid = vcid;
        vcidSet = true;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setFrameLength(final int frameLength) {
        this.frameLength = frameLength;
        this.frameLengthSet = true;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setFrameSequenceNumber(final int frameSequenceNumber) {

        if (frameSequenceNumber < 0 || frameSequenceNumber > 255) {
            throw new IllegalArgumentException(
                    "frameSequenceNumber must be between 0 to 255 inclusive, but " + frameSequenceNumber +
                            " was supplied");
        }

        this.frameSequenceNumber = frameSequenceNumber;
        frameSequenceNumberSet = true;
        rollFrameSequenceNumberSet = false;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder rollFrameSequenceNumber() {
        rollFrameSequenceNumberSet = true;
        frameSequenceNumberSet = false;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setData(final byte[] data) {

        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }

        this.data = data;
        dataSet = true;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setFrameErrorControlFieldAlgorithm(
            final FrameErrorControlFieldAlgorithm frameErrorControlFieldAlgorithm) {

        if (frameErrorControlFieldAlgorithm == null) {
            throw new IllegalArgumentException("frameErrorControlFieldAlgorithm cannot be null");
        }

        this.frameErrorControlFieldAlgorithm = frameErrorControlFieldAlgorithm;
        frameErrorControlFieldAlgorithmSet = true;
        frameErrorControlFieldSetToNone = false;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder noFrameErrorControlField() {
        frameErrorControlFieldSetToNone = true;
        frameErrorControlFieldAlgorithmSet = false;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setFrameErrorControlFieldValue(final byte[] frameErrorControlFieldValue) {
        this.fecfValue = frameErrorControlFieldValue;
        this.fecfValueSet = true;
        return this;
    }

    @Override
    public ITcTransferFramesBuilder setFrameErrorControlFieldLength(int length) {
        this.fecLength = length;
        this.fecLengthSet = true;
        return this;
    }

    @Override
    public List<ITcTransferFrame> build() throws FrameWrapUnwrapException {

        checkPreconditions();

        try (final MpsSession tcSession = new MpsSession(scid)) {

            final TcSession.TcwrapGroup tcwrapGroup = getTcwrapGroup(tcSession);

            setWrapGroupFrameHeaderFields(tcwrapGroup);

            // Wrap
            final String dataHexStr = BinOctHexUtility.toHexFromBytes(data);
            tcwrapGroup.HexstrFrmTcwrapToFrm(dataHexStr);

            final List<TcSession.bufitem> bufferItems = getBufferItemsFromWrapGroup(tcwrapGroup);

            return extractFramesFromBufferItems(tcSession, bufferItems);

        } catch (final Exception e) {
            throw new FrameWrapUnwrapException(e);
        }

    }

    private List<TcSession.bufitem> getBufferItemsFromWrapGroup(final TcSession.TcwrapGroup tcwrapGroup) {
        // Retrieve the wrapped frames
        // TODO getTcwrapBufferLinearList() isn't working as expected either - check with Tim

//            final List<TcSession.bufitem> bufferItems = tcwrapGroup.getTcwrapBufferLinearList();
//            if (bufferItems == null) {
//                throw new FrameWrapUnwrapException("MPSA UplinkUtils returned null transfer frame buffer items list");
//            }

        // TODO Until getTcwrapBufferLinearList() works, temporarily do below
        final List<TcSession.bufitem> bufferItems = new ArrayList<>(1);

        bufferItems.add(tcwrapGroup.getTcwrapBuffer());
        return bufferItems;
    }

    private List<ITcTransferFrame> extractFramesFromBufferItems(final MpsSession tcSession,
                                                                final List<TcSession.bufitem> bufferItems) throws
                                                                                                           FrameWrapUnwrapException,
                                                                                                           ArgumentParseException {
        final List<ITcTransferFrame> tcFrames = new ArrayList<>(bufferItems.size());

        if (fecLengthSet) {
            return modifyFecfFrames(tcSession, bufferItems);
        } else {

            // Count the ordinal in frame list
            int i = 1;

            final MpsTcTransferFrameParser tcParser = new MpsTcTransferFrameParser(scid);
            for (final TcSession.bufitem bufferItem : bufferItems) {

                if (bufferItem.nerrors > 0) {
                    throw new FrameWrapUnwrapException("MPSA UplinkUtils reported " + bufferItem.nerrors + " errors " +
                            "on frame " + i + ": " + bufferItem.errmsg);
                }

                final TcSession.frmitem frameItem = tcSession.getFrameItem(bufferItem);
                modifyFrameHeader(tcSession, frameItem);

                tcFrames.add(tcParser.parse(frameItem));

                i++;
            }

            return tcFrames;
        }
    }

    private List<ITcTransferFrame> modifyFecfFrames(MpsSession tcSession, List<TcSession.bufitem> bufferItems) throws
                                                                                                               FrameWrapUnwrapException {
        List<ITcTransferFrame> frames = new ArrayList<>(bufferItems.size());

        for (TcSession.bufitem bufferItem : bufferItems) {

            // Configure a wrap group
            TcSession.TcwrapGroup localGroup = getLocalTcwrapGroup(tcSession, getFecType());
            setWrapGroupFrameHeaderFields(localGroup);

            TcSession.frmitem originalFrameItem = tcSession.getFrameItem(bufferItem);
            if (originalFrameItem.frmfecdata == null && originalFrameItem.frmfeclen == 0) {
                MpsTcTransferFrameParser parser = new MpsTcTransferFrameParser(scid);
                ITcTransferFrame frame = parser.parse(originalFrameItem);
                frames.add(frame);
            } else {

                String dataHex = UplinkUtils.bintoasciihex(originalFrameItem.data, originalFrameItem.datalen << 3, 0);

                // real frame length should be (original length) - (default FECF length) + (new FECF length)
                int originalFrameLength  = originalFrameItem.frmlen;
                int correctedFrameLength = originalFrameLength - 2 + fecLength;

                // override the frame length to get a new frame
                localGroup.set_flen_override(correctedFrameLength);
                localGroup.HexstrFrmTcwrapToFrm(dataHex);

                TcSession.bufitem correctedFrameBuffer = localGroup.getTcwrapBuffer(0);
                String frameHex = UplinkUtils
                        .bintoasciihex(correctedFrameBuffer.buf, correctedFrameBuffer.nbits, 0);
                // strip off the last 2 bytes as the FEC
                String fecHex = frameHex.substring(frameHex.length() - 4);
                // pad the FEC string to fill bytes, override, and generate a new frame
                String paddedFecHex = StringUtils.leftPad(fecHex, fecLength * 2, '0');
                localGroup.set_fec_override(paddedFecHex);
                localGroup.HexstrFrmTcwrapToFrm(dataHex);

                TcSession.bufitem correctedFecFrameBuffer = localGroup.getTcwrapBuffer(1);
                TcSession.frmitem correctedFecFrameItem   = tcSession.getFrameItem(correctedFecFrameBuffer, false);

                // parse out frame, add  to list
                MpsTcTransferFrameParser parser = new MpsTcTransferFrameParser(scid);
                parser.setFecLength(fecLength);
                ITcTransferFrame frame = parser.parse(correctedFecFrameItem, false);
                frames.add(frame);
            }
        }

        return frames;
    }

    private void modifyFrameHeader(final MpsSession tcSession, final TcSession.frmitem frameItem) throws
                                                                                                  ArgumentParseException {
        if (fecfValueSet) {
            final TcSession.bufitem fecfBufferItem = tcSession
                    .hexStringToBufferItem(BinOctHexUtility.toHexFromBytes(
                            fecfValue));
            frameItem.frmfecdata = fecfBufferItem.buf;
            frameItem.frmfeclen = fecfBufferItem.nbits >> 3;
        }

        if (frameLengthSet) {
            frameItem.frmlen = frameLength;
        }
    }

    private void setWrapGroupFrameHeaderFields(final TcSession.TcwrapGroup tcwrapGroup) {
        if (transferFrameVersionNumberSet) {
            tcwrapGroup.set_ver(transferFrameVersionNumber);
        }

        if (reservedSpareSet) {
            tcwrapGroup.set_spare(reservedSpare);
        }

        if (bypassFlagSet) {
            if (bypassFlag) {
                tcwrapGroup.set_bp();
            } else {
                tcwrapGroup.reset_bp();
            }
        } // else use CTS default

        if (controlCommandFlagSet) {
            if (controlCommandFlag) {
                tcwrapGroup.set_cc();
            } else {
                tcwrapGroup.reset_cc();
            }
        } // else use CTS default

        // if not set, use the length that CTS comes up with
        if (frameLengthSet) {
            tcwrapGroup.set_flen_override(frameLength);
        }


    }

    private void checkPreconditions() throws FrameWrapUnwrapException {
        // Check for error conditions
        if (!scidSet) {
            throw new FrameWrapUnwrapException("scid must be set");
        }

        if (!dataSet) {
            throw new FrameWrapUnwrapException("data must be set");
        }

        if (!vcidSet) {
            throw new FrameWrapUnwrapException("vcid must be set");
        }
    }

    private TcSession.TcwrapGroup getTcwrapGroup(final MpsSession tcSession) throws FrameWrapUnwrapException {
        final TcSession.TcwrapGroup tcwrapGroup;

        if (isAutoconfigureWrapGroup()) {
            tcwrapGroup = tcSession.createGlobalWrapGroup(vcid, -1);
        } else {
            final int fecType = getFecType();

            if (!frameSequenceNumberSet && !rollFrameSequenceNumberSet) {
                tcwrapGroup = tcSession.createGlobalWrapGroup(vcid, fecType);
            } else if (frameSequenceNumberSet || rollFrameSequenceNumberSet) {
                final int fsnType = frameSequenceNumberSet ? frameSequenceNumber : -1;
                tcwrapGroup = tcSession.createGlobalWrapGroup(vcid, fecType, fsnType);
            } else {
                throw new FrameWrapUnwrapException("Unexpected state encountered while creating TcwrapGroup");
            }
        }
        return tcwrapGroup;
    }

    private TcSession.TcwrapGroup getLocalTcwrapGroup(final MpsSession tcSession, int fecType) throws
                                                                                               FrameWrapUnwrapException {
        final TcSession.TcwrapGroup tcwrapGroup;

        if (isAutoconfigureWrapGroup()) {
            tcwrapGroup = tcSession.createLocalWrapGroup(vcid, -1);
        } else {
            if (!frameSequenceNumberSet && !rollFrameSequenceNumberSet) {
                tcwrapGroup = tcSession.createLocalWrapGroup(vcid, fecType);
            } else if (frameSequenceNumberSet || rollFrameSequenceNumberSet) {
                final int fsnType = frameSequenceNumberSet ? frameSequenceNumber : -1;
                tcwrapGroup = tcSession.createLocalWrapGroup(vcid, fecType, fsnType);
            } else {
                throw new FrameWrapUnwrapException("Unexpected state encountered while creating TcwrapGroup");
            }
        }
        return tcwrapGroup;
    }

    private int getFecType() {
        int fecType = -1;
        if (frameErrorControlFieldSetToNone) {
            fecType = TC_FEC_NONE;
        } else if (frameErrorControlFieldAlgorithmSet) {
            fecType = frameErrorControlFieldAlgorithm == EACSUM55AA ? TC_FEC_EACSUM55AA : TC_FEC_SDLC;
        }
        return fecType;
    }

    private boolean isAutoconfigureWrapGroup() {
        return !frameErrorControlFieldSetToNone && !frameErrorControlFieldAlgorithmSet && !frameSequenceNumberSet && !rollFrameSequenceNumberSet;
    }

}