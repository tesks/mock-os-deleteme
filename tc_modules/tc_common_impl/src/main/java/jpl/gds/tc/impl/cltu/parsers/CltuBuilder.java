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
package jpl.gds.tc.impl.cltu.parsers;

import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.cltu.ICltuBuilder;
import jpl.gds.tc.api.config.CltuProperties;
import jpl.gds.tc.api.config.PlopProperties;
import jpl.gds.tc.impl.cltu.Cltu;

import java.util.ArrayList;
import java.util.List;

import static jpl.gds.shared.util.BinOctHexUtility.HEX_STRING_PREFIX1;
import static jpl.gds.shared.util.BinOctHexUtility.toByteFromHex;

/**
 * Builder for MPS CLTU instances.
 *
 *
 * MPCS-11285 - 09/24/19 - removed the requirement of frame list being set.
 *         (can happen if the CLTU doesn't contain a valid frame)
 */
public class CltuBuilder implements ICltuBuilder {
    private static byte[] defaultAcquisitionSeq = BinOctHexUtility
            .createPaddedArray("0x55", 22, toByteFromHex('5', '5'));
    private static byte[] defaultIdleSeq        = BinOctHexUtility
            .createPaddedArray("55", 8, toByteFromHex('5', '5'));
    private static byte[] defaultStartSeq       = BinOctHexUtility
            .toBytesFromHex(HEX_STRING_PREFIX1 + "5555EB90");
    private static byte[] defaultTailSeq        = BinOctHexUtility
            .toBytesFromHex(HEX_STRING_PREFIX1 + "C5C5C5C5C5C5C579");

    private Integer                orderId = null;
    private byte[]                 acquisitionSequence = null;
    private byte[]                 startSequence = null;
    private byte[]                 data = null;
    private byte[]                 tailSequence = null;
    private byte[]                 idleSequence = null;
    private List<IBchCodeblock> codeblocks = null;
    private List<ITcTransferFrame> frames = null;

    /**
     * Set the default acquisition sequence
     *
     * @param acquisitionSequence the default CLTU acquisition sequence
     */
    public static void setDefaultAcquisitionSeq(final byte[] acquisitionSequence) {
        defaultAcquisitionSeq = acquisitionSequence;
    }

    /**
     * Set the default idle sequence
     *
     * @param idleSequence the default CLTU idle sequence
     */
    public static void setDefaultIdleSequence(final byte[] idleSequence) {
        defaultIdleSeq = idleSequence;
    }

    /**
     * Set the default start sequence
     *
     * @param startSequence the default CLTU start sequence
     */
    public static void setDefaultStartSequence(final byte[] startSequence) {
        defaultStartSeq = startSequence;
    }

    /**
     * Set the default tail sequence
     *
     * @param tailSequence the default CLTU tail sequence
     */
    public static void setDefaultTailSequence(final byte[] tailSequence) {
        defaultTailSeq = tailSequence;
    }

    /**
     * Set the default acquisition, idle, start, and tail sequences
     * @param cltuProperties the current CltuProperties
     * @param plopProperties the current PlopProperties
     */
    public static void setSequences(final CltuProperties cltuProperties, final PlopProperties plopProperties) {
        setDefaultAcquisitionSeq(plopProperties.getAcquisitionSequence());
        setDefaultIdleSequence(plopProperties.getIdleSequence());
        setDefaultStartSequence(cltuProperties.getStartSequence());
        setDefaultTailSequence(cltuProperties.getTailSequence());
    }

    @Override
    public ICltu build() {

        checkPreconditions();

        final ICltu cltu = new Cltu();

        cltu.setData(data);
        cltu.setCodeblocks(codeblocks);
        cltu.setFrames(frames);
        cltu.setStartSequence(startSequence);
        cltu.setTailSequence(tailSequence);
        cltu.setAcquisitionSequence(acquisitionSequence);
        cltu.setIdleSequence(idleSequence);

        if (orderId != null) {
            cltu.setOrderId(orderId);
        }

        return cltu;
    }

    private void checkPreconditions() {
        if (data == null) {
            throw new IllegalStateException("Data must be set.");
        }

        if (codeblocks == null) {
            throw new IllegalStateException("Codeblocks must be set.");
        }

        if (frames == null) {
            frames = new ArrayList<>();
        }

        if (acquisitionSequence == null) {
            setAcquisitionSequence(defaultAcquisitionSeq);
        }

        if (idleSequence == null) {
            setIdleSequence(defaultIdleSeq);
        }

        if (startSequence == null) {
            setStartSequence(defaultStartSeq);
        }

        if (tailSequence == null) {
            setTailSequence(defaultTailSeq);
        }
    }

    @Override
    public ICltuBuilder setOrderId(final int orderId) {
        this.orderId = orderId;
        return this;
    }

    @Override
    public ICltuBuilder setAcquisitionSequence(final byte[] acquisitionSequence) {
        if (acquisitionSequence == null) {
            throw new IllegalArgumentException("Acquisition sequence must not be null.");
        }
        this.acquisitionSequence = acquisitionSequence;
        return this;
    }

    @Override
    public ICltuBuilder setStartSequence(final byte[] startSequence) {
        if (startSequence == null) {
            throw new IllegalArgumentException("Start sequence must not be null.");
        }
        this.startSequence = startSequence;
        return this;
    }

    @Override
    public ICltuBuilder setData(final byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null.");
        }
        this.data = data;
        return this;
    }

    @Override
    public ICltuBuilder setTailSequence(final byte[] tailSequence) {
        if (tailSequence == null) {
            throw new IllegalArgumentException("Tail sequence must not be null.");
        }
        this.tailSequence = tailSequence;
        return this;
    }

    @Override
    public ICltuBuilder setIdleSequence(final byte[] idleSequence) {
        if (idleSequence == null) {
            throw new IllegalArgumentException("Idle sequence must not be null.");
        }
        this.idleSequence = idleSequence;
        return this;
    }

    @Override
    public ICltuBuilder setCodeblocks(final List<IBchCodeblock> codeblocks) {
        if (codeblocks == null) {
            throw new IllegalArgumentException("Code blocks must not be null.");
        }
        this.codeblocks = codeblocks;

        return this;
    }

    @Override
    public ICltuBuilder setFrames(final List<ITcTransferFrame> frames) {
        if (frames == null) {
            throw new IllegalArgumentException("Frames must not be null.");
        }
        this.frames = frames;

        return this;
    }
}
