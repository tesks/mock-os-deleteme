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

package jpl.gds.tc.impl.cltu;

import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.cltu.IBchCodeBlockBuilder;
import jpl.gds.tc.api.cltu.ICltuBuilder;
import jpl.gds.tc.impl.cltu.parsers.BchCodeBlockBuilder;
import jpl.gds.tc.impl.cltu.parsers.CltuBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CLTU holder for CLTU bytes returned from MPS CTS. When obtained from the MPS TEW utility, this CLTU is guaranteed to
 * be made according to the MPSA "Gold Standard". If the CLTU is modified after creation, this guarantee does not hold.
 * <p>
 * Consider using the class CltuBuilder and associated ICltuParser to create this object.
 *
 */
public class Cltu implements ICltu {

    private byte[] startSeq;
    private byte[] tailSeq;
    private byte[] acquisitionSeq ;
    private byte[] idleSeq;

    private List<IBchCodeblock>    bchCodeBlocks;
    private List<ITcTransferFrame> frames = new ArrayList<>();

    private Integer orderId;
    private byte[] data;

    @Override
    public byte[] getBytes() {
        if (bchCodeBlocks == null || bchCodeBlocks.isEmpty()) {
            throw new IllegalStateException("There are no BCH codeblocks to return data bytes.");
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(
                startSeq.length + tailSeq.length + (bchCodeBlocks.size() * 8));
        try {
            baos.write(startSeq);
            baos.write(data);
            baos.write(tailSeq);
        } catch (final IOException e) {
            // ignore
        }
        return baos.toByteArray();
    }

    @Override
    public List<ITcTransferFrame> getFrames() {
        if (frames == null) {
            throw new IllegalStateException("Frames are null.");
        }
        return frames;
    }

    @Override
    public ICltu copy() {

        final List<IBchCodeblock> copyCodeblocks = bchCodeBlocks.stream()
                .map(block -> {
                    IBchCodeBlockBuilder builder = new BchCodeBlockBuilder();
                    builder.setEdac(Arrays.copyOf(block.getEdac(), block.getEdac().length));
                    builder.setData(Arrays.copyOf(block.getData(), block.getData().length));
                    return builder.build();
                })
                .collect(Collectors.toList());

        ICltuBuilder builder = new CltuBuilder()
                .setData(Arrays.copyOf(data, data.length))
                .setCodeblocks(copyCodeblocks)
                .setAcquisitionSequence(Arrays.copyOf(acquisitionSeq, acquisitionSeq.length))
                .setStartSequence(Arrays.copyOf(startSeq, startSeq.length))
                .setIdleSequence(Arrays.copyOf(idleSeq, idleSeq.length))
                .setTailSequence(Arrays.copyOf(tailSeq, tailSeq.length))
                .setFrames(frames.stream().map(ITcTransferFrame::copy).collect(Collectors.toList()));

        if(orderId != null) {
            builder.setOrderId(orderId);
        }

        return builder.build();
    }

    @Override
    public Integer getOrderId() {
        return orderId;
    }

    @Override
    public void setOrderId(final Integer orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        this.orderId = orderId;
    }

    @Override
    public byte[] getAcquisitionSequence() {
        if (acquisitionSeq == null) {
            throw new IllegalStateException("Acquisition sequence is null.");
        }
        return acquisitionSeq;
    }

    @Override
    public void setAcquisitionSequence(final byte[] acquisitionSequence) {
        if (acquisitionSequence == null) {
            throw new IllegalArgumentException("Acquisition sequence must not be null.");
        }
        this.acquisitionSeq = acquisitionSequence;
    }

    @Override
    public byte[] getStartSequence() {
        if (startSeq == null) {
            throw new IllegalStateException("Start sequence is null.");
        }
        return startSeq;
    }

    @Override
    public void setStartSequence(final byte[] startSequence) {
        if (startSequence == null) {
            throw new IllegalArgumentException("Start sequence must not be null.");
        }
        this.startSeq = startSequence;
    }

    @Override
    public byte[] getData() {
        if (data == null) {
            throw new IllegalStateException("Data is null.");
        }
        return data;
    }

    @Override
    public void setData(final byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null.");
        }
        this.data = data;
    }

    @Override
    public byte[] getTailSequence() {
        if (tailSeq == null) {
            throw new IllegalStateException("Tail sequence is null.");
        }
        return tailSeq;
    }

    @Override
    public void setTailSequence(final byte[] tailSequence) {
        if (tailSequence == null) {
            throw new IllegalArgumentException("Tail sequence must not be null.");
        }
        this.tailSeq = tailSequence;
    }

    @Override
    public byte[] getIdleSequence() {
        if (idleSeq == null) {
            throw new IllegalStateException("Idle sequence is null.");
        }
        return idleSeq;
    }

    @Override
    public void setIdleSequence(final byte[] idleSequence) {
        if (idleSequence == null) {
            throw new IllegalArgumentException("Idle sequence must not be null.");
        }
        this.idleSeq = idleSequence;
    }

    /**
     * {@inheritDoc}
     * <p>
     * NOTE: this will return a copy of this object's internal code blocks. If you wish to modify the code blocks, set
     * them into the object afterward.
     *
     * @return
     */
    @Override
    public List<IBchCodeblock> getCodeblocks() {
        if (bchCodeBlocks == null) {
            throw new IllegalStateException("There are no codeblocks.");
        }

        List<IBchCodeblock> ret = new ArrayList<>();

        bchCodeBlocks.forEach(bchCodeBlock -> ret.add(bchCodeBlock.copy()));

        return ret;
    }

    @Override
    public void setCodeblocks(final List<IBchCodeblock> codeblockObjects) {
        this.bchCodeBlocks = codeblockObjects;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (final IBchCodeblock codeblock : bchCodeBlocks) {
            try {
                baos.write(codeblock.getBytes());
            } catch (final IOException e) {
                throw new IllegalArgumentException("Error converting codeblocks to data bytes.");
            }
        }
        setData(baos.toByteArray());
    }

    @Override
    public void setFrames(final List<ITcTransferFrame> frames) {
        this.frames = frames;
    }

    @Override
    public String getHexDisplayString() {
        return BinOctHexUtility.toHexFromBytes(getBytes());
    }

    @Override
    public byte[] getPlopBytes() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(getAcquisitionSequence());

            baos.write(getBytes());

            baos.write(getIdleSequence());
        } catch (final IOException e) {
            // suppress/ignore
        }
        return baos.toByteArray();
    }
}
