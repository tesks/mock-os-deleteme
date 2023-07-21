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

package jpl.gds.tc.mps.impl.frame.parsers;

import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.SWIGTYPE_p_unsigned_char;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.exception.ArgumentParseException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.mps.impl.session.MpsSession;

import java.util.Arrays;
import org.apache.commons.lang.StringUtils;

import static jpl.gds.shared.string.StringUtil.cleanUpString;
import static jpl.gds.shared.util.BinOctHexUtility.toBytesFromHex;

/**
 * MPS Telecommand frame parser utilizing the MPS/CTS utilities
 *
 * @since R8.2
 */
public class MpsTcTransferFrameParser implements IMpsTcTransferFrameParser {

    private static final int DEFAULT_FEC_LENGTH = 2;

    private final int    scid;
    private final Tracer tracer;

    private int fecLength = DEFAULT_FEC_LENGTH;

    /**
     * Constructor
     *
     * @param scid spacecraft ID
     */
    public MpsTcTransferFrameParser(final int scid) {
        this(scid, TraceManager.getTracer(Loggers.UPLINK));
    }

    /**
     * Set the FEC length of the frame being parsed. Only used when FEC validation is turned off, to help parse the FEC
     * out of the data.
     *
     * @param fecLength
     */
    public void setFecLength(final int fecLength) {
        this.fecLength = fecLength;
    }

    /**
     * Constructor
     *
     * @param scid   spacecraft ID
     * @param tracer logging tracer
     */
    public MpsTcTransferFrameParser(final int scid, final Tracer tracer) {
        this.scid = scid;
        this.tracer = tracer;
    }

    @Override
    public ITcTransferFrame parse(final TcSession.frmitem frameItem) {
        return parse(frameItem, true);
    }

    /**
     * {@inheritDoc}
     *
     * When skipping FEC validation, the FEC (if present) is tacked on to the end of the data through CTS. Based on the
     * configured FEC length, we examine the last bytes of the data and compare to zeros to see if the FEC is present.
     * If it is present, the FEC is stripped from the data and added to the frame. The FEC length is configurable.
     */
    @Override
    public ITcTransferFrame parse(final TcSession.frmitem frameItem, final boolean validateFecf) {
        final MpsTcTransferFrameBuilder builder = new MpsTcTransferFrameBuilder();
        builder.setVersion(frameItem.frmver)
                .setBypassFlag(frameItem.frmbypass)
                .setCtrlCmdFlag(frameItem.frmcntlcmd)
                .setSpacecraftId(frameItem.frmscid)
                .setFrameLength(frameItem.frmlen)
                .setSequenceNumber(frameItem.frmseqnum)
                .setSpare((byte) frameItem.frmspare)
                .setVcid((byte) frameItem.frmvc);

        final byte[] data = getBytesFromCtsByteBuffer(frameItem.data, frameItem.datalen);
        builder.setData(data);

        if (!validateFecf && data.length - fecLength > 0) {
            // when FECF validation is turned off, FECF may be present at the end of the frame bytes.
            final byte[] potentialFecfBytes = Arrays.copyOfRange(data, data.length - fecLength, data.length);
            if (fecfIsNotZeros(potentialFecfBytes)) {
                builder.setFecf(potentialFecfBytes);
                builder.setData(Arrays.copyOfRange(data, 0, data.length - fecLength));
            }
        } else if (frameItem.frmfecdata != null && frameItem.frmfeclen > 0) {
            builder.setFecf(getBytesFromCtsByteBuffer(frameItem.frmfecdata, frameItem.frmfeclen));
        }

        return builder.build();
    }

    private boolean fecfIsNotZeros(final byte[] potentialFecfBytes) {
        return !BinOctHexUtility.toHexFromBytes(potentialFecfBytes).equals(StringUtils.repeat("00", fecLength));
    }

    private byte[] getBytesFromCtsByteBuffer(final SWIGTYPE_p_unsigned_char buffer, final int byteLength) {
        final String hex = UplinkUtils.bintoasciihex(buffer, byteLength << 3, 0);
        return toBytesFromHex(cleanUpString(hex));
    }

    @Override
    public ITcTransferFrame parse(final byte[] tcBytes) throws FrameWrapUnwrapException {
        return parse(tcBytes, true);
    }

    @Override
    public ITcTransferFrame parse(final byte[] tcBytes, final boolean validateFecf) throws FrameWrapUnwrapException {
        try {
            tracer.debug("Attempting to parse a frame from bytes through MPS/CTS.");
            final TcSession.frmitem frameItem = getFrameItem(tcBytes, validateFecf);
            tracer.debug("Successfully parsed a frame from bytes through MPS/CTS.");

            return parse(frameItem, validateFecf);
        } catch (final ArgumentParseException e) {
            throw new FrameWrapUnwrapException(e);
        }
    }

    @Override
    public ITcTransferFrame parse(final byte[] tcBytes, final int offset) throws FrameWrapUnwrapException {
        return parse(tcBytes, offset, true);
    }

    @Override
    public ITcTransferFrame parse(final byte[] tcBytes, final int offset, final boolean validateFecf) throws
                                                                                                      FrameWrapUnwrapException {
        return parse(Arrays.copyOfRange(tcBytes, offset, tcBytes.length), validateFecf);
    }

    private TcSession.frmitem getFrameItem(final byte[] tcBytes, final boolean validateFecf) throws
                                                                                             FrameWrapUnwrapException,
                                                                                             ArgumentParseException {
        try (final MpsSession session = new MpsSession(scid)) {
            final TcSession.bufitem frameBi   = session.hexStringToBufferItem(BinOctHexUtility.toHexFromBytes(tcBytes));
            final TcSession.frmitem frameItem = session.getFrameItem(frameBi, validateFecf);
            if (frameItem.nerrors > 0) {
                throw new FrameWrapUnwrapException(frameItem.errmsg);
            }
            return frameItem;
        }
    }

}
