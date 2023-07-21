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
package jpl.gds.tc.mps.impl.cltu.serializers;

import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.cltu.ITcCltuBuilder;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.mps.impl.session.MpsSession;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code MpsTcCltuBuilder} is the CTS/MPSA implementation of ITcCltuBuilder. Set the codeblocks and (optionally) any
 * overridden sequences, and use the #build() method to retrieve CLTU bytes generated from CTS/MPSA.
 *
 * @since 8.2.0
 */
public class MpsTcCltuBuilder implements ITcCltuBuilder {

    private final int                 scid;
    private       boolean             startSequenceSet;
    private       String              startSequence;
    private       boolean             tailSequenceSet;
    private       String              tailSequence;
    private       boolean             acquisitionSequenceSet;
    private       String              acquisitionSequence;
    private       List<IBchCodeblock> codeblocks;
    private       boolean             codeblocksSet;
    private       byte[]              frameBytes;
    private       boolean             frameBytesSet;

    public MpsTcCltuBuilder(final int scid) {
        this.scid = scid;
    }

    /**
     * @return the MPS-configured CLTU start sequence
     */
    public String getStartSequence() {
        try (final MpsSession session = new MpsSession(scid)) {
            return session.getCltuStartSequence();
        }
    }

    /**
     * @return the MPS-configured CLTU tail sequence
     */
    public String getTailSequence() {
        try (final MpsSession session = new MpsSession(scid)) {
            return session.getCltuTailSequence();
        }
    }


    /**
     * There are two ways the builder can create CLTUs
     * 1. using codeblocks (NB - does not support CCSDS pseudo-randomization of frames)
     * 2. using frameBytes
     *
     * If you wish to build using codeblocks, use the setters provided in the builder to populate
     * the codeblocks (at minimum), and the start, tail, and acquisition sequences.
     *
     * If you wish to build using frames, use setFrameBytes.
     *
     * @return
     * @throws CltuEndecException
     */
    @Override
    public byte[] build() throws CltuEndecException {

        checkPreconditions();

        try (final MpsSession session = new MpsSession(scid)) {

            if(frameBytesSet) {
                // prefer CLTU creation with frame bytes, since this way supports pseudo-randomization
                return getCtsCltuBytes(session, frameBytes);
            }
            else {

                final TcSession.cltuitem cltuItem = session.createCltuItem();

                setSequences(session, cltuItem);
                setCltuCodeblocks(cltuItem);

                return getCtsCltuBytes(session, cltuItem);
            }
        }
    }

    private byte[] getCtsCltuBytes(final MpsSession session, final TcSession.cltuitem cltuItem) throws
                                                                                                CltuEndecException {
        final TcSession.bufitem ctsCltuBufferItem = session.encodeCltu(cltuItem);
        return BinOctHexUtility.toBytesFromHex(
                "0x" + UplinkUtils.bintoasciihex(ctsCltuBufferItem.buf, ctsCltuBufferItem.nbits, 0));
    }

    /**
     * MPCS-11856 - 8/6/2020
     * This method relies on the TcSession method "BufferEncodeToCltu" which accepts raw frame data, instead
     * of a cltuitem.
     * As of this writing, only this implementation of getCtsCltuBytes supports CCSDS pseudo-randomization.
     * @param frameBytes TC frames that have been serialized to bytes
     * @return CLTU bytes from CTS/MPSA. Frames will have been randomized if randomization was turned on in the telecmd.xml.
     * @throws CltuEndecException
     */
    private byte[] getCtsCltuBytes(final MpsSession session, final byte[] frameBytes) throws
            CltuEndecException {
        final TcSession.bufitem ctsCltuBufferItem = session.encodeCltu(frameBytes);
        return BinOctHexUtility.toBytesFromHex(
                "0x" + UplinkUtils.bintoasciihex(ctsCltuBufferItem.buf, ctsCltuBufferItem.nbits, 0));
    }

    private void setCltuCodeblocks(final TcSession.cltuitem cltuItem) {
        final CtsCodeblockLists ctsCodeblockLists = convertCodeblocks(codeblocks);
        cltuItem.cbAhstrAry = ctsCodeblockLists.getCodewordList();
        cltuItem.edacAhstrAry = ctsCodeblockLists.getEdacList();
    }

    private void setSequences(final MpsSession session, final TcSession.cltuitem cltuItem) {
        cltuItem.ssqAhstr = startSequenceSet ? startSequence : session.getCltuStartSequence();
        cltuItem.tsqAhstr = tailSequenceSet ? tailSequence : session.getCltuTailSequence();
        if (acquisitionSequenceSet) {
            cltuItem.asqAhstr = acquisitionSequence;
        }
    }

    private void checkPreconditions() {
        if (!codeblocksSet && !frameBytesSet) {
            throw new IllegalStateException("Codeblocks or frameBytes must be set.");
        }
    }

    private static class CtsCodeblockLists {
        private final ArrayList<String> codewordList;
        private final ArrayList<String> edacList;

        CtsCodeblockLists(final ArrayList<String> codewordList, final ArrayList<String> edacList) {
            this.codewordList = codewordList;
            this.edacList = edacList;
        }

        ArrayList<String> getCodewordList() {
            return this.codewordList;
        }

        ArrayList<String> getEdacList() {
            return this.edacList;
        }
    }

    @Override
    public ITcCltuBuilder setStartSequence(final byte[] startSequence) {
        return setStartSequence(BinOctHexUtility.toHexFromBytes(startSequence));
    }

    @Override
    public ITcCltuBuilder setStartSequence(final String startSequence) {
        checkValidHex("start", startSequence);
        this.startSequence = BinOctHexUtility.stripHexPrefix(startSequence);
        this.startSequenceSet = true;
        return this;
    }

    private void checkValidHex(final String sequenceType, final String sequenceHex) {
        if (sequenceHex == null) {
            throw new IllegalArgumentException("Start sequence cannot be null.");
        }
        if (!BinOctHexUtility.isValidHex(sequenceHex)) {
            throw new IllegalArgumentException("Input " + sequenceType + " sequence is not valid hex.");
        }
    }

    @Override
    public ITcCltuBuilder setTailSequence(final byte[] tailSequence) {
        return setTailSequence(BinOctHexUtility.toHexFromBytes(tailSequence));
    }

    @Override
    public ITcCltuBuilder setTailSequence(final String tailSequence) {
        if (tailSequence == null) {
            throw new IllegalArgumentException("Tail sequence cannot be null.");
        }
        checkValidHex("tail", tailSequence);
        this.tailSequence = BinOctHexUtility.stripHexPrefix(tailSequence);
        this.tailSequenceSet = true;
        return this;
    }

    @Override
    public ITcCltuBuilder setAcquisitionSequence(final byte[] acquisitionSequence) {
        return setAcquisitionSequence(BinOctHexUtility.toHexFromBytes(acquisitionSequence));
    }

    @Override
    public ITcCltuBuilder setAcquisitionSequence(final String acquisitionSequence) {
        if (acquisitionSequence == null) {
            throw new IllegalArgumentException("Acquisition sequence cannot be null.");
        }
        checkValidHex("acquisition", acquisitionSequence);
        this.acquisitionSequence = BinOctHexUtility.stripHexPrefix(acquisitionSequence);
        this.acquisitionSequenceSet = true;
        return this;
    }

    @Override
    public ITcCltuBuilder setCodeblocks(final List<IBchCodeblock> codeblocks) {
        if (codeblocks == null) {
            throw new IllegalArgumentException("Codeblocks cannot be null.");
        }
        this.codeblocks = codeblocks;
        this.codeblocksSet = true;
        return this;
    }

    /**
     * MPCS-11856 - 8/6/2020
     * Setting frame bytes, which can be used to build a CLTU
     *
     * @param frameBytes byte array
     * @return self
     */
    @Override
    public ITcCltuBuilder setFrameBytes(final byte[] frameBytes) {
        if (frameBytes == null) {
            throw new IllegalArgumentException("FrameBytes cannot be null.");
        }
        this.frameBytes = frameBytes;
        this.frameBytesSet = true;
        return this;
    }

    private CtsCodeblockLists convertCodeblocks(final List<IBchCodeblock> codeblocks) {
        final ArrayList<String> codewordList = new ArrayList<>();
        final ArrayList<String> edacList     = new ArrayList<>();
        for (final IBchCodeblock codeblock : codeblocks) {
            final String codeword = BinOctHexUtility.toHexFromBytes(codeblock.getData());
            final String edac     = BinOctHexUtility.toHexFromBytes(codeblock.getEdac());

            codewordList.add(codeword);
            edacList.add(edac);
        }

        return new CtsCodeblockLists(codewordList, edacList);
    }
}
