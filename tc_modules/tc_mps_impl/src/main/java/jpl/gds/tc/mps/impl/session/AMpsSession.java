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

package jpl.gds.tc.mps.impl.session;

import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import gov.nasa.jpl.uplinkutils.scmf_dataRec;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.exception.ArgumentParseException;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.mps.impl.frame.serializers.MpsTcTransferFrameSerializer;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * An MPS session is required to perform various telecommand wrapping and unwrapping functions. This is a delegate to
 * the CTS TcSession object, which is a manual object that requires manual deletion. This object implements the
 * Closeable interface, use it either in a try-with-resources block, or call close on it manually.
 *
 * @since R8.2
 */
public abstract class AMpsSession implements Closeable {

    protected final TcSession session;

    AMpsSession(final TcSession session) {
        this.session = session;
    }

    @Override
    public void close() {
        this.session.delete();
    }

    /**
     * @return the mission ID
     */
    public int getMissionId() {
        return session.get_missionid();
    }

    /**
     * @return the spacecraft ID
     */
    public int getScid() {
        return session.get_scid();
    }

    /**
     * Get a linear list of CLTU buffers. Use this to retrieve CLTUs once you have wrapped frames to CLTUs in the global
     * wrapping group.
     *
     * @return telecommand buffer list
     */
    public List<TcSession.bufitem> getLinearCltuBufferList() {
        return session.getTcwrapBufferLinearList();
    }

    /**
     * Get a linear list of translated command buffers.
     *
     * @return translated command buffer list
     */
    public List<TcSession.bufitem> getCommandBufferList() {
        final List<TcSession.bufitem> buffers = new ArrayList<>();
        TcSession.bufitem             buffer;
        int                           i       = 0;
        while ((buffer = session.getCmdBuffer(i++)) != null) {
            buffers.add(buffer);
        }

        return buffers;
    }

    /**
     * Convert a hex string to a buffer item
     *
     * @param hexString
     * @return TcSession buffer item
     * @throws ArgumentParseException
     */
    public TcSession.bufitem hexStringToBufferItem(final String hexString) throws ArgumentParseException {
        if (!BinOctHexUtility.isValidHex(hexString)) {
            throw new IllegalArgumentException("Input hex string is invalid.");
        }

        final TcSession.bufitem bufferItem = session.hexstring_to_bufitem(hexString);
        if (bufferItem.nerrors > 0) {
            throw new ArgumentParseException(bufferItem.errmsg);
        }
        return bufferItem;
    }

    /**
     * Create a global wrapping group with the supplied virtual channel ID
     *
     * @param vcid
     * @return global wrapping group
     */
    public TcSession.TcwrapGroup createGlobalWrapGroup(final int vcid) {
        return session.create_tcwrap_group_global(vcid);
    }

    /**
     * Create a global wrapping group with the supplied virtual channel ID and frame error control type
     *
     * @param vcid
     * @param fecType
     * @return global wrapping group
     */
    public TcSession.TcwrapGroup createGlobalWrapGroup(final int vcid, final int fecType) {
        return session.create_tcwrap_group_global(vcid, fecType);
    }

    /**
     * Create a global wrapping group with the supplied virtual channel ID, frame error control type, and frame sequence
     * number.
     *
     * @param vcid
     * @param fecType
     * @param frameSequenceNumber
     * @return global wrapping group
     */
    public TcSession.TcwrapGroup createGlobalWrapGroup(final int vcid, final int fecType,
                                                       final int frameSequenceNumber) {
        return session.create_tcwrap_group_global(vcid, fecType, frameSequenceNumber);
    }

    /**
     * Create a local wrapping group with the supplied virtual channel ID
     *
     * @param vcid
     * @return local wrapping group
     */
    public TcSession.TcwrapGroup createLocalWrapGroup(final int vcid) {
        return session.create_tcwrap_group(vcid);
    }

    /**
     * Create a local wrapping group with the supplied virtual channel ID and frame error control type.
     *
     * @param vcid
     * @param fecType
     * @return local wrapping group
     */
    public TcSession.TcwrapGroup createLocalWrapGroup(final int vcid, final int fecType) {
        return session.create_tcwrap_group(vcid, fecType);
    }

    /**
     * Create a local wrapping group with the supplied virtual channel ID, frame error control type, and frame sequence
     * number.
     *
     * @param vcid
     * @param fecType
     * @param frameSequenceNumber
     * @return local wrapping group
     */
    public TcSession.TcwrapGroup createLocalWrapGroup(final int vcid, final int fecType,
                                                      final int frameSequenceNumber) {
        return session.create_tcwrap_group(vcid, fecType, frameSequenceNumber);
    }

    /**
     * Get a CLTU item from a CLTU
     *
     * @param cltu
     * @return CLTU item
     * @throws CltuEndecException
     */
    public TcSession.cltuitem getCltuItem(final ICltu cltu) throws CltuEndecException {
        return getCltuItem(cltu, true);
    }

    /**
     * Get a CLTU item from a CLTU
     *
     * @param cltu
     * @param validateCodewords true to validate BCH codewords, false to skip validation
     * @return CLTU item
     * @throws CltuEndecException
     */
    public TcSession.cltuitem getCltuItem(final ICltu cltu, final boolean validateCodewords) throws CltuEndecException {
        return getCltuItem(cltu.getBytes(), validateCodewords);
    }

    /**
     * Get a CLTU item from CLTU bytes
     *
     * @param cltuBytes
     * @return CLTU item
     * @throws CltuEndecException
     */
    public TcSession.cltuitem getCltuItem(final byte[] cltuBytes) throws CltuEndecException {
        return getCltuItem(cltuBytes, true);
    }

    /**
     * Get a CLTU item from CLTU bytes
     *
     * @param cltuBytes
     * @param validateCodewords true to validate BCH codewords, false to skip validation
     * @return
     * @throws CltuEndecException
     */
    public TcSession.cltuitem getCltuItem(final byte[] cltuBytes, final boolean validateCodewords) throws
            CltuEndecException {
        final TcSession.bufitem bufferItem = session.hexstring_to_bufitem(BinOctHexUtility.toHexFromBytes(cltuBytes));
        final TcSession.cltuitem cltuItem = session
                .decodeCltu(bufferItem.buf, bufferItem.nbits >> 3, validateCodewords);
        checkCltuItemErrors(cltuItem);
        return cltuItem;
    }

    private void checkCltuItemErrors(final TcSession.cltuitem cltuItem) throws CltuEndecException {
        if (cltuItem.nerrors > 0 || cltuItem.errmsg != null) {
            throw new CltuEndecException(cltuItem.errmsg);
        }
    }

    /**
     * Get a CLTU item from an SCMF data record
     *
     * @param dataRecord
     * @return CLTU item
     * @throws CltuEndecException
     */
    public TcSession.cltuitem getCltuItem(final scmf_dataRec dataRecord) throws CltuEndecException {
        return getCltuItem(dataRecord, true);
    }

    /**
     * Get a CLTU item from an SCMF data record
     *
     * @param dataRecord
     * @param validateCodewords true to validate BCH codewords, false to skip validation
     * @return CLTU item
     * @throws CltuEndecException
     */
    public TcSession.cltuitem getCltuItem(final scmf_dataRec dataRecord, final boolean validateCodewords) throws
            CltuEndecException {
        final TcSession.cltuitem cltuItem = session
                .decodeCltu(dataRecord.getDrData(), ((int) dataRecord.getDrNumBits()) >> 3, validateCodewords);
        checkCltuItemErrors(cltuItem);
        return cltuItem;
    }

    /**
     * Get a CLTU item from a TcSession buffer item
     *
     * @param cltuBufferItem
     * @return CLTU item
     * @throws CltuEndecException
     */
    public TcSession.cltuitem getCltuItem(final TcSession.bufitem cltuBufferItem) throws CltuEndecException {
        return getCltuItem(cltuBufferItem, true);
    }

    /**
     * Get a CLTU item from a TcSession buffer item
     *
     * @param cltuBufferItem
     * @param validateCodewords true to validate BCH codewords, false to skip validation
     * @return CLTU item
     * @throws CltuEndecException
     */
    public TcSession.cltuitem getCltuItem(final TcSession.bufitem cltuBufferItem,
                                          final boolean validateCodewords) throws
            CltuEndecException {
        final TcSession.cltuitem cltuItem = session
                .decodeCltu(cltuBufferItem.buf, cltuBufferItem.nbits >> 3, validateCodewords);
        checkCltuItemErrors(cltuItem);
        return cltuItem;
    }

    /**
     * Get a frame item from a CLTU item
     *
     * @param cltuItem
     * @return a frame item
     * @throws FrameWrapUnwrapException
     */
    public TcSession.frmitem getFrameItem(final TcSession.cltuitem cltuItem) throws FrameWrapUnwrapException {
        return getFrameItem(cltuItem, true);
    }

    /**
     * Get a frame item from a CLTU item
     *
     * @param cltuItem
     * @param validateFec true to validate frame FEC, false to skip validation
     * @return a frame item
     * @throws FrameWrapUnwrapException
     */
    public TcSession.frmitem getFrameItem(final TcSession.cltuitem cltuItem, final boolean validateFec) throws
            FrameWrapUnwrapException {
        try {
            if (!validateFec) {
                turnOffFrameValidation();
            }

            final TcSession.frmitem frameItem = session.unwrapFrame(cltuItem.data, cltuItem.datalen);
            checkFrameItemErrors(frameItem);

            return frameItem;

        } finally {

            if (!validateFec) {
                turnOnFrameValidation();
            }
        }
    }

    private void turnOffFrameValidation() {
        session.set_fec(UplinkUtils.TC_FEC_IGNORE);
    }

    private void turnOnFrameValidation() {
        session.restore_fec();
    }

    /**
     * Get a frame item from frame bytes
     *
     * @param frameBytes
     * @return a frame item
     * @throws FrameWrapUnwrapException
     */
    public TcSession.frmitem getFrameItem(final byte[] frameBytes) throws FrameWrapUnwrapException {
        return getFrameItem(frameBytes, true);
    }

    /**
     * Get a frame item from frame bytes
     *
     * @param frameBytes
     * @param validateFec true to validate frame FEC, false to skip validation
     * @return a frame item
     * @throws FrameWrapUnwrapException
     */
    public TcSession.frmitem getFrameItem(final byte[] frameBytes, final boolean validateFec) throws
            FrameWrapUnwrapException {
        try {
            if (!validateFec) {
                turnOffFrameValidation();
            }

            final TcSession.bufitem frameBufferItem = session
                    .hexstring_to_bufitem(BinOctHexUtility.toHexFromBytes(frameBytes));
            return getFrameItem(frameBufferItem);

        } finally {
            if (!validateFec) {
                turnOnFrameValidation();
            }
        }
    }

    /**
     * Get a frame item from a TC transfer frame
     *
     * @param frame
     * @return frame item
     * @throws FrameWrapUnwrapException
     */
    public TcSession.frmitem getFrameItem(final ITcTransferFrame frame) throws FrameWrapUnwrapException {
        return getFrameItem(new MpsTcTransferFrameSerializer().getBytes(frame));
    }

    /**
     * Get a frame item from a TC transfer frame
     *
     * @param frame
     * @param validateFec true to validate frame FEC, false to skip validation
     * @return
     * @throws FrameWrapUnwrapException
     */
    public TcSession.frmitem getFrameItem(final ITcTransferFrame frame, final boolean validateFec) throws
            FrameWrapUnwrapException {
        return getFrameItem(new MpsTcTransferFrameSerializer().getBytes(frame), validateFec);
    }

    /**
     * Get a frame item from a buffer item
     *
     * @param bufferItem
     * @return a frame item
     * @throws FrameWrapUnwrapException
     */
    public TcSession.frmitem getFrameItem(final TcSession.bufitem bufferItem) throws FrameWrapUnwrapException {
        return getFrameItem(bufferItem, true);
    }

    /**
     * Get a frame item from a buffer item
     *
     * @param bufferItem
     * @param validateFec true to validate frame FEC, false to skip validation
     * @return a frame item
     * @throws FrameWrapUnwrapException
     */
    public TcSession.frmitem getFrameItem(final TcSession.bufitem bufferItem, final boolean validateFec) throws
            FrameWrapUnwrapException {
        try {
            if (!validateFec) {
                turnOffFrameValidation();
            }
            final TcSession.frmitem frameItem = session.unwrapFrame(bufferItem.buf, bufferItem.nbits >> 3);
            checkFrameItemErrors(frameItem);

            return frameItem;

        } finally {
            if (!validateFec) {
                turnOnFrameValidation();
            }
        }
    }

    private void checkFrameItemErrors(final TcSession.frmitem frameItem) throws FrameWrapUnwrapException {
        if (frameItem.nerrors > 0) {
            throw new FrameWrapUnwrapException("Error unwrapping frame from CLTU: " + frameItem.errmsg);
        }
    }

    /**
     * Wrap frame bytes to a CLTU item
     *
     * @param frameBytes
     * @param vcid       virtual channel ID
     * @return a CLTU item
     * @throws CltuEndecException
     */
    public TcSession.cltuitem wrapFrameToCltu(final byte[] frameBytes, final int vcid) throws CltuEndecException {
        final TcSession.TcwrapGroup wrapGroup = createLocalWrapGroup(vcid);
        wrapGroup.HexstrEncodeToCltu(BinOctHexUtility.toHexFromBytes(frameBytes));
        final TcSession.bufitem cltuBufferItem = wrapGroup.getTcwrapBuffer();
        checkBufferItemErrors(cltuBufferItem);
        final TcSession.cltuitem cltuItem = session.decodeCltu(cltuBufferItem.buf, cltuBufferItem.nbits >> 3, true);
        checkCltuItemErrors(cltuItem);
        return cltuItem;
    }

    private void checkBufferItemErrors(final TcSession.bufitem cltuBufferItem) throws CltuEndecException {
        if (cltuBufferItem.nerrors > 0) {
            throw new CltuEndecException(cltuBufferItem.errmsg);
        }
    }

    /**
     * Create an empty CLTU item from the session
     *
     * @return a CLTU item
     * @throws CltuEndecException
     */
    public TcSession.cltuitem createCltuItem() throws CltuEndecException {
        final TcSession.cltuitem cltuItem = session.new cltuitem();
        checkCltuItemErrors(cltuItem);
        return cltuItem;
    }

    /**
     * @return MPS-configured CLTU start sequence
     */
    public String getCltuStartSequence() {
        return session.get_startseq();
    }

    /**
     * @return MPS-configured CLTU tail sequence
     */
    public String getCltuTailSequence() {
        return session.get_tailseq();
    }

    /**
     * Encode a CLTU item to a buffer item.
     *
     *  WARNING: as of 8/6/2020, CTS says this method does not support CCSDS pseudo-randomization.
     *
     * @param cltuItem
     * @return CTS buffer item representing the CLTU
     * @throws CltuEndecException
     */
    public TcSession.bufitem encodeCltu(final TcSession.cltuitem cltuItem) throws CltuEndecException {
        final TcSession.bufitem cltuBufferItem = session.encodeCltu(cltuItem);
        checkBufferItemErrors(cltuBufferItem);
        return cltuBufferItem;
    }

    /**
     * MPCS-11856 - 8/6/2020 - jfwagner
     * Encode a CLTU with CTS' newer CLTU-building interface, "BufferEncodeToCltu."
     * The CTS team says that "BufferEncodeToCltu" supports CCSDS pseudo-randomization, which can be configured
     * using telecmd.xml.
     *
     * @param frameBytes TC frames that have been serialized to bytes
     * @return CTS buffer item representing the CLTU
     * @throws CltuEndecException
     */
    public TcSession.bufitem encodeCltu(final byte[] frameBytes) throws CltuEndecException {
        // convert the frame bytes to hex, then use the CTS-provided utility to turn hex into a buffer
        TcSession.bufitem frameBuf = session.hexstring_to_bufitem(BinOctHexUtility.toHexFromBytes(frameBytes));

        // BufferEncodeToCltu takes the buffer itself and the buffer length in bytes
        TcSession.bufitem cltuEncoded = session.BufferEncodeToCltu(frameBuf.buf, frameBuf.nbits >> 3);
        checkBufferItemErrors(cltuEncoded);
        return cltuEncoded;
    }

    public boolean setCltuStartSequence(final String hexStartSequence) {
        if (!BinOctHexUtility.isValidHex(hexStartSequence)) {
            throw new IllegalArgumentException("Start sequence must be a valid hex string.");
        }
        return session.set_startseq(BinOctHexUtility.stripHexPrefix(hexStartSequence));
    }

    public boolean setCltuTailSequence(final String hexTailSequence) {
        if (!BinOctHexUtility.isValidHex(hexTailSequence)) {
            throw new IllegalArgumentException("Tail sequence must be a valid hex string.");
        }
        return session.set_tailseq(BinOctHexUtility.stripHexPrefix(hexTailSequence));
    }

    public void restoreCltuStartSequence() {
        session.restore_startseq();
        ;
    }

    public void restoreCltuTailSequence() {
        session.restore_tailseq();
    }

}
